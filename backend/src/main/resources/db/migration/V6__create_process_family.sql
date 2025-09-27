CREATE TABLE process_family (
    process_family_id INT AUTO_INCREMENT PRIMARY KEY,
    process_family_name VARCHAR(255) NOT NULL,
    department_id INT NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pf_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);
CREATE INDEX idx_pf_department ON process_family(department_id);
CREATE UNIQUE INDEX uq_pf_name_per_dept ON process_family(department_id, process_family_name);
