CREATE TABLE governance_configuration_audit (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id INT NOT NULL,
    business_process_id INT NULL,
    action VARCHAR(50) NOT NULL,
    previous_value JSON NULL,
    new_value JSON NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_config_audit_actor FOREIGN KEY(actor_id) REFERENCES users(id),
    CONSTRAINT fk_config_audit_process FOREIGN KEY(business_process_id) REFERENCES business_process(business_process_id)
) ENGINE=InnoDB;
