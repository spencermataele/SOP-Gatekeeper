CREATE TABLE IF NOT EXISTS business_process_owner (
     business_process_owner_id INT PRIMARY KEY AUTO_INCREMENT,
     business_process_owner_name VARCHAR(255) NOT NULL,
     business_process_owner_position_id INT NOT NULL,
     created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     parent_business_process_owner_id INT NULL,
     CONSTRAINT fk_process_owner_parent
         FOREIGN KEY (parent_business_process_owner_id) REFERENCES business_process_owner(business_process_owner_id)
             ON DELETE SET NULL ON UPDATE CASCADE
);

-- Index for parent lookup
CREATE INDEX idx_process_owner_parent ON business_process_owner(parent_business_process_owner_id);