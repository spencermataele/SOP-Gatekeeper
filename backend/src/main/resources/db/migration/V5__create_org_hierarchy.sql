-- ORG
CREATE TABLE IF NOT EXISTS org (
    org_id INT PRIMARY KEY AUTO_INCREMENT,
    org_name VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ORG GROUP
CREATE TABLE IF NOT EXISTS org_group (
    org_group_id INT PRIMARY KEY AUTO_INCREMENT,
    org_group_name VARCHAR(255) NOT NULL,
    org_id INT NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_group_org FOREIGN KEY (org_id) REFERENCES org(org_id)
);

CREATE INDEX idx_org_group_org_id ON org_group(org_id);

-- DEPARTMENT (child of ORG GROUP)
CREATE TABLE IF NOT EXISTS department (
    department_id INT PRIMARY KEY AUTO_INCREMENT,
    department_name VARCHAR(255) NOT NULL,
    org_group_id INT NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_department_group FOREIGN KEY (org_group_id) REFERENCES org_group(org_group_id)
);
CREATE INDEX idx_department_group_id ON department(org_group_id);

-- DEPT SUBGROUP (child of DEPARTMENT)
CREATE TABLE IF NOT EXISTS dept_subgroup (
    dept_subgroup_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_subgroup_name VARCHAR(255) NOT NULL,
    department_id INT NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subgroup_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);
CREATE INDEX idx_subgroup_department_id ON dept_subgroup(department_id);
