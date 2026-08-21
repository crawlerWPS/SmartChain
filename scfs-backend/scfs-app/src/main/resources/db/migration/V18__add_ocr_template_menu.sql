-- OCR 识别模板纳入规则配置菜单树。
INSERT INTO schema_common.sys_menu
    (parent_id, menu_name, menu_code, menu_type, path, component, permission, icon, sort, visible, status)
SELECT parent.id, 'OCR识别模板', 'rule.ocr-template', 'MENU',
       '/rule/ocr-template', 'rule/ocr-template', 'RULE.view', 'ScanOutlined', 4, 1, 1
FROM schema_common.sys_menu parent
WHERE parent.menu_code = 'rule'
ON CONFLICT (menu_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    path = EXCLUDED.path,
    component = EXCLUDED.component,
    permission = EXCLUDED.permission,
    icon = EXCLUDED.icon,
    sort = EXCLUDED.sort,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 按现有 RULE.view API 权限初始化菜单授权，保持升级前可见范围。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_role_permission permission ON permission.role_id = role.id
JOIN schema_common.sys_menu menu ON menu.menu_code = 'rule.ocr-template'
WHERE permission.module = 'RULE'
  AND permission.permissions ? 'view'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 同时补齐规则配置父级，兼容此前没有任何规则菜单的新角色。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, parent.id
FROM schema_common.sys_role role
JOIN schema_common.sys_role_permission permission ON permission.role_id = role.id
JOIN schema_common.sys_menu parent ON parent.menu_code = 'rule'
WHERE permission.module = 'RULE'
  AND permission.permissions ? 'view'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_menu menu ON menu.menu_code = 'rule.ocr-template'
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;
