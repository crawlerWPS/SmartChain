-- 菜单名称与实际页面保持一致。
UPDATE schema_common.sys_menu
SET menu_name = '规则管理', updated_at = NOW()
WHERE menu_code = 'rule.definition';

-- 审计查询页面是单页，不保留不存在的二级菜单。
DELETE FROM schema_common.sys_role_menu
WHERE menu_id IN (
    SELECT id FROM schema_common.sys_menu
    WHERE menu_code IN ('audit-trail.log', 'audit-trail.trace')
);
DELETE FROM schema_common.sys_menu
WHERE menu_code IN ('audit-trail.log', 'audit-trail.trace');

-- 按页面实际按钮名称登记所有数据变更操作。
INSERT INTO schema_common.sys_menu
    (parent_id, menu_name, menu_code, menu_type, permission, sort, visible, status)
SELECT parent.id, button.menu_name, button.menu_code, 'BUTTON', button.permission, button.sort, 1, 1
FROM schema_common.sys_menu parent
JOIN (VALUES
    ('graph.relations', '导入关系', 'graph:import', 'GRAPH.update', 10),
    ('graph.role', '重新计算分析', 'graph-role:recalculate', 'GRAPH.update', 10),
    ('graph.position', '重新计算分析', 'graph-position:recalculate', 'GRAPH.update', 10),
    ('graph.abnormal', '解除', 'graph-abnormal:resolve', 'GRAPH.update', 10),

    ('audit.application', '新建申请', 'application:create', 'VERIFY.create', 10),
    ('audit.application', '提交申请', 'application:submit', 'VERIFY.update', 11),
    ('audit.application', '驳回', 'application:reject', 'VERIFY.reject', 12),
    ('audit.application', '通过', 'application:approve', 'VERIFY.approve', 13),
    ('audit.application', '无法判断，升级运营主管', 'application:escalate', 'VERIFY.approve', 14),
    ('audit.material', '点击或拖拽文件上传', 'material:upload', 'VERIFY.create', 10),
    ('audit.material', '重新识别', 'material:recognize', 'VERIFY.update', 11),
    ('audit.material', '删除', 'material:delete', 'VERIFY.delete', 12),
    ('audit.material', '执行全部核验', 'material:verify', 'VERIFY.update', 13),
    ('audit.supplement', '完整性执行检查', 'preaudit:completeness', 'PREAUDIT.update', 10),
    ('audit.supplement', '有效性执行检查', 'preaudit:validity', 'PREAUDIT.update', 11),
    ('audit.supplement', '一致性执行检查', 'preaudit:consistency', 'PREAUDIT.update', 12),
    ('audit.verify', '生成报告', 'report:generate', 'VERIFY.update', 10),
    ('audit.verify', '导出 PDF', 'report:export', 'VERIFY.view', 11),
    ('audit.risk', '执行评分', 'risk:score', 'RISK.update', 10),

    ('rule.definition', '新建规则', 'rule:create', 'RULE.create', 10),
    ('rule.definition', '启用', 'rule:enable', 'RULE.update', 11),
    ('rule.definition', '禁用', 'rule:disable', 'RULE.update', 12),
    ('rule.definition', '通过', 'rule:approve', 'RULE.approve', 13),
    ('rule.definition', '驳回', 'rule:reject', 'RULE.approve', 14),
    ('rule.weight', '新建权重', 'weight:create', 'RULE.create', 10),
    ('rule.weight', '通过', 'weight:approve', 'RULE.approve', 11),
    ('rule.weight', '驳回', 'weight:reject', 'RULE.approve', 12),
    ('rule.template', '新建模板', 'template:create', 'RULE.create', 10),
    ('rule.template', '修改', 'template:update', 'RULE.update', 11),
    ('rule.template', '删除', 'template:delete', 'RULE.delete', 12),
    ('rule.ocr-template', '新建模板', 'ocr-template:create', 'RULE.create', 10),
    ('rule.ocr-template', '修改', 'ocr-template:update', 'RULE.update', 11),
    ('rule.ocr-template', '删除', 'ocr-template:delete', 'RULE.delete', 12),

    ('audit-trail', '导出 Excel', 'audit:export', 'AUDIT.export', 10),
    ('system.user', '新建用户', 'system:user:create', 'USER.create', 10),
    ('system.user', '编辑', 'system:user:edit', 'USER.update', 11),
    ('system.user', '启用', 'system:user:enable', 'USER.update', 12),
    ('system.user', '禁用', 'system:user:disable', 'USER.update', 13),
    ('system.user', '删除', 'system:user:delete', 'USER.delete', 14),
    ('system.role', '新建角色', 'system:role:create', 'USER.create', 10),
    ('system.role', '配置菜单权限', 'system:role:configure', 'USER.update', 11),
    ('system.role', '删除', 'system:role:delete', 'USER.delete', 12),
    ('system.menu', '新增根菜单', 'system:menu:add-root', 'USER.create', 10),
    ('system.menu', '新增子菜单', 'system:menu:add-child', 'USER.create', 11),
    ('system.menu', '编辑', 'system:menu:edit', 'USER.update', 12),
    ('system.menu', '删除', 'system:menu:delete', 'USER.delete', 13)
) AS button(parent_code, menu_name, menu_code, permission, sort)
    ON parent.menu_code = button.parent_code
ON CONFLICT (menu_code) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    permission = EXCLUDED.permission,
    sort = EXCLUDED.sort,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 按现有 API 权限初始化按钮，升级后不改变用户已有操作能力。
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_role_permission permission ON permission.role_id = role.id
JOIN schema_common.sys_menu menu ON menu.menu_type = 'BUTTON'
WHERE permission.module = split_part(menu.permission, '.', 1)
  AND permission.permissions ? split_part(menu.permission, '.', 2)
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM schema_common.sys_role role
JOIN schema_common.sys_menu menu ON menu.menu_type = 'BUTTON'
WHERE role.role_code = 'ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;
