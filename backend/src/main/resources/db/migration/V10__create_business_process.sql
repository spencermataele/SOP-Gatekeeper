CREATE TABLE business_process (
    process_id INT AUTO_INCREMENT PRIMARY KEY,
    process_name VARCHAR(255) NOT NULL,
    process_family_id INT NOT NULL,
    parent_process_id INT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bp_family FOREIGN KEY (process_family_id) REFERENCES process_family(process_family_id),
    CONSTRAINT fk_bp_parent FOREIGN KEY (parent_process_id) REFERENCES business_process(process_id)
);
CREATE UNIQUE INDEX uq_process_name_in_family ON business_process(process_family_id, process_name);
CREATE INDEX idx_p_parent ON business_process(parent_process_id);
