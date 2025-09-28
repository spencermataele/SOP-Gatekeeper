DROP TABLE IF EXISTS process_dept_subgroup;

CREATE TABLE process_dept_subgroup (
   process_id INT NOT NULL,
   dept_subgroup_id INT NOT NULL,
   PRIMARY KEY (process_id, dept_subgroup_id),
   CONSTRAINT fk_pds_p FOREIGN KEY (process_id) REFERENCES business_process(process_id),
   CONSTRAINT fk_pds_sg FOREIGN KEY (dept_subgroup_id) REFERENCES dept_subgroup(dept_subgroup_id)
);
