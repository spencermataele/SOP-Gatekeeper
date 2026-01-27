ALTER TABLE sop
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE sop
    ADD COLUMN published_timestamp TIMESTAMP NULL;

ALTER TABLE sop
    ADD COLUMN change_request_id BIGINT NULL;

ALTER TABLE sop
    ADD CONSTRAINT fk_sop_change_request
        FOREIGN KEY (change_request_id)
            REFERENCES change_request(change_request_id);

CREATE INDEX idx_sop_active
    ON sop(is_active);

CREATE INDEX idx_sop_change_request
    ON sop(change_request_id);
