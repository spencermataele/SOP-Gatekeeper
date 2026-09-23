-- Read-only inventory for the lifecycle migration design; not a Flyway migration.
-- Run against the MVP schema. No document content or credentials are returned.
START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY;

SELECT 'inventory' AS report, 'sop' AS entity, COUNT(*) AS row_count FROM sop
UNION ALL SELECT 'inventory', 'change_request', COUNT(*) FROM change_request
UNION ALL SELECT 'inventory', 'change_approval', COUNT(*) FROM change_approval
UNION ALL SELECT 'inventory', 'notification_log', COUNT(*) FROM notification_log
UNION ALL SELECT 'inventory', 'business_process', COUNT(*) FROM business_process
UNION ALL SELECT 'inventory', 'users', COUNT(*) FROM users;

SELECT status, is_active, COUNT(*) AS row_count FROM sop GROUP BY status, is_active;

SELECT s.sop_id, 'MISSING_PROCESS' AS issue
FROM sop s LEFT JOIN business_process p ON p.business_process_id = s.business_process_id
WHERE p.business_process_id IS NULL
UNION ALL
SELECT s.sop_id, 'PROCESS_METADATA_MISMATCH'
FROM sop s JOIN business_process p ON p.business_process_id = s.business_process_id
WHERE NOT (s.business_process_family_id <=> p.business_process_family_id)
   OR NOT (s.business_process_name <=> p.business_process_name)
   OR NOT (s.parent_business_process_id <=> p.parent_business_process_id)
UNION ALL
SELECT s.sop_id, 'ORG_HIERARCHY_MISMATCH'
FROM sop s
LEFT JOIN org o ON o.org_id = s.org_id
LEFT JOIN org_group g ON g.org_group_id = s.org_group_id
LEFT JOIN department d ON d.department_id = s.department_id
LEFT JOIN dept_subgroup ds ON ds.dept_subgroup_id = s.dept_subgroup_id
WHERE o.org_id IS NULL OR g.org_group_id IS NULL OR d.department_id IS NULL
   OR NOT (g.org_id <=> s.org_id) OR NOT (d.org_group_id <=> s.org_group_id)
   OR (s.dept_subgroup_id IS NOT NULL AND
       (ds.dept_subgroup_id IS NULL OR NOT (ds.department_id <=> s.department_id)))
UNION ALL
SELECT s.sop_id, 'PROCESS_ORG_MISMATCH'
FROM sop s
JOIN business_process p ON p.business_process_id = s.business_process_id
LEFT JOIN business_process_family f ON f.business_process_family_id = p.business_process_family_id
LEFT JOIN department d ON d.department_id = f.department_id
LEFT JOIN org_group g ON g.org_group_id = d.org_group_id
WHERE g.org_id IS NULL OR NOT (g.org_id <=> s.org_id)
UNION ALL
SELECT s.sop_id, 'UNRESOLVED_OWNER_REFERENCE'
FROM sop s LEFT JOIN business_process_owner po
  ON po.business_process_owner_id = s.current_business_process_owner_id
WHERE po.business_process_owner_id IS NULL
UNION ALL
SELECT s.sop_id, 'MISSING_AUTHOR'
FROM sop s LEFT JOIN users u ON u.id = s.author_id WHERE u.id IS NULL
UNION ALL
SELECT s.sop_id, 'ACTIVE_STATUS_CONFLICT'
FROM sop s WHERE (s.status = 'ACTIVE' AND s.is_active <> 1)
             OR (s.status <> 'ACTIVE' AND s.is_active <> 0)
UNION ALL
SELECT s.sop_id, 'INVALID_PREDECESSOR'
FROM sop s LEFT JOIN sop prev ON prev.sop_id = s.supersedes_sop_id
WHERE s.supersedes_sop_id IS NOT NULL AND
 (prev.sop_id IS NULL OR prev.sop_id = s.sop_id
  OR NOT (prev.org_id <=> s.org_id)
  OR NOT (prev.business_process_id <=> s.business_process_id))
ORDER BY sop_id, issue;

-- IDs matching users are candidates for review, never proof of identity.
SELECT po.business_process_owner_id AS legacy_owner_id,
       u.id AS same_number_user_id,
       (po.business_process_owner_name = u.full_name) AS same_name
FROM business_process_owner po LEFT JOIN users u ON u.id = po.business_process_owner_id;

SELECT business_process_id, COUNT(DISTINCT current_business_process_owner_id) AS owner_count,
       SUM(current_business_process_owner_id IS NULL) AS missing_owner_count
FROM sop GROUP BY business_process_id
HAVING owner_count <> 1 OR missing_owner_count > 0;

SELECT cr.change_request_id, cr.change_status, 'REQUEST_LINK_MISMATCH' AS issue
FROM change_request cr
JOIN sop original ON original.sop_id = cr.original_sop_id
JOIN sop proposed ON proposed.sop_id = cr.proposed_sop_id
WHERE NOT (original.org_id <=> proposed.org_id)
   OR NOT (original.business_process_id <=> proposed.business_process_id)
   OR NOT (proposed.supersedes_sop_id <=> original.sop_id)
   OR NOT (proposed.change_request_id <=> cr.change_request_id);

-- Complete graph/cycle checks and content checksums belong to the converter rehearsal.
-- This inventory is not a certification that the database is migration-ready.
ROLLBACK;
