CREATE TABLE managed_change (
    change_id CHAR(36) PRIMARY KEY,
    origin_suggestion_id CHAR(36) NOT NULL UNIQUE,
    business_process_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    implementation_owner_id INT NOT NULL,
    state VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    plan TEXT NULL,
    evidence TEXT NULL,
    planned_start DATE NULL,
    planned_finish DATE NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY(origin_suggestion_id) REFERENCES process_suggestion(suggestion_id),
    FOREIGN KEY(business_process_id) REFERENCES business_process(business_process_id),
    FOREIGN KEY(implementation_owner_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE managed_change_suggestion (
    change_id CHAR(36) NOT NULL,
    suggestion_id CHAR(36) NOT NULL,
    PRIMARY KEY(change_id,suggestion_id),
    FOREIGN KEY(change_id) REFERENCES managed_change(change_id),
    FOREIGN KEY(suggestion_id) REFERENCES process_suggestion(suggestion_id)
) ENGINE=InnoDB;
CREATE TABLE managed_change_ticket (
    change_id CHAR(36) NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    ticket_number VARCHAR(100) NOT NULL,
    ticket_url VARCHAR(2000) NOT NULL,
    PRIMARY KEY(change_id,system_name,ticket_number),
    FOREIGN KEY(change_id) REFERENCES managed_change(change_id)
) ENGINE=InnoDB;
CREATE TABLE managed_change_event (
    event_id CHAR(36) PRIMARY KEY,
    change_id CHAR(36) NOT NULL,
    actor_id INT NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    previous_value JSON NOT NULL,
    new_value JSON NOT NULL,
    command_payload JSON NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY(change_id) REFERENCES managed_change(change_id),
    FOREIGN KEY(actor_id) REFERENCES users(id)
) ENGINE=InnoDB;

INSERT INTO managed_change(change_id,origin_suggestion_id,business_process_id,title,implementation_owner_id,plan)
SELECT UUID(),s.suggestion_id,s.business_process_id,s.title,pg.owner_user_id,s.proposal
FROM process_suggestion s JOIN process_governance pg USING(business_process_id) WHERE s.decision='ACCEPTED';
INSERT INTO managed_change_suggestion(change_id,suggestion_id)
SELECT change_id,origin_suggestion_id FROM managed_change;
