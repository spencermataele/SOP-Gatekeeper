ALTER TABLE approval_decision
    ADD COLUMN decision VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD CONSTRAINT ck_decision_kind CHECK (decision IN ('APPROVED', 'REJECTED')),
    ADD CONSTRAINT ck_rejection_reason CHECK (decision <> 'REJECTED' OR
        (self_approval = 0 AND reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)),
    ADD CONSTRAINT uq_decision_kind UNIQUE (revision_id, decision_id, decision);

-- A publication may reference an approval, never a rejection of that same candidate.
ALTER TABLE publication_record
    ADD COLUMN decision_kind VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD CONSTRAINT ck_publication_approved CHECK (decision_kind = 'APPROVED'),
    ADD CONSTRAINT fk_publication_approved FOREIGN KEY (revision_id, decision_id, decision_kind)
        REFERENCES approval_decision(revision_id, decision_id, decision);

ALTER TABLE workflow_command
    MODIFY result_revision_id BIGINT NULL,
    ADD COLUMN result_kind VARCHAR(20) NOT NULL DEFAULT 'REVISION',
    ADD COLUMN result_working_copy_id BIGINT NULL,
    ADD COLUMN result_version BIGINT NULL,
    ADD CONSTRAINT fk_command_copy FOREIGN KEY (result_working_copy_id) REFERENCES sop_working_copy(working_copy_id),
    ADD CONSTRAINT ck_command_result CHECK (
        (result_kind = 'REVISION' AND result_revision_id IS NOT NULL AND result_working_copy_id IS NULL AND result_version IS NULL) OR
        (result_kind = 'COPY' AND result_revision_id IS NULL AND result_working_copy_id IS NOT NULL AND result_version >= 0 AND result_version IS NOT NULL) OR
        (result_kind = 'REQUEST' AND result_revision_id IS NULL AND result_working_copy_id IS NULL AND result_version >= 0 AND result_version IS NOT NULL));

ALTER TABLE business_audit_event
    MODIFY revision_id BIGINT NULL,
    ADD COLUMN working_copy_id BIGINT NULL,
    ADD CONSTRAINT fk_audit_work_item FOREIGN KEY (document_id, work_item_id)
        REFERENCES sop_work_item(document_id, work_item_id),
    ADD CONSTRAINT fk_audit_copy FOREIGN KEY (document_id, work_item_id, working_copy_id)
        REFERENCES sop_working_copy(document_id, work_item_id, working_copy_id),
    DROP CHECK ck_audit_action,
    ADD CONSTRAINT ck_audit_action CHECK (action IN (
        'SUBMITTED', 'CANDIDATE_REPLACED', 'PUBLISHED', 'SELF_APPROVED',
        'DRAFT_CREATED', 'REVIEW_COPY_CREATED', 'COPY_SAVED', 'REJECTED', 'CANCELLED')),
    ADD CONSTRAINT ck_audit_revision CHECK (
        action IN ('DRAFT_CREATED', 'COPY_SAVED', 'CANCELLED') OR revision_id IS NOT NULL);
