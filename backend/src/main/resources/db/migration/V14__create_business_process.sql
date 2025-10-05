DROP TABLE IF EXISTS business_process;

CREATE TABLE business_process (
    business_process_id INT AUTO_INCREMENT PRIMARY KEY,
    business_process_name VARCHAR(255) NOT NULL,
    business_process_family_id INT NOT NULL,
    parent_business_process_id INT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bps_family FOREIGN KEY (business_process_family_id) REFERENCES business_process_family(business_process_family_id),
    CONSTRAINT fk_bps_parent FOREIGN KEY (parent_business_process_id) REFERENCES business_process(business_process_id)
);
CREATE UNIQUE INDEX uq_process_name_in_a_family ON business_process(business_process_family_id, business_process_name);
CREATE INDEX idx_bp_parent ON business_process(parent_business_process_id);