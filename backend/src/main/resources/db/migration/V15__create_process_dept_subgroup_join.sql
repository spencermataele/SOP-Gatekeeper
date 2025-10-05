DROP TABLE IF EXISTS business_process_dept_subgroup;

CREATE TABLE business_process_dept_subgroup (
    business_process_id INT NOT NULL,
    dept_subgroup_id INT NOT NULL,
    PRIMARY KEY (business_process_id, dept_subgroup_id),
    CONSTRAINT fk_pdsg_p FOREIGN KEY (business_process_id) REFERENCES business_process(business_process_id),
    CONSTRAINT fk_pdsg_sg FOREIGN KEY (dept_subgroup_id) REFERENCES dept_subgroup(dept_subgroup_id)
);