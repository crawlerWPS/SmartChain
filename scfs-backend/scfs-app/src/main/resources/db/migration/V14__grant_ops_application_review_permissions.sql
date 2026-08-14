-- 运营主管需要查看和处理由风控审核员升级的融资申请。
UPDATE schema_common.sys_role_permission permission
SET permissions = (
    SELECT jsonb_agg(DISTINCT to_jsonb(action_text))
    FROM jsonb_array_elements_text(
        permission.permissions || '["view","approve","reject"]'::jsonb
    ) AS actions(action_text)
)
WHERE permission.module = 'VERIFY'
  AND permission.role_id IN (
      SELECT id FROM schema_common.sys_role WHERE role_code = 'OPS'
  );

INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT role.id, 'VERIFY', '["view","approve","reject"]'::jsonb
FROM schema_common.sys_role role
WHERE role.role_code = 'OPS'
  AND NOT EXISTS (
      SELECT 1
      FROM schema_common.sys_role_permission permission
      WHERE permission.role_id = role.id
        AND permission.module = 'VERIFY'
  );
