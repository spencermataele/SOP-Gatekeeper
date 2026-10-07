CREATE VIEW sop_library_hierarchy AS
SELECT CONCAT('org:',org_id) AS node_key, CAST(NULL AS CHAR(80)) AS parent_key, org_id, org_name AS name, 'Organization' AS kind FROM org
UNION ALL SELECT CONCAT('group:',org_group_id),CONCAT('org:',org_id),org_id,org_group_name,'Group' FROM org_group
UNION ALL SELECT CONCAT('department:',d.department_id),CONCAT('group:',g.org_group_id),g.org_id,d.department_name,'Department' FROM department d JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('subgroup:',s.dept_subgroup_id),CONCAT('department:',d.department_id),g.org_id,s.dept_subgroup_name,'Subgroup' FROM dept_subgroup s JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('family:',f.business_process_family_id),CONCAT('department:',d.department_id),g.org_id,f.business_process_family_name,'Process family' FROM business_process_family f JOIN department d USING(department_id) JOIN org_group g USING(org_group_id)
UNION ALL SELECT CONCAT('process:',p.business_process_id),CONCAT('family:',f.business_process_family_id),g.org_id,p.business_process_name,'Process' FROM business_process p JOIN business_process_family f USING(business_process_family_id) JOIN department d USING(department_id) JOIN org_group g USING(org_group_id);

CREATE TABLE hierarchy_display_code (
 node_key VARCHAR(80) PRIMARY KEY,
 display_code VARCHAR(24) NOT NULL
);
-- Initial codes are allocated by sibling name, then persisted independently of database IDs.
INSERT INTO hierarchy_display_code(node_key,display_code)
SELECT node_key, LPAD(ROW_NUMBER() OVER(PARTITION BY parent_key ORDER BY name,node_key),3,'0') FROM sop_library_hierarchy;
