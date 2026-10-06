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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class LifecycleWorkflowIntegrationTest extends DatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired LifecycleWorkflowService workflow;
    private long document, request, copy;

    @BeforeEach
    void fixture() {
        jdbc.update("INSERT INTO client_configuration VALUES (1, 1)");
        jdbc.update("INSERT INTO process_governance VALUES (1, 2, 0)");
        jdbc.update("INSERT INTO user_reporting_line VALUES (2, 3)");
        jdbc.update("INSERT INTO sop_document (org_id, business_process_id) VALUES (1, 1)");
        document = id();
        jdbc.update("INSERT INTO sop_work_item (document_id, original_author_id) VALUES (?, 4)", document);
        request = id();
        copy = copy(request, 4, null);
        actor(4);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private void actor(int id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.USER); // The command must query current database authority, not trust this role.
        var principal = new AppUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private long copy(long requestId, int editor, Long source) {
        jdbc.update("""
                INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, source_candidate_id, title, description, details)
                VALUES (?, ?, ?, ?, 'Test SOP', 'Test purpose', ?)
                """, document, requestId, editor, source, com.woven.support.StructuredFixtures.details("Verified steps"));
        return id();
    }

    private long submit() { return workflow.submit(request, copy, 0, 0, null, UUID.randomUUID()); }
    private Set<Integer> participants(long candidate) {
        return Set.copyOf(jdbc.queryForList("SELECT DISTINCT user_id FROM revision_participant WHERE revision_id = ?", Integer.class, candidate));
    }

    @Test
    void submissionFreezesCopyCapturesParticipantsAndNotifiesAssignedOwner() {
        long candidate = submit();
        assertEquals(Set.of(4), participants(candidate));
        assertEquals(List.of(2), jdbc.queryForList("SELECT reviewer_id FROM review_assignment WHERE revision_id = ?", Integer.class, candidate));
        assertEquals("SUBMITTED", jdbc.queryForObject("SELECT state FROM sop_working_copy WHERE working_copy_id = ?", String.class, copy));
        assertEquals(1, count("business_audit_event"));
        assertEquals(1, count("notification_event"));
        assertEquals(2, count("notification_recipient"));
        assertEquals(0, count("publication_record"));
    }

    @Test
    void independentApprovalPublishesWithMatchingDecisionHistoryAndOutbox() {
        long candidate = submit();
        actor(2);
        assertEquals(candidate, workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(candidate, jdbc.queryForObject("SELECT current_revision_id FROM sop_document WHERE document_id = ?", Long.class, document));
        assertEquals("PUBLISHED", jdbc.queryForObject("SELECT state FROM sop_work_item WHERE work_item_id = ?", String.class, request));
        assertEquals(1, count("approval_decision"));
        assertEquals(1, count("publication_record"));
        assertEquals(1, count("revision_history_position"));
        assertEquals(2, count("notification_event"));
        assertEquals(6, count("notification_recipient"));
    }

    @Test
    void sameCommandsReturnSameResultWithoutDuplicateRecords() {
        UUID submitCommand = UUID.randomUUID();
        long candidate = workflow.submit(request, copy, 0, 0, null, submitCommand);
        assertEquals(candidate, workflow.submit(request, copy, 0, 0, null, submitCommand));
        actor(2);
        UUID approveCommand = UUID.randomUUID();
        workflow.approve(request, candidate, 1, Mode.NORMAL, null, approveCommand);
        assertEquals(candidate, workflow.approve(request, candidate, 1, Mode.NORMAL, null, approveCommand));
        assertEquals(2, count("workflow_command"));
        assertEquals(2, count("notification_event"));
        assertEquals(1, count("publication_record"));
    }

    @Test
    void commandIdCannotBeReusedForDifferentInput() {
        UUID command = UUID.randomUUID();
        workflow.submit(request, copy, 0, 0, null, command);
        assertThrows(IllegalStateException.class, () -> workflow.submit(request, copy, 0, 0, "Different input", command));
    }

    @Test
    void ownerEditsPreserveAuthorAndRerouteToManagerOrAdmin() {
        long first = submit();
        actor(2);
        long reviewerCopy = copy(request, 2, first);
        long second = workflow.submit(request, reviewerCopy, 1, 0, "Corrected steps", UUID.randomUUID());
        assertEquals(Set.of(2, 4), participants(second));
        assertEquals(Set.of(1, 3), Set.copyOf(jdbc.queryForList("SELECT reviewer_id FROM review_assignment WHERE revision_id = ?", Integer.class, second)));
        assertEquals(2, count("sop_revision"));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM notification_recipient n JOIN business_audit_event a USING(event_id) WHERE a.action = 'CANDIDATE_REPLACED' AND n.user_id = 4", Integer.class));
        assertThrows(SecurityException.class, () -> workflow.approve(request, second, 2, Mode.NORMAL, null, UUID.randomUUID()));
    }

    @Test
    void managerCanApproveOwnerEditedCandidate() {
        long first = submit();
        actor(2);
        long second = workflow.submit(request, copy(request, 2, first), 1, 0, "Correction", UUID.randomUUID());
        actor(3);
        workflow.approve(request, second, 2, Mode.NORMAL, null, UUID.randomUUID());
        assertEquals("DIRECT_MANAGER", jdbc.queryForObject("SELECT authority FROM approval_decision", String.class));
    }

    @Test
    void selfApprovalRecordsExceptionAndDeduplicatesRecipients() {
        long first = submit();
        actor(2);
        long second = workflow.submit(request, copy(request, 2, first), 1, 0, "Correction", UUID.randomUUID());
        workflow.approve(request, second, 2, Mode.SELF_APPROVAL, "Urgent verified correction", UUID.randomUUID());
        assertTrue(jdbc.queryForObject("SELECT self_approval FROM approval_decision", Boolean.class));
        assertEquals("Urgent verified correction", jdbc.queryForObject("SELECT reason FROM approval_decision", String.class));
        assertEquals(8, jdbc.queryForObject("SELECT COUNT(*) FROM notification_recipient n JOIN business_audit_event a USING(event_id) WHERE a.action = 'SELF_APPROVED'", Integer.class));
    }

    @Test
    void blankSelfApprovalDoesNotPublishOrNotify() {
        long first = submit();
        actor(2);
        long second = workflow.submit(request, copy(request, 2, first), 1, 0, "Correction", UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> workflow.approve(request, second, 2, Mode.SELF_APPROVAL, " ", UUID.randomUUID()));
        assertEquals(0, count("approval_decision"));
        assertEquals(2, count("notification_event"));
    }

    @Test
    void oldCandidateCannotBeApprovedAfterReplacement() {
        long first = submit();
        actor(2);
        workflow.submit(request, copy(request, 2, first), 1, 0, "Correction", UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> workflow.approve(request, first, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(0, count("publication_record"));
    }

    @Test
    void secondDraftBasedOnOldPublicationCannotOverwriteWinner() {
        long candidate = submit();
        jdbc.update("INSERT INTO sop_work_item (document_id, original_author_id) VALUES (?, 4)", document);
        long secondRequest = id();
        long secondCandidate = workflow.submit(secondRequest, copy(secondRequest, 4, null), 0, 0, null, UUID.randomUUID());
        actor(2);
        workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> workflow.approve(secondRequest, secondCandidate, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(1, count("publication_record"));
    }

    @Test
    void ownerChangeInvalidatesRoutingInsteadOfGrantingSilentApproval() {
        long candidate = submit();
        jdbc.update("UPDATE process_governance SET owner_user_id = 3, lock_version = 1 WHERE business_process_id = 1");
        actor(2);
        assertThrows(IllegalStateException.class, () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(0, count("approval_decision"));
    }

    @Test
    void unassignedAdminCannotUseNormalApprovalOfOrdinaryAuthorsWork() {
        long candidate = submit();
        actor(1);
        assertThrows(SecurityException.class, () -> workflow.approve(request, candidate, 1, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(0, count("approval_decision"));
    }

    @Test
    void anonymousAndDifferentAuthorsCannotSubmit() {
        SecurityContextHolder.clearContext();
        assertThrows(SecurityException.class, this::submit);
        actor(3);
        assertThrows(SecurityException.class, this::submit);
        assertEquals(0, count("sop_revision"));
    }

    @Test
    void missingOwnerConfigurationFailsBeforeAnyWorkflowWrites() {
        jdbc.update("DELETE FROM process_governance WHERE business_process_id = 1");
        assertThrows(DataAccessException.class, this::submit);
        assertEquals(0, count("sop_revision"));
    }

    @Test
    void incompleteContentFailsBeforeSnapshotCapture() {
        jdbc.update("UPDATE sop_working_copy SET details = ' ' WHERE working_copy_id = ?", copy);
        assertThrows(IllegalArgumentException.class, this::submit);
        assertEquals(0, count("sop_revision"));
    }

    @Test
    void productGuideCannotBeSubmittedAsClientContent() {
        jdbc.update("UPDATE sop_document SET document_kind = 'PRODUCT_GUIDE', product_key = 'test-workflow-guide' WHERE document_id = ?", document);
        assertThrows(IllegalStateException.class, this::submit);
    }

    @Test
    void clientMismatchFailsBeforeAnySnapshotIsCreated() {
        jdbc.update("INSERT INTO org (org_name) VALUES ('Other test client')");
        long otherOrg = id();
        jdbc.update("UPDATE client_configuration SET org_id = ? WHERE configuration_id = 1", otherOrg);
        assertThrows(SecurityException.class, this::submit);
        assertEquals(0, count("sop_revision"));
    }

    @Test
    void ordinaryAuthorCannotUseExplicitSelfApproval() {
        long candidate = submit();
        assertThrows(SecurityException.class,
                () -> workflow.approve(request, candidate, 1, Mode.SELF_APPROVAL, "I authored this", UUID.randomUUID()));
        assertEquals(0, count("publication_record"));
    }

    @Test
    void removingAdminRoleInvalidatesTheirAssignment() {
        long first = submit();
        actor(2);
        long second = workflow.submit(request, copy(request, 2, first), 1, 0, "Correction", UUID.randomUUID());
        jdbc.update("UPDATE users SET role = 'USER' WHERE id = 1");
        actor(1);
        assertThrows(IllegalStateException.class,
                () -> workflow.approve(request, second, 2, Mode.NORMAL, null, UUID.randomUUID()));
        assertEquals(0, count("approval_decision"));
    }

    @Test
    void anotherActorCannotReplayAnAuthorsCommand() {
        UUID command = UUID.randomUUID();
        workflow.submit(request, copy, 0, 0, null, command);
        actor(2);
        assertThrows(SecurityException.class, () -> workflow.submit(request, copy, 0, 0, null, command));
        assertEquals(1, count("workflow_command"));
    }
}
