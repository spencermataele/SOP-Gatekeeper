ALTER TABLE sop_work_item
    ADD COLUMN prior_rejected_request_id BIGINT NULL,
    ADD COLUMN resubmission_revision_id BIGINT NULL,
    ADD CONSTRAINT fk_rejected_attempt FOREIGN KEY (document_id, prior_rejected_request_id)
        REFERENCES sop_work_item(document_id, work_item_id),
    ADD CONSTRAINT fk_resubmission_source FOREIGN KEY (document_id, prior_rejected_request_id, resubmission_revision_id)
        REFERENCES sop_revision(document_id, work_item_id, revision_id),
    ADD CONSTRAINT ck_resubmission_source CHECK (
        (prior_rejected_request_id IS NULL AND resubmission_revision_id IS NULL) OR
        (prior_rejected_request_id IS NOT NULL AND resubmission_revision_id IS NOT NULL));

ALTER TABLE business_audit_event DROP CHECK ck_audit_action,
    ADD CONSTRAINT ck_audit_action CHECK (action IN (
        'SUBMITTED', 'CANDIDATE_REPLACED', 'PUBLISHED', 'SELF_APPROVED',
        'DRAFT_CREATED', 'REVIEW_COPY_CREATED', 'COPY_SAVED', 'REJECTED', 'CANCELLED', 'REASSIGNED'));

CREATE TABLE review_assignment_history (
    reassignment_event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    revision_id BIGINT NOT NULL,
    reviewer_id INT NOT NULL,
    authority VARCHAR(30) NOT NULL,
    routing_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    PRIMARY KEY (reassignment_event_id, reviewer_id),
    CONSTRAINT fk_assignment_history_event FOREIGN KEY (reassignment_event_id) REFERENCES business_audit_event(event_id),
    CONSTRAINT fk_assignment_history_revision FOREIGN KEY (revision_id) REFERENCES sop_revision(revision_id),
    CONSTRAINT fk_assignment_history_user FOREIGN KEY (reviewer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE review_routing_change (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    previous_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    new_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    owner_user_id INT NOT NULL,
    manager_user_id INT NULL,
    CONSTRAINT fk_routing_change_event FOREIGN KEY (event_id) REFERENCES business_audit_event(event_id),
    CONSTRAINT fk_routing_change_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_routing_change_manager FOREIGN KEY (manager_user_id) REFERENCES users(id)
) ENGINE=InnoDB;
