-- ============================================================
-- SCFS V6__normalize_drl_newlines.sql
-- 修复初始化规则中将字面量 \\n+-- 保存为换行转义文本的问题
-- ============================================================

UPDATE schema_common.rule_definition
SET drl_content = replace(drl_content, E'\\\\n', E'\\n'),
    updated_at = NOW()
WHERE position(E'\\\\n' IN drl_content) > 0;

UPDATE schema_common.rule_change_log
SET old_content = replace(old_content, E'\\\\n', E'\\n'),
    new_content = replace(new_content, E'\\\\n', E'\\n')
WHERE position(E'\\\\n' IN COALESCE(old_content, '') || COALESCE(new_content, '')) > 0;
