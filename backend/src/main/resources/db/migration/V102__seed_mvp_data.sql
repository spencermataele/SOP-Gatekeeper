-- V2__seed_mvp_data.sql
-- Minimal seed data to make the MVP usable immediately after migrating.

SET NAMES utf8mb4;
SET foreign_key_checks = 0;

-- Org Hierarchy
INSERT INTO org (org_id, org_name) VALUES
  (1, 'Woven - SOP Gatekeeper')
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name);

INSERT INTO org_group (org_group_id, org_id, org_group_name) VALUES
  (1, 1, 'IT - Information Technology')
ON DUPLICATE KEY UPDATE org_group_name = VALUES(org_group_name), org_id = VALUES(org_id);

INSERT INTO department (department_id, org_group_id, department_name) VALUES
  (10, 1, 'Process Governance')
ON DUPLICATE KEY UPDATE department_name = VALUES(department_name), org_group_id = VALUES(org_group_id);

INSERT INTO dept_subgroup (dept_subgroup_id, department_id, dept_subgroup_name) VALUES
  (101, 10, 'Standard Operating Procedures'),
  (102, 10, 'Admin')
ON DUPLICATE KEY UPDATE dept_subgroup_name = VALUES(dept_subgroup_name), department_id = VALUES(department_id);

-- Users
INSERT INTO users (id, username, email, password, full_name, role) VALUES
  (1, 'Admin', 'admin@woven.com', '$2a$10$GruJr8hVNWnPUljSLmTdruoHohapg8G9VBsiBKTsTCzbV.zIGTs3e', 'System Admin', 'ADMIN'),
  (2, 'JohnA', 'johna@woven.com', '$2a$10$GruJr8hVNWnPUljSLmTdruoHohapg8G9VBsiBKTsTCzbV.zIGTs3e', 'John Anderson', 'USER'),
  (3, 'JohnB', 'johnb@woven.com', '$2a$10$GruJr8hVNWnPUljSLmTdruoHohapg8G9VBsiBKTsTCzbV.zIGTs3e', 'John Benito', 'USER'),
  (4, 'JaneA', 'janea@woven.com', '$2a$10$GruJr8hVNWnPUljSLmTdruoHohapg8G9VBsiBKTsTCzbV.zIGTs3e', 'Jane Anderson', 'USER')
ON DUPLICATE KEY UPDATE
  username=VALUES(username), email=VALUES(email), full_name=VALUES(full_name), role=VALUES(role);

INSERT INTO business_process_owner (business_process_owner_id, business_process_owner_name, business_process_owner_position_id) VALUES
  (1, 'System Admin', 100),
  (2, 'John Anderson', 200),
  (3, 'John Benito', 200),
  (4, 'Jane Anderson', 200)
ON DUPLICATE KEY UPDATE
  business_process_owner_id=VALUES(business_process_owner_id), business_process_owner_name=VALUES(business_process_owner_name), business_process_owner_position_id=VALUES(business_process_owner_position_id);

-- Business process
INSERT INTO business_process_family (business_process_family_id, business_process_family_name, department_id) VALUES
  (1, 'Admin', 10),
  (2, 'Users', 10)
ON DUPLICATE KEY UPDATE business_process_family_name=VALUES(business_process_family_name), department_id=VALUES(department_id);

INSERT INTO business_process (business_process_id, business_process_name, business_process_family_id, parent_business_process_id) VALUES
  (1, 'Initial Setup', 1, NULL),
  (2, 'How To''s', 2, NULL)
ON DUPLICATE KEY UPDATE business_process_name=VALUES(business_process_name), business_process_family_id=VALUES(business_process_family_id), parent_business_process_id=VALUES(parent_business_process_id);

INSERT IGNORE INTO business_process_dept_subgroup (business_process_id, dept_subgroup_id) VALUES
  (1, 102),
  (2, 101);

-- SOPs
INSERT INTO sop (
    sop_id,
    title,
    author_id,
    org_id,
    org_group_id,
    department_id,
    dept_subgroup_id,
    current_business_process_owner_id,
    current_business_process_owner_position_id,
    business_process_family_id,
    business_process_id,
    business_process_name,
    parent_business_process_id,
    version_id,
    sop_description,
    sop_details,
    is_active,
    status,
    published_timestamp,
    supersedes_sop_id,
    change_request_id
) VALUES
    (
        1,
        'Update Admin Password',
        1,
        1,
        1,
        10,
        102,
        1,
        100,
        1,
        2,
        'Initial Setup',
        NULL,
        '1.0',
        'This SOP describes how to update the Admin password during initial application setup.',
        'Step 1. Login\n
            Who: Admin\n
            Where: Workstation\n
            What: Login using the username and password provided found in your "Getting Started - Administrator" user guide\n
        Step 2. Access Users Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Once successfully logged in, navigate to the Admin page.  Then select the Users option.\n
        Step 3. Edit Admin Password\n
            Who: Admin\n
            Where: Workstation\n
            What: Locate the Admin user row in the All Users list.  Click edit button. In the edit form, update password field to desired password.
                Be sure to record this password in writing and store in a secure location.  \n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        2, -- sop_id
        'Setup Org Structure', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        101, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        1, -- family_id_1_Admin_2_Users
        1, -- process_id_1_Init_2_HTs
        'Initial Setup', -- process_name
        NULL, -- parent
        '1.0', -- version
        'This SOP describes how to set up your organization''s hierarchy structure as it relates to business processes.', -- description
        'Step 1. Access Organizations Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Organizations option.\n
        Step 2. Add Orgs\n
            Who: Admin\n
            Where: Workstation\n
            What: Simply type the name or your org (typically your company''s name) into the Org Name field and click Add Org.
                Repeat for all additional orgs.\n
        Step 3. Access Org Groups Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Org Groups option.\n
        Step 4. Add Org Groups\n
            Who: Admin\n
            Where: Workstation\n
            What: Select the org for the org group you wish to create from the Org drop menu.
                Simply enter the org group name into the Org Group Name field and click Add Org Group.
                Repeat for all additional org groups.\n
        Step 5. Access Departments Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Departments option.\n
        Step 6. Add Departments\n
            Who: Admin\n
            Where: Workstation\n
            What: Select the org group for the department you wish to create from the Org Group drop menu.
                Simply enter the department name into the Department Name field and click Add Department.
                Repeat for all additional departments.\n
        Step 7. Access Department Subgroup Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Department Subgroups option.\n
        Step 8. Add Department Subgroup\n
            Who: Admin\n
            Where: Workstation\n
            What: Select the department for the subgroup you wish to create from the Department drop menu.
                Simply enter the subgroup name into the Subgroup Name field and click Add Subgroup.
                Repeat for all additional subgroups.\n
        ',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        3, -- sop_id
        'Setup Process Families, Processes, and Process Owners', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        101, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        1, -- family_id_1_Admin_2_Users
        1, -- process_id_1_Init_2_HTs
        'Initial Setup', -- process_name
        NULL, -- parent
        '1.0', -- version
        'This SOP describes how to setup your organization''s business process tree', -- description
        'Step 1. Access Business Process Families Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Business Process Families option.\n
        Step 2. Add Business Process Families\n
            Who: Admin\n
            Where: Workstation\n
            What: Select New Family button.  Enter the name fo your process family in the Name field. Select the corresponding
                department for this process family.  Click Save.  Repeat for all additional process families.\n
        Step 1. Access Business Processes Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Business Processes option.\n
        Step 2. Add Business Processes\n
            Who: Admin\n
            Where: Workstation\n
            What: Select New Process button.  Enter the name fo your process in the Name field. Select the corresponding
                process family for this process.  If applicable, select this processes parent process from the drop menu.
                A parent process is an existing process within the same process family that immediately precedes a process
                in it''s overall process stream.  Click Save.  Repeat for all additional process families.\n
        Step 1. Access Process Owners Page\n
            Who: Admin\n
            Where: Workstation\n
            What: Navigate to the Admin page.  Then select the Process Owners option.\n
        Step 2. Add Process Owners\n
            Who: Admin\n
            Where: Workstation\n
            What: Select a user from the Users drop menu.  Then enter the user''s position id.  Click Create.
                Repeat for all additional process families.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        4, -- sop_id
        'Create a new SOP', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        102, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        2, -- family_id_1_Admin_2_Users
        2, -- process_id_1_Init_2_HTs
        'How To''s', -- process_name
        NULL, -- parent
        '1.0', -- version
        'The purpose of this SOP is to describe how Process Owners can create a new SOP in SOP Gatekeeper.', -- description
        'Step 1. Access the Create SOPs Form\n
            Who: Process Owner\n
            Where: Workstation\n
            What: Navigate to SOPs.  Click Create New SOP button\n
        Step 2. Fill out SOP Form\n
            Who: Process Owner\n
            Where: Workstation\n
            What: Fill in all the required fields with applicable information.  Be sure to include a parent process if one exists.
                A parent process is an existing process within the same process family that immediately precedes a process
                in it''s overall process stream. Fill out descriptive step instructions by selecting Add Step for each step needed.
                Click Create SOP.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        5, -- sop_id
        'Update an SOP - Request Change', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        102, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        2, -- family_id_1_Admin_2_Users
        2, -- process_id_1_Init_2_HTs
        'How To''s', -- process_name
        4, -- parent
        '1.0', -- version
        'This SOP describes how to update current SOPs by initiating a change workflow using SOP Gatekeeper.', -- description
        'Step 1. Access the Edit SOP Form\n
            Who: Any User\n
            Where: Workstation\n
            What: Navigate to SOPs.  Click Request Change button next to the SOP you wish to update.
        Step 2. Edit SOP\n
            Who: Any User\n
            Where: Workstation\n
            What: Update or fill in all required fields.  Click Save Changes button.\n
        Step 3. Submit SOP Update Draft\n
            Who: Any User\n
            Where: Workstation\n
            What: Access the Change Dashboard from the main menu.  Find your SOP update draft in the My Requests list.
                Select Submit to advanced your draft to "In Review" status.  This will trigger a notification for the
                SOP''s change-authorizer(s) to review your changes.\n
        Step 4. Monitor Change Request Status\n
            Who: Any User\n
            Where: Workstation\n
            What: You will receive a notification once your change request has been approved or rejected.  This updated status will be
            displayed in your Change Dashboard.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        6, -- sop_id
        'Approve A Requested SOP Change', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        102, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        2, -- family_id_1_Admin_2_Users
        2, -- process_id_1_Init_2_HTs
        'How To''s', -- process_name
        5, -- parent
        '1.0', -- version
        'This SOP describes how a process owner approves requested changes to their process''s SOP', -- description
        'Step 1. Access Review Change Request Page\n
            Who: Process Owner\n
            Where: Workstation\n
            What: Select Change Dashboard from the main menu.  Find the change from the Pending My Approval list and click
                the Review button.\n
        Step 2. Review Changes and Approve/Reject\n
            Who: Process Owner\n
            Where: Workstation\n
            What: Review the updated SOP draft displayed.  Once you''ve decided to approve or deny this request, enter comments in the Comments
                field and select the Approve & Publish or Reject button.  This will notify the request that the request''s status has been updated.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        7, -- sop_id
        'Review Org Hierarchy Report', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        102, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        2, -- family_id_1_Admin_2_Users
        2, -- process_id_1_Init_2_HTs
        'How To''s', -- process_name
        NULL, -- parent
        '1.0', -- version
        'This SOP describes how review the Org Hierarchy Report in SOP Gatekeeper.', -- description
        'Step 1. Select Org Hierarchy Report\n
            Who: Any User\n
            Where: Workstation\n
            What: Select Reports from the main menu.  Find Org Hierarchy Report from the Reports list and click Open button.\n
        Step 2. Interacting With The Report\n
            Who: Any User\n
            Where: Workstation\n
            What: This report includes filters that you can use by filling in any fields and selecting Apply.  Reports can also be
            exported to CSV if the report author enables this feature before publishing the report.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    ),
    (
        8, -- sop_id
        'Example SOP For Change Management Workflow', -- title
        1, -- author_id
        1, -- org_id
        1, -- org_group
        10, -- department
        102, -- subg_101_Admin_102_Users
        1, -- owner_id
        100, -- owner_job_id
        2, -- family_id_1_Admin_2_Users
        2, -- process_id_1_Init_2_HTs
        'How To''s', -- process_name
        NULL, -- parent
        '1.0', -- version
        'This SOP serves as a testing SOP for users to test and observe SOP Gatekeeper''s SOP change management workflow.', -- description
        'Step 1. Edit Me\n
            Who: Any User\n
            Where: Workstation\n
            What: Make edits to this SOP so that could can track the change request through the SOP change management workflow.\n',
        1,
        'ACTIVE',
        NOW(6),
        NULL,
        NULL
    )
ON DUPLICATE KEY UPDATE
  title=VALUES(title), sop_description=VALUES(sop_description), sop_details=VALUES(sop_details),
  is_active=VALUES(is_active), status=VALUES(status), supersedes_sop_id=VALUES(supersedes_sop_id);

-- Make sure not to collide with seeded rows
ALTER TABLE users AUTO_INCREMENT = 5;
ALTER TABLE sop AUTO_INCREMENT = 10;

SET foreign_key_checks = 1;
