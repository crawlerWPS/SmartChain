-- ============================================================
-- SCFS V2__init_data.sql - 初始化种子数据
-- 对应 RFC 6.3 阶段 1 S1-3
-- ============================================================

SET search_path TO schema_common;

-- 默认管理员密码：admin123（BCrypt 哈希）
INSERT INTO schema_common.sys_user (username, password_hash, real_name, role_code, email, phone, status)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ADMIN', 'admin@scfs.com', '13800000000', 1)
ON CONFLICT (username) DO NOTHING;

-- 表 2：7 个角色（对应 RFC 1.2 用户角色定义）
INSERT INTO schema_common.sys_role (role_code, role_name, role_type, description, status) VALUES
('RM', '客户经理', 'BUSINESS', 'R-01 负责融资申请提交', 1),
('RCO', '风控审核员', 'RISK_CONTROL', 'R-02 负责人工审核决策', 1),
('OPS_MAKER', '规则经办岗', 'CONFIG_MAKER', 'R-03a 规则/权重/模板经办提交', 1),
('OPS_CHECKER', '规则复核岗', 'CONFIG_CHECKER', 'R-03b 规则/权重/模板复核', 1),
('OPS', '运营主管', 'OPS', 'R-03c 运营监控', 1),
('AUDIT', '审计人员', 'AUDIT', 'R-04 审计日志查询', 1),
('ADMIN', '系统管理员', 'SYSTEM', 'R-05 用户/角色/菜单管理', 1)
ON CONFLICT (role_code) DO NOTHING;

-- 表 3：默认角色 API 权限
-- ADMIN 全部权限
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('GRAPH', '["view","create","update","delete","export"]'),
  ('VERIFY', '["view","create","update","delete","export","approve","reject"]'),
  ('PREAUDIT', '["view","create","update","delete","export"]'),
  ('RISK', '["view","create","update","delete","export"]'),
  ('RULE', '["view","create","update","delete","export","approve","reject"]'),
  ('USER', '["view","create","update","delete"]'),
  ('AUDIT', '["view","export"]')
) AS m(module, perms)
WHERE r.role_code = 'ADMIN';

-- RM 客户经理：申请+材料创建
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('VERIFY', '["view","create","update"]'),
  ('GRAPH', '["view"]'),
  ('PREAUDIT', '["view"]'),
  ('RISK', '["view"]')
) AS m(module, perms)
WHERE r.role_code = 'RM';

-- RCO 风控审核员
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('VERIFY', '["view","approve","reject","export"]'),
  ('GRAPH', '["view"]'),
  ('PREAUDIT', '["view"]'),
  ('RISK', '["view"]')
) AS m(module, perms)
WHERE r.role_code = 'RCO';

-- OPS_MAKER / OPS_CHECKER 双岗
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('RULE', '["view","create","update"]')
) AS m(module, perms)
WHERE r.role_code = 'OPS_MAKER';

INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('RULE', '["view","approve","reject"]')
) AS m(module, perms)
WHERE r.role_code = 'OPS_CHECKER';

-- AUDIT 审计
INSERT INTO schema_common.sys_role_permission (role_id, module, permissions)
SELECT r.id, m.module, m.perms::jsonb
FROM schema_common.sys_role r
CROSS JOIN (VALUES
  ('AUDIT', '["view","export"]'),
  ('VERIFY', '["view"]'),
  ('GRAPH', '["view"]')
) AS m(module, perms)
WHERE r.role_code = 'AUDIT';

-- 表 3a：默认菜单树（对应 RFC 2.2 默认菜单树）
-- 工作台目录
INSERT INTO schema_common.sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status) VALUES
(1, 0, '工作台', 'workspace', 'DIRECTORY', '/workspace', NULL, 'DashboardOutlined', 1, 1, 1),
(2, 1, '我的待办', 'workspace.todo', 'MENU', '/workspace/todo', 'workspace/todo', 'BellOutlined', 1, 1, 1),
(3, 1, '运营监控', 'workspace.monitor', 'MENU', '/workspace/monitor', 'workspace/monitor', 'MonitorOutlined', 2, 1, 1),
-- 供应链图谱
(10, 0, '供应链图谱', 'graph', 'DIRECTORY', '/graph', NULL, 'ApartmentOutlined', 2, 1, 1),
(11, 10, '关系图谱', 'graph.relations', 'MENU', '/graph/relations', 'graph/relations', 'ShareAltOutlined', 1, 1, 1),
(12, 10, '企业角色', 'graph.role', 'MENU', '/graph/role', 'graph/role', 'TeamOutlined', 2, 1, 1),
(13, 10, '位置分析', 'graph.position', 'MENU', '/graph/position', 'graph/position', 'EnvironmentOutlined', 3, 1, 1),
(14, 10, '异常关系', 'graph.abnormal', 'MENU', '/graph/abnormal', 'graph/abnormal', 'WarningOutlined', 4, 1, 1),
-- 审核中心
(20, 0, '审核中心', 'audit', 'DIRECTORY', '/audit', NULL, 'AuditOutlined', 3, 1, 1),
(21, 20, '融资申请', 'audit.application', 'MENU', '/audit/application', 'audit/application', 'FileTextOutlined', 1, 1, 1),
(22, 20, '材料管理', 'audit.material', 'MENU', '/audit/material', 'audit/material', 'FolderOutlined', 2, 1, 1),
(23, 20, '核验报告', 'audit.verify', 'MENU', '/audit/verify', 'audit/verify', 'SafetyOutlined', 3, 1, 1),
(24, 20, '补正清单', 'audit.supplement', 'MENU', '/audit/supplement', 'audit/supplement', 'CheckSquareOutlined', 4, 1, 1),
(25, 20, '风险画像', 'audit.risk', 'MENU', '/audit/risk', 'audit/risk', 'RadarChartOutlined', 5, 1, 1),
-- 规则配置
(30, 0, '规则配置', 'rule', 'DIRECTORY', '/rule', NULL, 'SettingOutlined', 4, 1, 1),
(31, 30, '规则定义', 'rule.definition', 'MENU', '/rule/definition', 'rule/definition', 'CodeOutlined', 1, 1, 1),
(32, 31, '创建规则', 'rule:create', 'BUTTON', NULL, NULL, NULL, 1, 1, 1),
(33, 31, '审批规则', 'rule:approve', 'BUTTON', NULL, NULL, NULL, 2, 1, 1),
(34, 30, '风险权重', 'rule.weight', 'MENU', '/rule/weight', 'rule/weight', 'PercentageOutlined', 2, 1, 1),
(35, 34, '创建权重', 'weight:create', 'BUTTON', NULL, NULL, NULL, 1, 1, 1),
(36, 34, '审批权重', 'weight:approve', 'BUTTON', NULL, NULL, NULL, 2, 1, 1),
(37, 30, '材料模板', 'rule.template', 'MENU', '/rule/template', 'rule/template', 'ProfileOutlined', 3, 1, 1),
(38, 37, '创建模板', 'template:create', 'BUTTON', NULL, NULL, NULL, 1, 1, 1),
(39, 37, '审批模板', 'template:approve', 'BUTTON', NULL, NULL, NULL, 2, 1, 1),
-- 审计查询
(40, 0, '审计查询', 'audit-trail', 'DIRECTORY', '/audit-trail', NULL, 'HistoryOutlined', 5, 1, 1),
(41, 40, '操作日志', 'audit-trail.log', 'MENU', '/audit-trail/log', 'audit-trail/log', 'FileSearchOutlined', 1, 1, 1),
(42, 40, '流程追溯', 'audit-trail.trace', 'MENU', '/audit-trail/trace', 'audit-trail/trace', 'BranchesOutlined', 2, 1, 1),
-- 系统管理
(50, 0, '系统管理', 'system', 'DIRECTORY', '/system', NULL, 'ToolOutlined', 6, 1, 1),
(51, 50, '用户管理', 'system.user', 'MENU', '/system/user', 'system/user', 'UserOutlined', 1, 1, 1),
(52, 50, '角色管理', 'system.role', 'MENU', '/system/role', 'system/role', 'SafetyOutlined', 2, 1, 1),
(53, 50, '菜单管理', 'system.menu', 'MENU', '/system/menu', 'system/menu', 'MenuOutlined', 3, 1, 1),
(54, 50, '数据源', 'system.datasource', 'MENU', '/system/datasource', 'system/datasource', 'DatabaseOutlined', 4, 1, 1)
ON CONFLICT DO NOTHING;

-- 重置序列避免主键冲突
SELECT setval('schema_common.sys_menu_id_seq', (SELECT MAX(id) FROM schema_common.sys_menu));

-- 表 3b：ADMIN 角色拥有全部菜单
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM schema_common.sys_role r, schema_common.sys_menu m
WHERE r.role_code = 'ADMIN' ON CONFLICT DO NOTHING;

-- RM 角色菜单（工作台+审核中心查看）
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM schema_common.sys_role r, schema_common.sys_menu m
WHERE r.role_code = 'RM' AND m.menu_code IN ('workspace', 'workspace.todo', 'workspace.monitor',
  'graph', 'graph.relations', 'graph.role', 'graph.position', 'graph.abnormal',
  'audit', 'audit.application', 'audit.material', 'audit.verify', 'audit.supplement', 'audit.risk')
ON CONFLICT DO NOTHING;

-- RCO 角色菜单
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM schema_common.sys_role r, schema_common.sys_menu m
WHERE r.role_code = 'RCO' AND m.menu_code IN ('workspace', 'workspace.todo',
  'graph', 'graph.relations', 'graph.role', 'graph.position', 'graph.abnormal',
  'audit', 'audit.application', 'audit.material', 'audit.verify', 'audit.supplement', 'audit.risk')
ON CONFLICT DO NOTHING;

-- OPS_MAKER / OPS_CHECKER 角色菜单（规则配置）
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM schema_common.sys_role r, schema_common.sys_menu m
WHERE r.role_code IN ('OPS_MAKER', 'OPS_CHECKER') AND m.menu_code IN (
  'rule', 'rule.definition', 'rule:create', 'rule:approve',
  'rule.weight', 'weight:create', 'weight:approve',
  'rule.template', 'template:create', 'template:approve')
ON CONFLICT DO NOTHING;

-- AUDIT 角色菜单
INSERT INTO schema_common.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM schema_common.sys_role r, schema_common.sys_menu m
WHERE r.role_code = 'AUDIT' AND m.menu_code IN ('audit-trail', 'audit-trail.log', 'audit-trail.trace')
ON CONFLICT DO NOTHING;

-- 表 6：初始规则集（Drools DRL）
INSERT INTO schema_common.rule_definition (rule_code, rule_name, category, drl_content, params, status, version, created_by) VALUES
('R_AMOUNT_DIFF', '金额差异校验', 'VERIFY',
 'package com.scfs.rules.verify;\nrule "R_AMOUNT_DIFF"\nwhen\n  $app: FinancingApplication();\n  $contract: MaterialRecognitionResult(materialType == "CONTRACT")\n  $invoice: MaterialRecognitionResult(materialType == "INVOICE")\n  eval(Math.abs($contract.amount.doubleValue() - $invoice.amount.doubleValue()) / $contract.amount.doubleValue() > 0.01)\nthen\n  insert(new RuleViolation("AMOUNT_DIFF", "合同金额与发票金额差异超过1%"));\nend',
 '{"tolerance": 0.01}'::jsonb, 1, 1, 1),
('R_AMOUNT_MATCH', '发票验收金额匹配', 'VERIFY',
 'package com.scfs.rules.verify;\nrule "R_AMOUNT_MATCH"\nwhen\n  $invoice: MaterialRecognitionResult(materialType == "INVOICE")\n  $acceptance: MaterialRecognitionResult(materialType == "ACCEPTANCE")\n  eval($invoice.amount.compareTo($acceptance.amount) != 0)\nthen\n  insert(new RuleViolation("AMOUNT_MISMATCH", "发票金额与验收金额不一致"));\nend',
 '{}'::jsonb, 1, 1, 1),
('R_TIME_ORDER', '时间逻辑校验', 'VERIFY',
 'package com.scfs.rules.verify;\nrule "R_TIME_ORDER"\nwhen\n  $contract: MaterialRecognitionResult(materialType == "CONTRACT")\n  $invoice: MaterialRecognitionResult(materialType == "INVOICE")\n  eval($invoice.invoiceDate != null && $contract.contractDate != null && $invoice.invoiceDate.before($contract.contractDate))\nthen\n  insert(new RuleViolation("TIME_INVOICE_BEFORE_CONTRACT", "发票日期早于合同日期"));\nend',
 '{}'::jsonb, 1, 1, 1),
('R_REPEAT_FINANCING', '重复融资检查', 'VERIFY',
 'package com.scfs.rules.verify;\nrule "R_REPEAT_FINANCING"\nwhen\n  $app: FinancingApplication(businessType == "AR_FINANCING")\n  exists FinancingApplication(enterpriseId == $app.enterpriseId, businessType == "AR_FINANCING", status == "APPROVED")\nthen\n  insert(new RuleViolation("REPEAT_FINANCING", "该企业已存在已审批通过的应收账款融资"));\nend',
 '{}'::jsonb, 1, 1, 1)
ON CONFLICT (rule_code) DO NOTHING;

-- 表 8：默认权重配置 40/30/30 阈值 85/70/50
-- 跳过双岗校验，直接启用（系统初始数据）
INSERT INTO schema_common.risk_weight_config (config_name, supply_chain_weight, transaction_weight, material_weight,
  low_risk_threshold, mid_risk_threshold, high_risk_threshold, status, version, maker_id)
VALUES ('default-40-30-30', 40, 30, 30, 85, 70, 50, 'ENABLED', 1, 1)
ON CONFLICT DO NOTHING;

-- 表 20：3 种业务类型默认材料清单模板
INSERT INTO schema_common.material_checklist_template (business_type, required_materials, version, status, maker_id)
VALUES
('AR_FINANCING', '["CONTRACT","INVOICE","LOGISTICS","ACCEPTANCE"]'::jsonb, 1, 'ENABLED', 1),
('FACTORING', '["CONTRACT","INVOICE","LOGISTICS","ACCEPTANCE","PAYMENT"]'::jsonb, 1, 'ENABLED', 1),
('ORDER_FINANCING', '["ORDER","CONTRACT","INVOICE","LOGISTICS","ACCEPTANCE"]'::jsonb, 1, 'ENABLED', 1)
ON CONFLICT (business_type) DO NOTHING;

-- ========== schema_graph 测试企业 ==========
INSERT INTO schema_graph.enterprise (name, uscc, industry, legal_person, registered_capital, address, data_source)
VALUES
('上海晨星科技有限公司', '91310000MA1FL3ABCD', '科技', '张明', 5000.00, '上海市浦东新区张江高科技园区', 'MOCK'),
('深圳蓝海制造集团', '91440300MA5FGHIJK', '制造', '李华', 10000.00, '深圳市南山区科技园', 'MOCK')
ON CONFLICT (uscc) DO NOTHING;
