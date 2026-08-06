-- ============================================================
-- SCFS V3__seed_data.sql - 业务种子数据
-- 覆盖 schema_common / schema_graph / schema_verify / schema_preaudit / schema_risk
-- 关键字段对齐 V1__init_schema.sql 与各 enum
-- ============================================================

SET search_path TO schema_common;

-- ============================================================
-- 0. 清理旧种子数据（保证脚本幂等，可重复执行）
-- ============================================================
DELETE FROM schema_common.sys_user WHERE username IN ('rm_zhang','rco_li','maker_wang','checker_zhao','ops_sun','audit_zhou');
DELETE FROM schema_common.file_object WHERE id BETWEEN 101 AND 305;
DELETE FROM schema_common.rule_definition WHERE rule_code IN ('R_SUBJECT_MATCH','R_MATERIAL_COMPLETE');
DELETE FROM schema_common.rule_change_log WHERE rule_code IN ('R_AMOUNT_DIFF','R_SUBJECT_MATCH') AND maker_id = 4;
DELETE FROM schema_common.risk_weight_config WHERE config_name = 'pending-50-30-20';
DELETE FROM schema_common.material_checklist_template WHERE business_type='FACTORING' AND version=2;
DELETE FROM schema_common.sys_audit_log WHERE created_at >= '2026-07-15 09:00:00' AND created_at <= '2026-07-25 12:00:00';
DELETE FROM schema_graph.enterprise WHERE id IN (3,4,5,6);
DELETE FROM schema_graph.supply_chain_relation WHERE core_enterprise_id = 3;
DELETE FROM schema_graph.enterprise_role WHERE core_enterprise_id = 3;
DELETE FROM schema_graph.enterprise_position_analysis WHERE enterprise_id IN (1,3,4,5,6);
DELETE FROM schema_graph.abnormal_relation WHERE enterprise_id IN (1,4,5);
DELETE FROM schema_verify.financing_application WHERE id IN (1,2,3);
DELETE FROM schema_verify.application_status_history WHERE application_id IN (1,2,3);
DELETE FROM schema_verify.application_material WHERE id IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14);
DELETE FROM schema_verify.material_recognition_result WHERE application_material_id IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14);
DELETE FROM schema_verify.verify_check_result WHERE application_id IN (1,2,3);
DELETE FROM schema_verify.verify_report WHERE report_no IN ('RPT2026070001','RPT2026070002');
DELETE FROM schema_preaudit.material_completeness_result WHERE application_id IN (1,2,3);
DELETE FROM schema_preaudit.material_validity_result WHERE application_id IN (1,2,3);
DELETE FROM schema_preaudit.enterprise_info_mismatch_detail WHERE result_id IN (SELECT id FROM schema_preaudit.enterprise_info_consistency_result WHERE application_id IN (1,2,3));
DELETE FROM schema_preaudit.enterprise_info_consistency_result WHERE application_id IN (1,2,3);
DELETE FROM schema_preaudit.supplement_list WHERE application_id = 3;
DELETE FROM schema_risk.transaction_stability WHERE enterprise_id IN (1,3,4,5,6);
DELETE FROM schema_risk.risk_profile WHERE application_id IN (1,2);

-- ============================================================
-- 1. sys_user：补齐 7 个角色用户（密码统一 admin123 的 BCrypt 哈希）
-- ============================================================
INSERT INTO schema_common.sys_user (username, password_hash, real_name, role_code, email, phone, status)
VALUES
  ('rm_zhang',  '$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS', '张客户经理', 'RM', 'rm@scfs.com', '13800000001', 1),
  ('rco_li',    '$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS', '李风控员',   'RCO', 'rco@scfs.com', '13800000002', 1),
  ('maker_wang','$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS', '王经办',     'OPS_MAKER', 'maker@scfs.com', '13800000003', 1),
  ('checker_zhao','$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS','赵复核',     'OPS_CHECKER', 'checker@scfs.com', '13800000004', 1),
  ('ops_sun',   '$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS', '孙运营',     'OPS', 'ops@scfs.com', '13800000005', 1),
  ('audit_zhou','$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS', '周审计',     'AUDIT', 'audit@scfs.com', '13800000006', 1)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- 2. file_object：先插入文件元数据（后续材料表引用）
-- ============================================================
INSERT INTO schema_common.file_object (id, file_name, file_type, file_size, minio_bucket, minio_object_key, content_hash, uploaded_by)
VALUES
  (101, 'contract_001.pdf',  'pdf', 102400, 'scfs', 'material/contract_001.pdf',  'hash_contract_001', 2),
  (102, 'invoice_001.pdf',   'pdf',  81920, 'scfs', 'material/invoice_001.pdf',   'hash_invoice_001',  2),
  (103, 'logistics_001.pdf', 'pdf',  65536, 'scfs', 'material/logistics_001.pdf', 'hash_logistics_001',2),
  (104, 'acceptance_001.pdf','pdf',  49152, 'scfs', 'material/acceptance_001.pdf','hash_acceptance_001',2),
  (201, 'contract_002.pdf',  'pdf', 120000, 'scfs', 'material/contract_002.pdf',  'hash_contract_002', 2),
  (202, 'invoice_002.pdf',   'pdf',  90000, 'scfs', 'material/invoice_002.pdf',   'hash_invoice_002',  2),
  (203, 'logistics_002.pdf', 'pdf',  70000, 'scfs', 'material/logistics_002.pdf', 'hash_logistics_002',2),
  (204, 'acceptance_002.pdf','pdf',  55000, 'scfs', 'material/acceptance_002.pdf','hash_acceptance_002',2),
  (205, 'payment_002.pdf',   'pdf',  30000, 'scfs', 'material/payment_002.pdf',   'hash_payment_002',  2),
  (301, 'contract_003.pdf',  'pdf',  80000, 'scfs', 'material/contract_003.pdf',  'hash_contract_003', 2),
  (302, 'invoice_003.pdf',   'pdf',  60000, 'scfs', 'material/invoice_003.pdf',   'hash_invoice_003',  2),
  (303, 'order_003.pdf',     'pdf',  50000, 'scfs', 'material/order_003.pdf',     'hash_order_003',    2),
  (304, 'logistics_003.pdf', 'pdf',  45000, 'scfs', 'material/logistics_003.pdf', 'hash_logistics_003',2),
  (305, 'acceptance_003.pdf','pdf',  40000, 'scfs', 'material/acceptance_003.pdf','hash_acceptance_003',2)
ON CONFLICT (id) DO NOTHING;

SELECT setval('schema_common.file_object_id_seq', GREATEST((SELECT MAX(id) FROM schema_common.file_object), 1));

-- ============================================================
-- 3. rule_definition：补 2 条核验规则 + 1 条预审规则
-- ============================================================
INSERT INTO schema_common.rule_definition (rule_code, rule_name, category, drl_content, params, status, version, created_by) VALUES
  ('R_SUBJECT_MATCH', '主体一致性核验', 'VERIFY',
   'package com.scfs.rules.verify;\nrule "R_SUBJECT_MATCH"\nwhen\n  $app: FinancingApplication();\n  $r: MaterialRecognitionResult(buyerUscc != null, sellerUscc != null)\n  eval(!$app.enterprise.uscc.equals($r.buyerUscc) && !$app.enterprise.uscc.equals($r.sellerUscc))\nthen\n  insert(new RuleViolation("SUBJECT_MISMATCH", "材料主体与申请企业不一致"));\nend',
   '{}'::jsonb, 1, 1, 1),
  ('R_MATERIAL_COMPLETE', '材料完整性预审', 'PREAUDIT',
   'package com.scfs.rules.preaudit;\nrule "R_MATERIAL_COMPLETE"\nwhen\n  $app: FinancingApplication();\n  $tpl: MaterialChecklistTemplate(businessType == $app.businessType);\n  eval(countMissingMaterials($app, $tpl) > 0)\nthen\n  insert(new PreAuditWarning("MATERIAL_MISSING", "缺少必要材料"));\nend',
   '{}'::jsonb, 1, 1, 1)
ON CONFLICT (rule_code) DO NOTHING;

-- ============================================================
-- 4. rule_change_log：双岗审批记录（1 条已通过、1 条待复核）
-- ============================================================
INSERT INTO schema_common.rule_change_log (rule_id, rule_code, change_type, old_version, new_version, old_content, new_content, status, maker_id, checker_id, checked_at, reject_reason)
VALUES
  ((SELECT id FROM schema_common.rule_definition WHERE rule_code='R_AMOUNT_DIFF'), 'R_AMOUNT_DIFF', 'UPDATE', 1, 2,
   'old drl content', 'new drl content with stricter tolerance', 'APPROVED', 4, 5, NOW(), NULL),
  ((SELECT id FROM schema_common.rule_definition WHERE rule_code='R_SUBJECT_MATCH'), 'R_SUBJECT_MATCH', 'CREATE', NULL, 1,
   NULL, 'initial drl content', 'PENDING', 4, NULL, NULL, NULL);

-- ============================================================
-- 5. risk_weight_config：补一条待复核配置
-- ============================================================
INSERT INTO schema_common.risk_weight_config (config_name, supply_chain_weight, transaction_weight, material_weight,
  low_risk_threshold, mid_risk_threshold, high_risk_threshold, status, version, maker_id)
VALUES ('pending-50-30-20', 50, 30, 20, 85, 70, 50, 'PENDING', 1, 4)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. material_checklist_template：补一条待复核
-- ============================================================
INSERT INTO schema_common.material_checklist_template (business_type, required_materials, version, status, maker_id)
VALUES ('FACTORING', '["CONTRACT","INVOICE","LOGISTICS","ACCEPTANCE","PAYMENT","QUALIFICATION"]'::jsonb, 2, 'PENDING', 4)
ON CONFLICT (business_type) DO NOTHING;

-- ============================================================
-- 7. sys_audit_log：覆盖当前月分区（202607）
-- ============================================================
INSERT INTO schema_common.sys_audit_log (user_id, username, module, action, target_type, target_id, detail, ip_address, created_at)
VALUES
  (1, 'admin',      'USER',  'LOGIN',     NULL,     NULL,    '{"client":"web"}'::jsonb,                  '127.0.0.1', '2026-07-15 09:10:00'),
  (2, 'rm_zhang',   'VERIFY','CREATE',    'APPLICATION', 'APP2026070001', '{"appNo":"APP2026070001"}'::jsonb, '127.0.0.1','2026-07-15 09:30:00'),
  (3, 'rco_li',     'VERIFY','APPROVE',   'APPLICATION', 'APP2026070001', '{"decision":"APPROVED"}'::jsonb,    '127.0.0.1','2026-07-18 14:00:00'),
  (4, 'maker_wang', 'RULE',  'CREATE',    'RULE',        'R_SUBJECT_MATCH','{"version":1}'::jsonb,         '127.0.0.1','2026-07-20 10:00:00'),
  (5, 'checker_zhao','RULE', 'APPROVE',   'RULE',        'R_AMOUNT_DIFF', '{"from":1,"to":2}'::jsonb,         '127.0.0.1','2026-07-20 16:00:00'),
  (6, 'audit_zhou', 'AUDIT', 'EXPORT',    'AUDIT_LOG',   NULL,            '{"filter":"2026-07"}'::jsonb,      '127.0.0.1','2026-07-25 11:00:00');

-- ============================================================
-- 8. schema_graph.enterprise：补 4 家企业（核心 + 一级 + 二级 + 普通）
-- ============================================================
INSERT INTO schema_graph.enterprise (id, name, uscc, industry, legal_person, registered_capital, establish_date, address, data_source, last_synced_at)
VALUES
  (3, '北京中科智造集团',     '91110000MA0ABCDE12', '制造', '陈强',   50000.00, '2010-03-15', '北京市海淀区中关村软件园',     'MOCK', NOW()),
  (4, '广州锐捷电子有限公司', '91440100MA0XYZAB34', '电子', '林芳',   3000.00,  '2015-07-20', '广州市天河区珠江新城',           'MOCK', NOW()),
  (5, '杭州诚信贸易公司',     '91330100MA0EFGHA56', '贸易', '吴明',   1000.00,  '2018-11-10', '杭州市西湖区文三路',             'MOCK', NOW()),
  (6, '苏州顺达物流公司',     '91320500MA0IJKLM78', '物流', '郑华',   800.00,   '2020-04-08', '苏州市工业园区独墅湖大道',       'MOCK', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval('schema_graph.enterprise_id_seq', GREATEST((SELECT MAX(id) FROM schema_graph.enterprise), 1));

-- ============================================================
-- 9. supply_chain_relation：以企业 3 为核心，向下延伸
-- ============================================================
INSERT INTO schema_graph.supply_chain_relation (from_enterprise_id, to_enterprise_id, relation_type, first_coop_date, last_coop_date, total_transactions, total_amount, core_enterprise_id, level)
VALUES
  (4, 3, 'SUPPLY',  '2021-01-01', '2026-06-30', 120, 24000000.00, 3, 1),
  (5, 3, 'SUPPLY',  '2022-05-15', '2026-06-20', 80,  9600000.00,  3, 1),
  (5, 4, 'SUPPLY',  '2022-08-01', '2026-05-10', 40,  3200000.00,  3, 2),
  (6, 3, 'LOGISTICS','2022-01-01','2026-06-30', 200, 4800000.00,  3, 1),
  (1, 3, 'SUPPLY',  '2023-03-01', '2026-04-15', 30,  1500000.00,  3, 2);

-- ============================================================
-- 10. enterprise_role：各企业在以 3 为核心的链中的角色
-- ============================================================
INSERT INTO schema_graph.enterprise_role (enterprise_id, role, core_enterprise_id, coop_duration_years, coop_enterprise_count, influence_level, credibility_level)
VALUES
  (3, 'CORE',         3, NULL, 4, 'HIGH', 'HIGH'),
  (4, 'KEY_SUPPLIER', 3, 5.5, 2, 'HIGH', 'HIGH'),
  (5, 'TIER1',        3, 4.1, 2, 'MID',  'MID'),
  (6, 'NORMAL',       3, 4.5, 1, 'LOW',  'MID'),
  (1, 'TIER2',        3, 3.3, 1, 'LOW',  'LOW');

-- ============================================================
-- 11. enterprise_position_analysis：位置分析
-- ============================================================
INSERT INTO schema_graph.enterprise_position_analysis (enterprise_id, in_core_chain, distance_to_core, upstream_stable, downstream_stable, credibility, credibility_reason)
VALUES
  (3, TRUE,  0, TRUE,  TRUE,  'HIGH', '核心企业，信用资质强'),
  (4, TRUE,  1, TRUE,  TRUE,  'HIGH', '一级关键供应商，合作稳定'),
  (5, TRUE,  1, TRUE,  FALSE, 'MID',  '一级供应商，下游波动'),
  (6, TRUE,  1, FALSE, TRUE,  'MID',  '物流服务商，上游承压'),
  (1, FALSE, 2, TRUE,  FALSE, 'LOW',  '二级供应商，位置较远');

-- ============================================================
-- 12. abnormal_relation：3 类异常各一条
-- ============================================================
INSERT INTO schema_graph.abnormal_relation (enterprise_id, abnormal_type, severity, description, evidence, status, detected_at)
VALUES
  (5, 'RAPID_EXPANSION','HIGH',   '近 6 个月新增 5 家关联企业，扩张速度异常',
   '{"newCompanies":5,"periodMonths":6}'::jsonb, 'OPEN', '2026-07-05 10:00:00'),
  (1, 'CIRCULAR',       'MID',    '与企业 5 形成循环贸易链',
   '{"cycle":[1,5,3,1]}'::jsonb, 'OPEN', '2026-07-08 11:00:00'),
  (4, 'RELATED_PARTY',  'LOW',    '与企业 3 法人存在亲属关系',
   '{"legalPersonRelation":"SIBLING"}'::jsonb, 'CONFIRMED', '2026-07-10 15:00:00');

-- ============================================================
-- 13. financing_application：3 个不同业务类型 + 不同状态
-- ============================================================
INSERT INTO schema_verify.financing_application (id, app_no, enterprise_id, business_type, financing_amount, submitted_by, status, current_handler, submitted_at, approved_at, version)
VALUES
  (1, 'APP2026070001', 4, 'AR_FINANCING',    5000000.00, 2, 'APPROVED',        NULL, '2026-07-15 09:30:00', '2026-07-18 14:00:00', 3),
  (2, 'APP2026070002', 5, 'FACTORING',       2000000.00, 2, 'PENDING_DECISION',3,    '2026-07-20 10:00:00', NULL,                  2),
  (3, 'APP2026070003', 6, 'ORDER_FINANCING', 1000000.00, 2, 'VERIFYING',       3,    '2026-07-22 11:00:00', NULL,                  1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('schema_verify.financing_application_id_seq', GREATEST((SELECT MAX(id) FROM schema_verify.financing_application), 1));

-- ============================================================
-- 14. application_status_history：流转记录
-- ============================================================
INSERT INTO schema_verify.application_status_history (application_id, from_status, to_status, operator_id, remark, created_at)
VALUES
  (1, NULL,             'DRAFT',            2, '创建草稿',          '2026-07-15 09:25:00'),
  (1, 'DRAFT',          'SUBMITTED',        2, '提交申请',          '2026-07-15 09:30:00'),
  (1, 'SUBMITTED',      'PRE_AUDITING',     1, '系统自动分派',      '2026-07-15 09:31:00'),
  (1, 'PRE_AUDITING',   'PRE_AUDIT_PASSED', 1, '预审通过',          '2026-07-16 10:00:00'),
  (1, 'PRE_AUDIT_PASSED','VERIFYING',       1, '进入核验',          '2026-07-16 10:05:00'),
  (1, 'VERIFYING',      'VERIFY_PASSED',    3, '核验通过',          '2026-07-17 15:00:00'),
  (1, 'VERIFY_PASSED',  'RISK_SCORING',     1, '进入风险评分',      '2026-07-17 15:05:00'),
  (1, 'RISK_SCORING',   'PENDING_DECISION', 1, '评分完成待决策',    '2026-07-17 17:00:00'),
  (1, 'PENDING_DECISION','APPROVED',        3, '审核通过',          '2026-07-18 14:00:00'),
  (2, NULL,             'DRAFT',            2, '创建草稿',          '2026-07-20 09:50:00'),
  (2, 'DRAFT',          'SUBMITTED',        2, '提交申请',          '2026-07-20 10:00:00'),
  (2, 'SUBMITTED',      'PRE_AUDITING',     1, '系统自动分派',      '2026-07-20 10:01:00'),
  (2, 'PRE_AUDITING',   'PRE_AUDIT_PASSED', 1, '预审通过',          '2026-07-21 10:00:00'),
  (2, 'PRE_AUDIT_PASSED','VERIFYING',       1, '进入核验',          '2026-07-21 10:05:00'),
  (2, 'VERIFYING',      'VERIFY_PASSED',    3, '核验通过',          '2026-07-22 14:00:00'),
  (2, 'VERIFY_PASSED',  'RISK_SCORING',     1, '进入风险评分',      '2026-07-22 14:05:00'),
  (2, 'RISK_SCORING',   'PENDING_DECISION', 1, '评分完成待决策',    '2026-07-22 16:00:00'),
  (3, NULL,             'DRAFT',            2, '创建草稿',          '2026-07-22 10:55:00'),
  (3, 'DRAFT',          'SUBMITTED',        2, '提交申请',          '2026-07-22 11:00:00'),
  (3, 'SUBMITTED',      'PRE_AUDITING',     1, '系统自动分派',      '2026-07-22 11:01:00'),
  (3, 'PRE_AUDITING',   'PRE_AUDIT_PASSED', 1, '预审通过',          '2026-07-23 10:00:00'),
  (3, 'PRE_AUDIT_PASSED','VERIFYING',       1, '进入核验',          '2026-07-23 10:05:00');

-- ============================================================
-- 15. application_material：3 个申请的材料
-- ============================================================
INSERT INTO schema_verify.application_material (id, application_id, file_object_id, material_type, identified_by, confidence, status)
VALUES
  (1, 1, 101, 'CONTRACT',   'AUTO', 0.95, 'IDENTIFIED'),
  (2, 1, 102, 'INVOICE',    'AUTO', 0.93, 'IDENTIFIED'),
  (3, 1, 103, 'LOGISTICS',  'AUTO', 0.91, 'IDENTIFIED'),
  (4, 1, 104, 'ACCEPTANCE', 'AUTO', 0.92, 'IDENTIFIED'),
  (5, 2, 201, 'CONTRACT',   'AUTO', 0.96, 'IDENTIFIED'),
  (6, 2, 202, 'INVOICE',    'AUTO', 0.94, 'IDENTIFIED'),
  (7, 2, 203, 'LOGISTICS',  'AUTO', 0.90, 'IDENTIFIED'),
  (8, 2, 204, 'ACCEPTANCE', 'AUTO', 0.93, 'IDENTIFIED'),
  (9, 2, 205, 'PAYMENT',    'AUTO', 0.88, 'IDENTIFIED'),
  (10,3, 301, 'CONTRACT',   'AUTO', 0.92, 'IDENTIFIED'),
  (11,3, 302, 'INVOICE',    'AUTO', 0.91, 'IDENTIFIED'),
  (12,3, 303, 'ORDER',      'AUTO', 0.95, 'IDENTIFIED'),
  (13,3, 304, 'LOGISTICS',  'AUTO', 0.89, 'IDENTIFIED'),
  (14,3, 305, 'ACCEPTANCE', 'AUTO', 0.90, 'IDENTIFIED')
ON CONFLICT (id) DO NOTHING;

SELECT setval('schema_verify.application_material_id_seq', GREATEST((SELECT MAX(id) FROM schema_verify.application_material), 1));

-- ============================================================
-- 16. material_recognition_result：对应材料的 OCR 结构化结果
-- ============================================================
INSERT INTO schema_verify.material_recognition_result (application_material_id, buyer_name, buyer_uscc, seller_name, seller_uscc, commodity, amount, amount_in_words, contract_date, order_date, invoice_date, logistics_date, acceptance_date, payment_date, contract_period, payment_term, transaction_no, field_confidence, raw_ocr_result, field_positions)
VALUES
  (1,  '北京中科智造集团','91110000MA0ABCDE12','广州锐捷电子有限公司','91440100MA0XYZAB34','电子元件',5000000.00,'人民币伍佰万元整','2026-05-01',NULL,'2026-05-10','2026-05-15','2026-05-20',NULL,'2026-05-01至2026-08-01','货到30天','CONTRACT-001',
      '{"buyer_name":0.96,"amount":0.98}'::jsonb, '{"rawText":"合同示例"}'::jsonb, '{}'::jsonb),
  (2,  '北京中科智造集团','91110000MA0ABCDE12','广州锐捷电子有限公司','91440100MA0XYZAB34','电子元件',5000000.00,'人民币伍佰万元整',NULL,NULL,'2026-05-10',NULL,NULL,NULL,NULL,'货到30天','INV-001',
      '{"amount":0.97}'::jsonb, '{"rawText":"发票示例"}'::jsonb, '{}'::jsonb),
  (3,  '苏州顺达物流公司','91320500MA0IJKLM78','北京中科智造集团','91110000MA0ABCDE12','电子元件',NULL,NULL,NULL,NULL,NULL,'2026-05-15',NULL,NULL,NULL,NULL,'LOG-001',
      '{}'::jsonb, '{"rawText":"物流单示例"}'::jsonb, '{}'::jsonb),
  (4,  '北京中科智造集团','91110000MA0ABCDE12','广州锐捷电子有限公司','91440100MA0XYZAB34','电子元件',5000000.00,'人民币伍佰万元整',NULL,NULL,NULL,NULL,'2026-05-20',NULL,NULL,NULL,'ACC-001',
      '{"amount":0.95}'::jsonb, '{"rawText":"验收单示例"}'::jsonb, '{}'::jsonb),
  (5,  '北京中科智造集团','91110000MA0ABCDE12','杭州诚信贸易公司','91330100MA0EFGHA56','原材料',2000000.00,'人民币贰佰万元整','2026-06-01',NULL,'2026-06-10','2026-06-15','2026-06-20',NULL,'2026-06-01至2026-09-01','货到45天','CONTRACT-002',
      '{"buyer_name":0.94,"amount":0.97}'::jsonb, '{"rawText":"合同示例2"}'::jsonb, '{}'::jsonb),
  (6,  '北京中科智造集团','91110000MA0ABCDE12','杭州诚信贸易公司','91330100MA0EFGHA56','原材料',2000000.00,'人民币贰佰万元整',NULL,NULL,'2026-06-10',NULL,NULL,NULL,NULL,'货到45天','INV-002',
      '{"amount":0.96}'::jsonb, '{"rawText":"发票示例2"}'::jsonb, '{}'::jsonb),
  (7,  '苏州顺达物流公司','91320500MA0IJKLM78','北京中科智造集团','91110000MA0ABCDE12','原材料',NULL,NULL,NULL,NULL,NULL,'2026-06-15',NULL,NULL,NULL,NULL,'LOG-002',
      '{}'::jsonb, '{"rawText":"物流单示例2"}'::jsonb, '{}'::jsonb),
  (8,  '北京中科智造集团','91110000MA0ABCDE12','杭州诚信贸易公司','91330100MA0EFGHA56','原材料',2000000.00,'人民币贰佰万元整',NULL,NULL,NULL,NULL,'2026-06-20',NULL,NULL,NULL,'ACC-002',
      '{"amount":0.94}'::jsonb, '{"rawText":"验收单示例2"}'::jsonb, '{}'::jsonb),
  (9,  '杭州诚信贸易公司','91330100MA0EFGHA56','北京中科智造集团','91110000MA0ABCDE12','原材料',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-06-25',NULL,'货到45天','PAY-002',
      '{}'::jsonb, '{"rawText":"付款凭证示例"}'::jsonb, '{}'::jsonb),
  (10, '北京中科智造集团','91110000MA0ABCDE12','苏州顺达物流公司','91320500MA0IJKLM78','物流服务',1000000.00,'人民币壹佰万元整','2026-06-15',NULL,'2026-06-25','2026-06-30','2026-07-05',NULL,'2026-06-15至2026-09-15','月结30天','CONTRACT-003',
      '{"buyer_name":0.93,"amount":0.96}'::jsonb, '{"rawText":"合同示例3"}'::jsonb, '{}'::jsonb),
  (11, '北京中科智造集团','91110000MA0ABCDE12','苏州顺达物流公司','91320500MA0IJKLM78','物流服务',1000000.00,'人民币壹佰万元整',NULL,NULL,'2026-06-25',NULL,NULL,NULL,NULL,'月结30天','INV-003',
      '{"amount":0.95}'::jsonb, '{"rawText":"发票示例3"}'::jsonb, '{}'::jsonb),
  (12, '北京中科智造集团','91110000MA0ABCDE12','苏州顺达物流公司','91320500MA0IJKLM78','物流服务',1000000.00,'人民币壹佰万元整',NULL,'2026-06-10',NULL,NULL,NULL,NULL,NULL,'月结30天','ORD-003',
      '{"amount":0.94}'::jsonb, '{"rawText":"订单示例3"}'::jsonb, '{}'::jsonb),
  (13, '苏州顺达物流公司','91320500MA0IJKLM78','北京中科智造集团','91110000MA0ABCDE12','物流服务',NULL,NULL,NULL,NULL,NULL,'2026-06-30',NULL,NULL,NULL,NULL,'LOG-003',
      '{}'::jsonb, '{"rawText":"物流单示例3"}'::jsonb, '{}'::jsonb),
  (14, '北京中科智造集团','91110000MA0ABCDE12','苏州顺达物流公司','91320500MA0IJKLM78','物流服务',1000000.00,'人民币壹佰万元整',NULL,NULL,NULL,NULL,'2026-07-05',NULL,NULL,NULL,'ACC-003',
      '{"amount":0.93}'::jsonb, '{"rawText":"验收单示例3"}'::jsonb, '{}'::jsonb)
ON CONFLICT (application_material_id) DO NOTHING;

-- ============================================================
-- 17. verify_check_result：申请 1/2 全部核验类型
-- ============================================================
INSERT INTO schema_verify.verify_check_result (application_id, check_type, result, details, executed_rules, executed_at)
VALUES
  (1, 'SUBJECT','PASS',  '{"buyerMatched":true,"sellerMatched":true}'::jsonb, '["R_SUBJECT_MATCH"]'::jsonb, '2026-07-17 14:50:00'),
  (1, 'AMOUNT', 'PASS',  '{"contractAmount":5000000,"invoiceAmount":5000000,"diff":0}'::jsonb, '["R_AMOUNT_DIFF","R_AMOUNT_MATCH"]'::jsonb, '2026-07-17 14:51:00'),
  (1, 'TIME',   'PASS',  '{"contractDate":"2026-05-01","invoiceDate":"2026-05-10"}'::jsonb, '["R_TIME_ORDER"]'::jsonb, '2026-07-17 14:52:00'),
  (1, 'REPEAT', 'PASS',  '{"existApproved":false}'::jsonb, '["R_REPEAT_FINANCING"]'::jsonb, '2026-07-17 14:53:00'),
  (2, 'SUBJECT','PASS',  '{"buyerMatched":true,"sellerMatched":true}'::jsonb, '["R_SUBJECT_MATCH"]'::jsonb, '2026-07-22 13:50:00'),
  (2, 'AMOUNT', 'PASS',  '{"contractAmount":2000000,"invoiceAmount":2000000,"diff":0}'::jsonb, '["R_AMOUNT_DIFF","R_AMOUNT_MATCH"]'::jsonb, '2026-07-22 13:51:00'),
  (2, 'TIME',   'PASS',  '{"contractDate":"2026-06-01","invoiceDate":"2026-06-10"}'::jsonb, '["R_TIME_ORDER"]'::jsonb, '2026-07-22 13:52:00'),
  (2, 'REPEAT', 'PASS',  '{"existApproved":false}'::jsonb, '["R_REPEAT_FINANCING"]'::jsonb, '2026-07-22 13:53:00');

-- ============================================================
-- 18. verify_report：申请 1/2 报告
-- ============================================================
INSERT INTO schema_verify.verify_report (report_no, application_id, version, overall_assessment, abnormal_count, risk_hints, content_snapshot, content_hash, generated_at)
VALUES
  ('RPT2026070001', 1, 1, 'PASS', 0,
   '[]'::jsonb,
   '{"applicationId":1,"results":["SUBJECT_PASS","AMOUNT_PASS","TIME_PASS","REPEAT_PASS"]}'::jsonb,
   'hash_report_001', '2026-07-17 15:00:00'),
  ('RPT2026070002', 2, 1, 'PASS', 0,
   '[]'::jsonb,
   '{"applicationId":2,"results":["SUBJECT_PASS","AMOUNT_PASS","TIME_PASS","REPEAT_PASS"]}'::jsonb,
   'hash_report_002', '2026-07-22 14:00:00')
ON CONFLICT (report_no) DO NOTHING;

-- ============================================================
-- 19. schema_preaudit.material_completeness_result
-- ============================================================
INSERT INTO schema_preaudit.material_completeness_result (application_id, required_count, submitted_count, completeness_pct, missing_materials, checked_at)
VALUES
  (1, 4, 4, 100.00, '[]'::jsonb,                                          '2026-07-16 09:55:00'),
  (2, 5, 5, 100.00, '[]'::jsonb,                                          '2026-07-21 09:55:00'),
  (3, 5, 5, 100.00, '[]'::jsonb,                                          '2026-07-23 09:55:00');

-- ============================================================
-- 20. material_validity_result
-- ============================================================
INSERT INTO schema_preaudit.material_validity_result (application_id, total_files, expired_count, incomplete_count, abnormal_count, details, checked_at)
VALUES
  (1, 4, 0, 0, 0, '{"allValid":true}'::jsonb,                '2026-07-16 09:56:00'),
  (2, 5, 0, 0, 0, '{"allValid":true}'::jsonb,                '2026-07-21 09:56:00'),
  (3, 5, 0, 0, 0, '{"allValid":true}'::jsonb,                '2026-07-23 09:56:00');

-- ============================================================
-- 21. enterprise_info_consistency_result + mismatch_detail
-- ============================================================
INSERT INTO schema_preaudit.enterprise_info_consistency_result (application_id, overall_consistent, name_consistent, uscc_consistent, legal_person_consistent, address_consistent, mismatch_count, checked_at)
VALUES
  (1, TRUE, TRUE, TRUE, TRUE, TRUE, 0, '2026-07-16 09:57:00'),
  (2, TRUE, TRUE, TRUE, TRUE, TRUE, 0, '2026-07-21 09:57:00'),
  (3, TRUE, TRUE, TRUE, TRUE, TRUE, 0, '2026-07-23 09:57:00');

INSERT INTO schema_preaudit.enterprise_info_mismatch_detail (result_id, field_type, field_name, consistent, source_values, mismatch_detail)
SELECT r.id, 'BUYER', 'name', TRUE,
       '{"application":"北京中科智造集团","material":"北京中科智造集团"}'::jsonb, NULL
FROM schema_preaudit.enterprise_info_consistency_result r
WHERE r.application_id = 1;

-- ============================================================
-- 22. supplement_list：申请 3 有一条补正（演示）
-- ============================================================
INSERT INTO schema_preaudit.supplement_list (application_id, supplement_items, status, deadline, generated_at)
VALUES
  (3, '[{"materialType":"PAYMENT","reason":"缺少付款凭证"}]'::jsonb, 'PENDING', '2026-08-05', '2026-07-23 10:00:00')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 23. schema_risk.transaction_stability：5 家企业稳定性
-- ============================================================
INSERT INTO schema_risk.transaction_stability (enterprise_id, score, transaction_count_12m, amount_std_dev, trend_data, calculated_at)
VALUES
  (3, 92.00, 200, 50000.00,  '{"trend":"stable"}'::jsonb,  '2026-07-17 16:00:00'),
  (4, 85.00, 120, 30000.00,  '{"trend":"stable"}'::jsonb,  '2026-07-17 16:00:00'),
  (5, 68.00, 80,  45000.00,  '{"trend":"volatile"}'::jsonb,'2026-07-22 15:00:00'),
  (6, 72.00, 200, 10000.00,  '{"trend":"stable"}'::jsonb,  '2026-07-23 10:00:00'),
  (1, 55.00, 30,  20000.00,  '{"trend":"declining"}'::jsonb,'2026-07-15 10:00:00');

-- ============================================================
-- 24. risk_profile：申请 1/2 的风险评分
-- 默认权重配置 id 由 V2 注入（config_name='default-40-30-30'）
-- ============================================================
INSERT INTO schema_risk.risk_profile (application_id, enterprise_id, version, supply_chain_score, transaction_score, material_score, weighted_config_id, overall_score, risk_level, risk_reasons, suggestions, content_hash, generated_at)
VALUES
  (1, 4, 1, 90.00, 85.00, 95.00,
   (SELECT id FROM schema_common.risk_weight_config WHERE config_name='default-40-30-30'),
   90.00, 'LOW',
   '["供应链位置稳定","交易记录连续"]'::jsonb,
   '["可正常推进审批"]'::jsonb,
   'hash_risk_001', '2026-07-17 16:30:00'),
  (2, 5, 1, 70.00, 68.00, 88.00,
   (SELECT id FROM schema_common.risk_weight_config WHERE config_name='default-40-30-30'),
   74.00, 'MID',
   '["交易波动较大","存在快速扩张迹象"]'::jsonb,
   '["建议追加担保措施","缩短融资期限"]'::jsonb,
   'hash_risk_002', '2026-07-22 15:30:00');
