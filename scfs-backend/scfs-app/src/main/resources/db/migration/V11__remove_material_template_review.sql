UPDATE schema_common.material_checklist_template
SET status = 'ENABLED', checker_id = NULL, checked_at = NULL, reject_reason = NULL,
    updated_at = NOW()
WHERE status IN ('DRAFT', 'PENDING', 'REJECTED', 'APPROVED');

DELETE FROM schema_common.sys_role_menu
WHERE menu_id IN (SELECT id FROM schema_common.sys_menu WHERE menu_code = 'template:approve');
DELETE FROM schema_common.sys_menu WHERE menu_code = 'template:approve';
