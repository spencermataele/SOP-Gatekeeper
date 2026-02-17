SET NAMES utf8mb4;
SET foreign_key_checks = 0;

CREATE TABLE IF NOT EXISTS org (
  org_id INT AUTO_INCREMENT PRIMARY KEY,
  org_name VARCHAR(255) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS org_group (
  org_group_id INT AUTO_INCREMENT PRIMARY KEY,
  org_id INT NOT NULL,
  org_group_name VARCHAR(255) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_org_group_org FOREIGN KEY (org_id) REFERENCES org(org_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS department (
  department_id INT AUTO_INCREMENT PRIMARY KEY,
  org_group_id INT NOT NULL,
  department_name VARCHAR(255) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_department_org_group FOREIGN KEY (org_group_id) REFERENCES org_group(org_group_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_subgroup (
  dept_subgroup_id INT AUTO_INCREMENT PRIMARY KEY,
  department_id INT NOT NULL,
  dept_subgroup_name VARCHAR(255) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_dept_subgroup_department FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS business_process_family (
  business_process_family_id INT AUTO_INCREMENT PRIMARY KEY,
  business_process_family_name VARCHAR(255) NOT NULL,
  department_id INT NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_bpf_department FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS business_process (
  business_process_id INT AUTO_INCREMENT PRIMARY KEY,
  business_process_name VARCHAR(255) NOT NULL,
  business_process_family_id INT NOT NULL,
  parent_business_process_id INT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_bp_family FOREIGN KEY (business_process_family_id) REFERENCES business_process_family(business_process_family_id),
  CONSTRAINT fk_bp_parent FOREIGN KEY (parent_business_process_id) REFERENCES business_process(business_process_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS business_process_owner (
  business_process_owner_id INT PRIMARY KEY,
  business_process_owner_name VARCHAR(255) NOT NULL,
  business_process_owner_position_id INT NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS business_process_dept_subgroup (
  business_process_id INT NOT NULL,
  dept_subgroup_id INT NOT NULL,
  PRIMARY KEY (business_process_id, dept_subgroup_id),
  CONSTRAINT fk_bpds_bp FOREIGN KEY (business_process_id) REFERENCES business_process(business_process_id),
  CONSTRAINT fk_bpds_ds FOREIGN KEY (dept_subgroup_id) REFERENCES dept_subgroup(dept_subgroup_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sop (
  sop_id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author_id INT NULL,
  org_id INT NOT NULL,
  org_group_id INT NOT NULL,
  department_id INT NOT NULL,
  dept_subgroup_id INT NULL,
  current_business_process_owner_id INT NULL,
  current_business_process_owner_position_id INT NULL,
  business_process_id INT NULL,
  business_process_name VARCHAR(255) NULL,
  business_process_family_id INT NULL,
  parent_business_process_id INT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  version_id VARCHAR(20) NOT NULL DEFAULT '1.0',
  sop_description LONGTEXT NOT NULL,
  sop_details LONGTEXT NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 0,
  published_timestamp DATETIME(6) NULL,
  supersedes_sop_id INT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  INDEX idx_sop_business_process_id (business_process_id),
  CONSTRAINT fk_sop_supersedes FOREIGN KEY (supersedes_sop_id) REFERENCES sop(sop_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS change_request (
  change_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  original_sop_id INT NOT NULL,
  proposed_sop_id INT NOT NULL,
  requested_by_user_id INT NOT NULL,
  change_summary LONGTEXT NULL,
  change_reason LONGTEXT NULL,
  change_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_cr_original_sop FOREIGN KEY (original_sop_id) REFERENCES sop(sop_id),
  CONSTRAINT fk_cr_proposed_sop FOREIGN KEY (proposed_sop_id) REFERENCES sop(sop_id),
  CONSTRAINT fk_cr_requestor FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

ALTER TABLE sop
  ADD COLUMN change_request_id BIGINT NULL,
  ADD CONSTRAINT fk_sop_change_request FOREIGN KEY (change_request_id) REFERENCES change_request(change_request_id);

CREATE TABLE IF NOT EXISTS change_approval (
  change_approval_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  change_request_id BIGINT NOT NULL,
  approver_user_id INT NOT NULL,
  approver_role VARCHAR(50) NOT NULL,
  approval_decision VARCHAR(30) NOT NULL DEFAULT 'IN_REVIEW',
  comments LONGTEXT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ca_change_request FOREIGN KEY (change_request_id) REFERENCES change_request(change_request_id),
  CONSTRAINT fk_ca_approver FOREIGN KEY (approver_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification_log (
  notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  change_request_id BIGINT NULL,
  user_id INT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  created_timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_timestamp DATETIME(6) NULL,
  CONSTRAINT fk_nl_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_nl_change_request FOREIGN KEY (change_request_id) REFERENCES change_request(change_request_id)
) ENGINE=InnoDB;

SET foreign_key_checks = 1;
