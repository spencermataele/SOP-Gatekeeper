package com.woven.backend;

import com.woven.app.repository.RevisionSnapshotStore;
import com.woven.support.DatabaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class LifecycleSchemaIntegrationTest extends DatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RevisionSnapshotStore snapshots;
    @Autowired PlatformTransactionManager transactions;

    private long document() {
        jdbc.update("INSERT INTO sop_document (org_id, business_process_id) VALUES (1, 1)");
        return lastId();
    }

    private long request(long document) {
        jdbc.update("INSERT INTO sop_work_item (document_id, original_author_id) VALUES (?, 1)", document);
        return lastId();
    }

    private long copy(long document, long request) {
        jdbc.update("""
                INSERT INTO sop_working_copy (document_id, work_item_id, editor_id, title, description, details)
                VALUES (?, ?, 1, 'Procedure', 'Purpose', 'Step one — café\nStep two')
                """, document, request);
        return lastId();
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void failureAfterSnapshotCreationRollsBackTheWholeTransaction() {
        var transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var documentId = new AtomicLong();
        var revisionId = new AtomicLong();
        var injectedFailure = new IllegalStateException("Simulated workflow failure");
        assertSame(injectedFailure, assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            long doc = document();
            documentId.set(doc);
            revisionId.set(snapshots.captureAuthoredCopy(copy(doc, request(doc)), 0, 1));
            throw injectedFailure;
        })));
        assertTrue(revisionId.get() > 0, "Failure must happen after the actual snapshot insert");
        assertTrue(snapshots.findAuthored(documentId.get(), revisionId.get()).isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sop_document WHERE document_id = ?", Integer.class, documentId.get()));
    }

    @Test
    void snapshotPreservesExactContentWhenWorkingCopyChanges() {
        long doc = document(), req = request(doc), copy = copy(doc, req);
        long revision = snapshots.captureAuthoredCopy(copy, 0, 1);
        var before = snapshots.findAuthored(doc, revision).orElseThrow();
        jdbc.update("UPDATE sop_working_copy SET details = 'Later edit', lock_version = 1 WHERE working_copy_id = ?", copy);
        assertEquals(before, snapshots.findAuthored(doc, revision).orElseThrow());
        assertEquals("Step one — café\nStep two", before.details());
        assertEquals(64, before.contentChecksum().length());
        assertTrue(snapshots.findAuthored(doc + 999, revision).isEmpty());
        assertNull(jdbc.queryForObject("SELECT current_revision_id FROM sop_document WHERE document_id = ?", Long.class, doc));
    }

    @Test
    void staleWorkingCopyCannotCreateSnapshot() {
        long doc = document(), copy = copy(doc, request(doc));
        jdbc.update("UPDATE sop_working_copy SET lock_version = 1 WHERE working_copy_id = ?", copy);
        assertThrows(OptimisticLockingFailureException.class, () -> snapshots.captureAuthoredCopy(copy, 0, 1));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sop_revision WHERE document_id = ?", Integer.class, doc));
    }

    @Test
    void cancelledWorkingCopyCannotCreateSnapshot() {
        long doc = document(), copy = copy(doc, request(doc));
        jdbc.update("UPDATE sop_working_copy SET state = 'CANCELLED' WHERE working_copy_id = ?", copy);
        assertThrows(OptimisticLockingFailureException.class, () -> snapshots.captureAuthoredCopy(copy, 0, 1));
    }

    @Test
    void copyCannotBeCapturedTwice() {
        long doc = document(), copy = copy(doc, request(doc));
        snapshots.captureAuthoredCopy(copy, 0, 1);
        assertThrows(DataAccessException.class, () -> snapshots.captureAuthoredCopy(copy, 0, 1));
    }

    @Test
    void currentAndBaseRevisionMustBelongToDocument() {
        long first = document(), second = document(), req = request(first);
        long revision = snapshots.captureAuthoredCopy(copy(first, req), 0, 1);
        long secondRequest = request(second);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE sop_document SET current_revision_id = ? WHERE document_id = ?", revision, second));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE sop_work_item SET base_revision_id = ? WHERE work_item_id = ?", revision, secondRequest));
    }

    @Test
    void candidateAndReviewerCopyMustBelongToSameRequestEvenWithinSameDocument() {
        long doc = document(), first = request(doc), second = request(doc);
        long revision = snapshots.captureAuthoredCopy(copy(doc, first), 0, 1);
        long otherCopy = copy(doc, second);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE sop_work_item SET current_candidate_id = ?, state = 'IN_REVIEW' WHERE work_item_id = ?", revision, second));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE sop_working_copy SET source_candidate_id = ? WHERE working_copy_id = ?", revision, otherCopy));
        jdbc.update("UPDATE sop_work_item SET current_candidate_id = ?, state = 'IN_REVIEW' WHERE work_item_id = ?", revision, first);
    }

    @Test
    void successorRetainsOriginalSnapshotAndPredecessorLink() {
        long doc = document(), req = request(doc);
        long first = snapshots.captureAuthoredCopy(copy(doc, req), 0, 1);
        long editedCopy = copy(doc, req);
        jdbc.update("UPDATE sop_working_copy SET source_candidate_id = ?, details = 'Reviewed change' WHERE working_copy_id = ?", first, editedCopy);
        long second = snapshots.captureAuthoredCopy(editedCopy, 0, 1);
        assertEquals(first, jdbc.queryForObject("SELECT predecessor_candidate_id FROM sop_revision WHERE revision_id = ?", Long.class, second));
        assertNotEquals(snapshots.findAuthored(doc, first).orElseThrow().contentChecksum(),
                snapshots.findAuthored(doc, second).orElseThrow().contentChecksum());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM sop_revision WHERE work_item_id = ?", Integer.class, req));
    }

    @Test
    void inReviewRequiresCandidateAndParticipantsRequireRealUsers() {
        long doc = document(), req = request(doc);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE sop_work_item SET state = 'IN_REVIEW' WHERE work_item_id = ?", req));
        long revision = snapshots.captureAuthoredCopy(copy(doc, req), 0, 1);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO revision_participant VALUES (?, -1, 'AUTHOR')", revision));
        jdbc.update("INSERT INTO revision_participant VALUES (?, 1, 'AUTHOR')", revision);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO revision_participant VALUES (?, 1, 'AUTHOR')", revision));
    }

    @Test
    void historyKeysAreUniqueAndCannotReferToAnotherDocument() {
        long doc = document(), req = request(doc), other = document();
        long first = snapshots.captureAuthoredCopy(copy(doc, req), 0, 1);
        long second = snapshots.captureAuthoredCopy(copy(doc, req), 0, 1);
        jdbc.update("INSERT INTO revision_history_position VALUES (?, ?, 1000)", doc, first);
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO revision_history_position VALUES (?, ?, 1000)", doc, second));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO revision_history_position VALUES (?, ?, 2000)", other, first));
    }

    @Test
    void productGuidesRequireStableKeysAndClientSopsCannotClaimThem() {
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO sop_document (org_id, business_process_id, document_kind) VALUES (1, 1, 'PRODUCT_GUIDE')"));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO sop_document (org_id, business_process_id, product_key) VALUES (1, 1, 'getting-started')"));
        jdbc.update("INSERT INTO sop_document (org_id, business_process_id, document_kind, product_key) VALUES (1, 1, 'PRODUCT_GUIDE', 'test-guide')");
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO sop_document (org_id, business_process_id, document_kind, product_key) VALUES (1, 1, 'PRODUCT_GUIDE', 'test-guide')"));
    }
}
