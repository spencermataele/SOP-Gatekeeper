CREATE TABLE sop (
    sop_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author_id INT NOT NULL,
    org_id INT NOT NULL,
    org_group_id INT NOT NULL,
    department_id INT NOT NULL,
    dept_subgroup_id INT NOT NULL,
    current_process_owner_id INT NOT NULL,
    current_process_owner_position_id INT NOT NULL,
    process_id INT NOT NULL,
    process_name VARCHAR(255) NOT NULL,
    process_family_id INT NOT NULL,
    parent_process_id INT NOT NULL,
    sop_location_path VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    version_id FLOAT NOT NULL,
    sop_details LONGTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE sop_attribute (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sop_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    value TEXT,
    CONSTRAINT fk_sop_attr_sop FOREIGN KEY (sop_id) REFERENCES sop(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;