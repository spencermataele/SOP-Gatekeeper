CREATE TABLE business_process_family (
    business_process_family_id INT AUTO_INCREMENT PRIMARY KEY,
    business_process_family_name VARCHAR(255) NOT NULL,
    department_id INT NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bpf_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);
CREATE INDEX idx_bpf_department ON business_process_family(department_id);
CREATE UNIQUE INDEX uq_bpf_name_per_dept ON business_process_family(department_id, business_process_family_name);