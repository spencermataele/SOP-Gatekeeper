package com.woven.backend;

import com.woven.app.domain.Role;
import com.woven.app.domain.User;
import com.woven.app.service.governance.ApprovalPolicy.Mode;
import com.woven.app.service.governance.LifecycleWorkflowService;
import com.woven.app.service.user.AppUserDetails;
import com.woven.support.DatabaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

/** Committed, isolated fixtures are needed so concurrent connections see the same request. */
class WorkflowAtomicityIntegrationTest extends DatabaseTest {
    @MockitoSpyBean JdbcTemplate jdbc;
    @Autowired LifecycleWorkflowService workflow;
    @Autowired PlatformTransactionManager transactions;
    private long document, request, candidate, reviewerCopy;
    private boolean configured;

    @BeforeEach
    void fixture() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO client_configuration VALUES (1, 1)");
            jdbc.update("INSERT INTO process_governance VALUES (1, 2, 0)");
            jdbc.update("INSERT INTO user_reporting_line VALUES (2, 3)");
            jdbc.update("INSERT INTO sop_document (org_id, business_process_id) VALUES (1, 1)");
            document = id();
            jdbc.update("INSERT INTO sop_work_item (document_id, original_author_id) VALUES (?, 4)", document);
            request = id();
            jdbc.update("""
                    INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, title, description, details)
                    VALUES (?, ?, 4, 'Atomicity fixture', 'Purpose', 'Original procedure')
                    """, document, request);
            long copy = id();
            actor(4);
            candidate = workflow.submit(request, copy, 0, 0, null, UUID.randomUUID());
            jdbc.update("""
                    INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, source_candidate_id, title, description, details)
                    VALUES (?, ?, 2, ?, 'Atomicity fixture', 'Purpose', 'Reviewer correction')
                    """, document, request, candidate);
            reviewerCopy = id();
        });
        configured = true;
        actor(2);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (!configured) return;
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("DELETE n FROM notification_recipient n JOIN business_audit_event a USING(event_id) WHERE a.document_id = ?", document);
            jdbc.update("DELETE n FROM notification_event n JOIN business_audit_event a USING(event_id) WHERE a.document_id = ?", document);
            jdbc.update("DELETE FROM business_audit_event WHERE document_id = ?", document);
            jdbc.update("DELETE c FROM workflow_command c JOIN sop_work_item w USING(work_item_id) WHERE w.document_id = ?", document);
            jdbc.update("DELETE FROM publication_record WHERE document_id = ?", document);
            for (String table : List.of("approval_decision", "review_assignment", "revision_participant")) {
                jdbc.update("DELETE t FROM " + table + " t JOIN sop_revision r USING(revision_id) WHERE r.document_id = ?", document);
            }
            jdbc.update("DELETE FROM revision_history_position WHERE document_id = ?", document);
            jdbc.update("UPDATE sop_document SET current_revision_id = NULL WHERE document_id = ?", document);
            jdbc.update("UPDATE sop_work_item SET current_candidate_id = NULL, base_revision_id = NULL, state = 'DRAFT' WHERE document_id = ?", document);
            jdbc.update("UPDATE sop_working_copy SET source_candidate_id = NULL WHERE document_id = ?", document);
            jdbc.update("UPDATE sop_revision SET predecessor_candidate_id = NULL WHERE document_id = ?", document);
            jdbc.update("DELETE FROM sop_revision WHERE document_id = ?", document);
            jdbc.update("DELETE FROM sop_working_copy WHERE document_id = ?", document);
            jdbc.update("DELETE FROM sop_work_item WHERE document_id = ?", document);
            jdbc.update("DELETE FROM sop_document WHERE document_id = ?", document);
            jdbc.update("DELETE FROM user_reporting_line WHERE user_id = 2");
            jdbc.update("DELETE FROM process_governance WHERE business_process_id = 1");
            jdbc.update("DELETE FROM client_configuration WHERE configuration_id = 1");
        });
    }

    private long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private void actor(int id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.USER);
        var principal = new AppUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private void failOutboxInsert() {
        doThrow(new DataIntegrityViolationException("Injected outbox persistence failure")).when(jdbc)
                .update(eq("INSERT INTO notification_event (event_id) VALUES (?)"), any(Object[].class));
    }

    @Test
    void auditFailureRollsBackReviewerSaveContentAndVersion() {
        doThrow(new DataIntegrityViolationException("Injected audit persistence failure")).when(jdbc)
                .update(contains("INSERT INTO business_audit_event"), any(Object[].class));
        assertThrows(DataIntegrityViolationException.class, () -> workflow.saveCopy(
                request, reviewerCopy, 1, 0, "Changed", "Purpose", "Changed procedure", UUID.randomUUID()));
        assertEquals("Reviewer correction", jdbc.queryForObject("SELECT details FROM sop_working_copy WHERE working_copy_id = ?", String.class, reviewerCopy));
        assertEquals(0L, jdbc.queryForObject("SELECT lock_version FROM sop_working_copy WHERE working_copy_id = ?", Long.class, reviewerCopy));
        assertEquals(1, count("workflow_command"));
    }

    @Test
    void outboxFailureRollsBackRejectionAndCopyCancellation() {
        failOutboxInsert();
        assertThrows(DataIntegrityViolationException.class,
                () -> workflow.reject(request, candidate, 1, "Clarify the instructions", UUID.randomUUID()));
        assertEquals(0, count("approval_decision"));
        assertEquals("IN_REVIEW", jdbc.queryForObject("SELECT state FROM sop_work_item WHERE work_item_id = ?", String.class, request));
        assertEquals("EDITABLE", jdbc.queryForObject("SELECT state FROM sop_working_copy WHERE working_copy_id = ?", String.class, reviewerCopy));
        assertEquals(1, count("notification_event"));
        assertEquals(1, count("business_audit_event"));
    }

    @Test
    void outboxFailureRollsBackPublicationDecisionPointerHistoryAndAudit() {
        failOutboxInsert();
        assertThrows(DataIntegrityViolationException.class,
                () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(0, count("publication_record"));
        assertEquals(0, count("approval_decision"));
        assertEquals(0, count("revision_history_position"));
        assertNull(jdbc.queryForObject("SELECT current_revision_id FROM sop_document WHERE document_id = ?", Long.class, document));
        assertEquals("IN_REVIEW", jdbc.queryForObject("SELECT state FROM sop_work_item WHERE work_item_id = ?", String.class, request));
        assertEquals(1, count("business_audit_event"));
        assertEquals(1, count("workflow_command"));
        assertEquals(1, count("notification_event"));
    }

    @Test
    void outboxFailureRollsBackReplacementAndLeavesCopyEditable() {
        failOutboxInsert();
        assertThrows(DataIntegrityViolationException.class,
                () -> workflow.submit(request, reviewerCopy, 1, 0, "Correction", UUID.randomUUID()));
        assertEquals(1, count("sop_revision"));
        assertEquals(candidate, jdbc.queryForObject("SELECT current_candidate_id FROM sop_work_item WHERE work_item_id = ?", Long.class, request));
        assertEquals("EDITABLE", jdbc.queryForObject("SELECT state FROM sop_working_copy WHERE working_copy_id = ?", String.class, reviewerCopy));
        assertEquals(1, count("review_assignment"));
        assertEquals(1, count("business_audit_event"));
        assertEquals(1, count("notification_event"));
    }

    @Test
    void publicationAndReplacementRaceHasExactlyOneWinner() throws Exception {
        List<Boolean> outcomes = race(
                () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID()),
                () -> workflow.submit(request, reviewerCopy, 1, 0, "Correction", UUID.randomUUID()));
        assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
        assertEquals(2, count("notification_event"));
        assertEquals(2, count("business_audit_event"));
        String state = jdbc.queryForObject("SELECT state FROM sop_work_item WHERE work_item_id = ?", String.class, request);
        if (state.equals("PUBLISHED")) {
            assertEquals(1, count("publication_record"));
            assertEquals(1, count("sop_revision"));
        } else {
            assertEquals("IN_REVIEW", state);
            assertEquals(0, count("publication_record"));
            assertEquals(2, count("sop_revision"));
        }
    }

    @Test
    void simultaneousRetriesReturnSuccessWithOnePublication() throws Exception {
        UUID command = UUID.randomUUID();
        List<Boolean> outcomes = race(
                () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, command),
                () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, command));
        assertEquals(List.of(true, true), outcomes);
        assertEquals(1, count("publication_record"));
        assertEquals(1, count("approval_decision"));
        assertEquals(2, count("notification_event"));
    }

    private List<Boolean> race(Callable<Long> first, Callable<Long> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var tasks = List.of(first, second).stream().map(command -> pool.submit(() -> {
                actor(2);
                ready.countDown();
                try {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Race did not start");
                    command.call();
                    return true;
                } catch (IllegalStateException conflict) {
                    assertTrue(conflict.getMessage().contains("changed") || conflict.getMessage().contains("closed"),
                            "Only a stale-request conflict is an expected loser: " + conflict.getMessage());
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return List.of(tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "Race workers must stop before fixture cleanup");
        }
    }
}
