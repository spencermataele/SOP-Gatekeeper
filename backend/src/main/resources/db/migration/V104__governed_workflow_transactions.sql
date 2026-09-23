-- Explicit deployment and ownership configuration; no identities inferred from MVP seed IDs.
CREATE TABLE client_configuration (
    configuration_id INT PRIMARY KEY,
    org_id INT NOT NULL UNIQUE,
    CONSTRAINT ck_single_client CHECK (configuration_id = 1),
    CONSTRAINT fk_client_org FOREIGN KEY (org_id) REFERENCES org(org_id)
) ENGINE=InnoDB;

CREATE TABLE process_governance (
    business_process_id INT PRIMARY KEY,
    owner_user_id INT NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_governance_process FOREIGN KEY (business_process_id) REFERENCES business_process(business_process_id),
    CONSTRAINT fk_governance_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT ck_governance_version CHECK (lock_version >= 0)
) ENGINE=InnoDB;

CREATE TABLE user_reporting_line (
    user_id INT PRIMARY KEY,
    manager_user_id INT NOT NULL,
    CONSTRAINT fk_reporting_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reporting_manager FOREIGN KEY (manager_user_id) REFERENCES users(id),
    CONSTRAINT ck_reporting_self CHECK (user_id <> manager_user_id)
) ENGINE=InnoDB;

ALTER TABLE sop_work_item ADD COLUMN routing_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL;

CREATE TABLE review_assignment (
    revision_id BIGINT NOT NULL,
    reviewer_id INT NOT NULL,
    authority VARCHAR(30) NOT NULL,
    routing_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (revision_id, reviewer_id),
    CONSTRAINT fk_assignment_revision FOREIGN KEY (revision_id) REFERENCES sop_revision(revision_id),
    CONSTRAINT fk_assignment_user FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT ck_assignment_authority CHECK (authority IN ('PROCESS_OWNER', 'DIRECT_MANAGER', 'ADMINISTRATOR'))
) ENGINE=InnoDB;

CREATE TABLE approval_decision (
    decision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision_id BIGINT NOT NULL,
    actor_id INT NOT NULL,
    authority VARCHAR(30) NOT NULL,
    self_approval BOOLEAN NOT NULL,
    reason TEXT NULL,
    decided_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (revision_id, decision_id),
    CONSTRAINT fk_decision_revision FOREIGN KEY (revision_id) REFERENCES sop_revision(revision_id),
    CONSTRAINT fk_decision_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_decision_authority CHECK (authority IN ('PROCESS_OWNER', 'DIRECT_MANAGER', 'ADMINISTRATOR')),
    CONSTRAINT ck_decision_self CHECK (self_approval IN (0, 1) AND
        (self_approval = 0 OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)))
) ENGINE=InnoDB;

CREATE TABLE publication_record (
    publication_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    revision_id BIGINT NOT NULL UNIQUE,
    previous_revision_id BIGINT NULL,
    decision_id BIGINT NOT NULL UNIQUE,
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_publication_revision FOREIGN KEY (document_id, revision_id) REFERENCES sop_revision(document_id, revision_id),
    CONSTRAINT fk_publication_previous FOREIGN KEY (document_id, previous_revision_id) REFERENCES sop_revision(document_id, revision_id),
    CONSTRAINT fk_publication_decision FOREIGN KEY (revision_id, decision_id) REFERENCES approval_decision(revision_id, decision_id)
) ENGINE=InnoDB;

CREATE TABLE workflow_command (
    command_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    actor_id INT NOT NULL,
    fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result_revision_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_command_request FOREIGN KEY (work_item_id) REFERENCES sop_work_item(work_item_id),
    CONSTRAINT fk_command_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_command_revision FOREIGN KEY (result_revision_id) REFERENCES sop_revision(revision_id)
) ENGINE=InnoDB;

CREATE TABLE business_audit_event (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    document_id BIGINT NOT NULL,
    work_item_id BIGINT NOT NULL,
    revision_id BIGINT NOT NULL,
    actor_id INT NOT NULL,
    action VARCHAR(30) NOT NULL,
    reason TEXT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_candidate FOREIGN KEY (document_id, work_item_id, revision_id)
        REFERENCES sop_revision(document_id, work_item_id, revision_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_audit_action CHECK (action IN ('SUBMITTED', 'CANDIDATE_REPLACED', 'PUBLISHED', 'SELF_APPROVED'))
) ENGINE=InnoDB;

CREATE TABLE notification_event (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_outbox_audit FOREIGN KEY (event_id) REFERENCES business_audit_event(event_id)
) ENGINE=InnoDB;

CREATE TABLE notification_recipient (
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id INT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    delivery_state VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    PRIMARY KEY (event_id, user_id, channel),
    CONSTRAINT fk_recipient_event FOREIGN KEY (event_id) REFERENCES notification_event(event_id),
    CONSTRAINT fk_recipient_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_recipient_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ck_recipient_state CHECK (delivery_state IN ('QUEUED', 'ATTEMPTING', 'ACCEPTED', 'DELIVERED', 'FAILED', 'BOUNCED'))
) ENGINE=InnoDB;
