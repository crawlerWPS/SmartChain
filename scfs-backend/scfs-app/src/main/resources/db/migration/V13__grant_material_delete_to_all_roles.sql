-- 所有角色均可删除已上传材料。
-- 已有 VERIFY 权限记录仅追加 delete，避免覆盖原权限或重复写入。
UPDATE schema_common.sys_role_permission
SET permissions = permissions || '["delete"]'::jsonb
WHERE module = 'VERIFY'
  AND NOT (permissions @> '["delete"]'::jsonb);

-- 对尚无 VERIFY 权限记录的角色创建最小删除权限记录。
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT role.id, 'VERIFY', '["delete"]'::jsonb
FROM schema_common.sys_role role
WHERE NOT EXISTS (
    SELECT 1
    FROM schema_common.sys_role_permission permission
    WHERE permission.role_id = role.id
      AND permission.module = 'VERIFY'
);
