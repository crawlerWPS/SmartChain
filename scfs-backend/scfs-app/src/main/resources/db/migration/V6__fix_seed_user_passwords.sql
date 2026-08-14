-- Keep all documented seed accounts aligned with the default password: admin123.
-- This is a forward-only migration because V2 may already have been applied.
UPDATE schema_common.sys_user
SET password_hash = '$2a$10$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS',
    updated_at = NOW()
WHERE username IN ('admin', 'rm_zhang', 'rco_li', 'maker_wang',
                   'checker_zhao', 'ops_sun', 'audit_zhou');
