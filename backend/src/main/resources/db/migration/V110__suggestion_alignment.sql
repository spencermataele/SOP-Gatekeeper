CREATE TABLE process_suggestion (
    suggestion_id CHAR(36) PRIMARY KEY,
    business_process_id INT NOT NULL,
    document_id BIGINT NULL,
    submitter_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    problem TEXT NOT NULL,
    proposal TEXT NOT NULL,
    benefit TEXT NOT NULL,
    cost_estimate VARCHAR(2000) NULL,
    roi_estimate VARCHAR(2000) NULL,
    decision VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    state VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
    concern VARCHAR(30) NOT NULL DEFAULT 'UNDETERMINED',
    action_state VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    action_plan TEXT NULL,
    evidence TEXT NULL,
    review_date DATE NULL,
    decline_reason VARCHAR(60) NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (business_process_id) REFERENCES business_process(business_process_id),
    FOREIGN KEY (document_id) REFERENCES sop_document(document_id),
    FOREIGN KEY (submitter_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE suggestion_communication (
    communication_id CHAR(36) PRIMARY KEY,
    suggestion_id CHAR(36) NOT NULL,
    actor_id INT NOT NULL,
    action VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    details JSON NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (suggestion_id) REFERENCES process_suggestion(suggestion_id),
    FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE suggestion_recipient (
    communication_id CHAR(36) NOT NULL,
    user_id INT NOT NULL,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE_IN_APP',
    PRIMARY KEY (communication_id,user_id),
    FOREIGN KEY (communication_id) REFERENCES suggestion_communication(communication_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE suggestion_revision_link (
    suggestion_id CHAR(36) NOT NULL,
    work_item_id BIGINT NOT NULL,
    linked_by INT NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (suggestion_id,work_item_id),
    FOREIGN KEY (suggestion_id) REFERENCES process_suggestion(suggestion_id),
    FOREIGN KEY (work_item_id) REFERENCES sop_work_item(work_item_id),
    FOREIGN KEY (linked_by) REFERENCES users(id)
) ENGINE=InnoDB;

ALTER TABLE sop_work_item ADD COLUMN initiation_reason VARCHAR(2000) NULL;
