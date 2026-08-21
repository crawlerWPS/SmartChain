-- 用户、角色删除按钮纳入角色菜单权限配置。
INSERT INTO schema_common.sys_menu
    (parent_id, menu_name, menu_code, menu_type, permission, sort, visible, status)
SELECT parent.id, button.menu_name, button.menu_code, 'BUTTON', 'USER.delete', button.sort, 1, 1
FROM schema_common.sys_menu parent
JOIN (VALUES
    ('system.user', '删除用户', 'system:user:delete', 10),
    ('system.role', '删除角色', 'system:role:delete', 10)
) AS button(parent_code, menu_name, menu_code, sort)
    ON parent.menu_code = button.parent_code
ON CONFLICT (menu_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    permission = EXCLUDED.permission,
    sort = EXCLUDED.sort,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 管理员默认获得新增按钮；其他角色可由管理员按需分配。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_menu menu
  ON menu.menu_code IN ('system:user:delete', 'system:role:delete')
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;
