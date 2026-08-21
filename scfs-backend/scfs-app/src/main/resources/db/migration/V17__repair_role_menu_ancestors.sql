-- 修复历史角色授权：任何已授权子菜单/按钮都必须包含完整父级路径。
WITH RECURSIVE role_menu_ancestors AS (
    SELECT rm.role_id, menu.id AS menu_id, menu.parent_id
    FROM schema_common.sys_role_menu rm
    JOIN schema_common.sys_menu menu ON menu.id = rm.menu_id

    UNION

    SELECT ancestor.role_id, parent.id AS menu_id, parent.parent_id
    FROM role_menu_ancestors ancestor
    JOIN schema_common.sys_menu parent ON parent.id = ancestor.parent_id
    WHERE ancestor.parent_id IS NOT NULL AND ancestor.parent_id > 0
)
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id
FROM role_menu_ancestors
ON CONFLICT (role_id, menu_id) DO NOTHING;
