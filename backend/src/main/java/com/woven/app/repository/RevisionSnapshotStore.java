package com.woven.app.repository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.Optional;

/**
 * Append/read storage, not a workflow authorization boundary. The workflow service must
 * authorize the actor and atomically save participants, routing, audit and outbox with this insert.
 */
@Repository
public class RevisionSnapshotStore {
    private final JdbcTemplate jdbc;

    public RevisionSnapshotStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Snapshot(long id, long documentId, long workItemId, String title,
                           String description, String details, int contentSchemaVersion,
                           String contentChecksum) {}

    @Transactional
    public long captureAuthoredCopy(long copyId, long expectedVersion, int authenticatedSubmitterId) {
        var key = new GeneratedKeyHolder();
        int inserted = jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO sop_revision
                      (document_id, work_item_id, source_working_copy_id, predecessor_candidate_id,
                       submitted_by_id, title, description, details, content_schema_version,
                       content_checksum, provenance)
                    SELECT document_id, work_item_id, working_copy_id, source_candidate_id,
                           ?, title, description, details, content_schema_version,
                           SHA2(CAST(JSON_ARRAY(title, description, details, content_schema_version)
                               AS CHAR CHARACTER SET utf8mb4), 256), 'AUTHORED'
                    FROM sop_working_copy
                    WHERE working_copy_id = ? AND lock_version = ? AND state = 'EDITABLE'
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, authenticatedSubmitterId);
            statement.setLong(2, copyId);
            statement.setLong(3, expectedVersion);
            return statement;
        }, key);
        if (inserted != 1) {
            throw new OptimisticLockingFailureException("Working copy is missing, changed, or no longer editable");
        }
        Number generatedId = key.getKey();
        if (generatedId == null) throw new IllegalStateException("Revision insert returned no identity");
        return generatedId.longValue();
    }

    @Transactional(readOnly = true)
    public Optional<Snapshot> findAuthored(long documentId, long revisionId) {
        return jdbc.query("""
                SELECT revision_id, document_id, work_item_id, title, description, details,
                       content_schema_version, content_checksum
                FROM sop_revision WHERE document_id = ? AND revision_id = ? AND provenance = 'AUTHORED'
                """, (row, index) -> new Snapshot(row.getLong("revision_id"), row.getLong("document_id"),
                row.getLong("work_item_id"), row.getString("title"), row.getString("description"),
                row.getString("details"), row.getInt("content_schema_version"),
                row.getString("content_checksum")), documentId, revisionId).stream().findFirst();
    }
}
