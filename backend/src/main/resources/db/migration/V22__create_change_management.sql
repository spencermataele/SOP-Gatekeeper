CREATE TABLE change_request (
    change_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sop_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    change_summary LONGTEXT NOT NULL,
    change_reason LONGTEXT NOT NULL,
    change_status VARCHAR(30) NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_change_request_sop
        FOREIGN KEY (sop_id) REFERENCES sop(id),

    CONSTRAINT fk_change_request_user
        FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE TABLE change_approval (
    change_approval_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_request_id BIGINT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    approver_role VARCHAR(50) NOT NULL,
    approval_decision VARCHAR(30) NOT NULL,
    comments LONGTEXT,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_change_approval
        UNIQUE (change_request_id, approver_user_id),

    CONSTRAINT fk_change_approval_change_request
        FOREIGN KEY (change_request_id) REFERENCES change_request(change_request_id),

    CONSTRAINT fk_change_approval_user
        FOREIGN KEY (approver_user_id) REFERENCES users(id)

);

CREATE TABLE notification_log (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_request_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_type VARCHAR(30) NOT NULL,

    CONSTRAINT fk_notification_change_request
        FOREIGN KEY (change_request_id) REFERENCES change_request(change_request_id),

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

/*Indexes*/

CREATE INDEX idx_change_request_sop
    ON change_request(sop_id);

CREATE INDEX idx_change_request_user
    ON change_request(requested_by_user_id);

CREATE INDEX idx_change_approval_request
    ON change_approval(change_request_id);

CREATE INDEX idx_change_approval_user
    ON change_approval(approver_user_id);

CREATE INDEX idx_notification_request
    ON notification_log(change_request_id);

CREATE INDEX idx_notification_user
    ON notification_log(user_id);



