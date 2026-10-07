-- Preserve ambiguous legacy assignments for administrator review instead of guessing a subdepartment.
ALTER TABLE business_process_family ADD COLUMN dept_subgroup_id INT NULL;
UPDATE business_process_family f JOIN (
 SELECT p.business_process_family_id, MIN(s.dept_subgroup_id) subgroup_id
 FROM business_process p JOIN business_process_dept_subgroup ps USING(business_process_id)
 JOIN dept_subgroup s USING(dept_subgroup_id)
 JOIN business_process_family family USING(business_process_family_id)
 WHERE s.department_id=family.department_id
 GROUP BY p.business_process_family_id HAVING COUNT(DISTINCT s.dept_subgroup_id)=1
) assigned ON assigned.business_process_family_id=f.business_process_family_id
SET f.dept_subgroup_id=assigned.subgroup_id;
UPDATE business_process_family f JOIN (
 SELECT department_id,MIN(dept_subgroup_id) subgroup_id FROM dept_subgroup GROUP BY department_id HAVING COUNT(*)=1
) sole USING(department_id) SET f.dept_subgroup_id=sole.subgroup_id WHERE f.dept_subgroup_id IS NULL;
INSERT INTO dept_subgroup(department_id,dept_subgroup_name)
SELECT DISTINCT f.department_id,'General (migration review)' FROM business_process_family f
WHERE f.dept_subgroup_id IS NULL AND NOT EXISTS (
 SELECT 1 FROM dept_subgroup s WHERE s.department_id=f.department_id AND s.dept_subgroup_name='General (migration review)');
CREATE TABLE family_hierarchy_migration (
 business_process_family_id INT PRIMARY KEY, department_id INT NOT NULL, dept_subgroup_id INT NOT NULL,
 needs_review BOOLEAN NOT NULL, migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO family_hierarchy_migration(business_process_family_id,department_id,dept_subgroup_id,needs_review)
SELECT f.business_process_family_id,f.department_id,COALESCE(f.dept_subgroup_id,
 (SELECT MIN(s.dept_subgroup_id) FROM dept_subgroup s WHERE s.department_id=f.department_id AND s.dept_subgroup_name='General (migration review)')),f.dept_subgroup_id IS NULL
FROM business_process_family f;
UPDATE business_process_family f JOIN family_hierarchy_migration m USING(business_process_family_id) SET f.dept_subgroup_id=m.dept_subgroup_id;
ALTER TABLE dept_subgroup ADD UNIQUE KEY uq_subgroup_department(dept_subgroup_id,department_id);
ALTER TABLE business_process_family MODIFY dept_subgroup_id INT NOT NULL,
 ADD CONSTRAINT fk_family_subdepartment FOREIGN KEY(dept_subgroup_id,department_id) REFERENCES dept_subgroup(dept_subgroup_id,department_id);
CREATE OR REPLACE VIEW sop_library_hierarchy AS
SELECT CONCAT('org:',org_id) AS node_key, CAST(NULL AS CHAR(80)) AS parent_key, org_id, org_name AS name, 'Organization' AS kind FROM org
UNION ALL SELECT CONCAT('group:',org_group_id),CONCAT('org:',org_id),org_id,org_group_name,'Group' FROM org_group
UNION ALL SELECT CONCAT('department:',d.department_id),CONCAT('group:',g.org_group_id),g.org_id,d.department_name,'Department' FROM department d JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('subgroup:',s.dept_subgroup_id),CONCAT('department:',d.department_id),g.org_id,s.dept_subgroup_name,'Subgroup' FROM dept_subgroup s JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('family:',f.business_process_family_id),CONCAT('subgroup:',f.dept_subgroup_id),g.org_id,f.business_process_family_name,'Process family' FROM business_process_family f JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('process:',p.business_process_id),CONCAT('family:',f.business_process_family_id),g.org_id,p.business_process_name,'Process' FROM business_process p JOIN business_process_family f USING(business_process_family_id) JOIN department d USING(department_id) JOIN org_group g USING(org_group_id);


