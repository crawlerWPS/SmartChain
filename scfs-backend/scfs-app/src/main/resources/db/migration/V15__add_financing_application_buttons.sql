-- 融资申请按钮权限：纳入角色菜单树配置。
INSERT INTO schema_common.sys_menu
    (parent_id, menu_name, menu_code, menu_type, permission, sort, visible, status)
SELECT parent.id, button.menu_name, button.menu_code, 'BUTTON', button.permission, button.sort, 1, 1
FROM schema_common.sys_menu parent
CROSS JOIN (VALUES
    ('新增融资申请', 'application:create', 'VERIFY.create', 1),
    ('拒绝融资申请', 'application:reject', 'VERIFY.reject', 2),
    ('通过融资申请', 'application:approve', 'VERIFY.approve', 3),
    ('提交运营主管', 'application:escalate', 'VERIFY.approve', 4)
) AS button(menu_name, menu_code, permission, sort)
WHERE parent.menu_code = 'audit.application'
ON CONFLICT (menu_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    permission = EXCLUDED.permission,
    sort = EXCLUDED.sort,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 按原 API 权限初始化按钮授权，保证升级后现有角色的操作能力不变。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_role_permission permission ON permission.role_id = role.id
JOIN schema_common.sys_menu menu ON
    (menu.menu_code = 'application:create' AND permission.permissions ? 'create')
    OR (menu.menu_code = 'application:reject' AND permission.permissions ? 'reject')
    OR (menu.menu_code = 'application:approve' AND permission.permissions ? 'approve')
    OR (menu.menu_code = 'application:escalate'
        AND role.role_code = 'RCO' AND permission.permissions ? 'approve')
WHERE permission.module = 'VERIFY'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 管理员保留全部新按钮。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_menu menu ON menu.menu_code IN (
    'application:create', 'application:reject', 'application:approve', 'application:escalate'
)
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;
