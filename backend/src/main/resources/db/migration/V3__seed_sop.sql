INSERT INTO sop
(title, author_id, org_id, org_group_id, department_id, dept_subgroup_id,
 current_business_process_owner_id, current_business_process_owner_position_id,
 business_process_id, business_process_name, business_process_family_id, parent_business_process_id,
 sop_location_path, sop_details, version_id, created_timestamp, updated_timestamp)
VALUES
    ('Receiving & Putaway SOP', 1, 1, 1, 10, 101,
     7, 701,
     500, 'Receiving', 50, 0,
     '/docs/warehouse/receiving', '## Purpose\nDescribe receiving steps...\n\n### Steps\n1. Verify BOL\n2. Inspect pallets\n3. Putaway', 1.0,
     NOW(), NOW());