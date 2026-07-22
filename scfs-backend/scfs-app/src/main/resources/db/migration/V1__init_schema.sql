-- ============================================================
-- SCFS V1__init_schema.sql - 初始化 5 个 schema 与全部 29 张表
-- 对应 RFC 6.3 阶段 1 S1-1 ~ S1-2
-- ============================================================

-- 1. 创建 5 个 Schema
CREATE SCHEMA IF NOT EXISTS schema_common;
CREATE SCHEMA IF NOT EXISTS schema_graph;
CREATE SCHEMA IF NOT EXISTS schema_verify;
CREATE SCHEMA IF NOT EXISTS schema_preaudit;
CREATE SCHEMA IF NOT EXISTS schema_risk;

SET search_path TO schema_common;

-- 表 1：sys_user
CREATE TABLE IF NOT EXISTS schema_common.sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) UNIQUE NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    real_name       VARCHAR(64) NOT NULL,
    role_code       VARCHAR(32) NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(20),
    status          SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_user_username ON schema_common.sys_user(username);

-- 表 2：sys_role
CREATE TABLE IF NOT EXISTS schema_common.sys_role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(32) UNIQUE NOT NULL,
    role_name       VARCHAR(64) NOT NULL,
    role_type       VARCHAR(16) NOT NULL,
    description     VARCHAR(255),
    status          SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_code ON schema_common.sys_role(role_code);

-- 表 3：sys_role_permission（API 权限）
CREATE TABLE IF NOT EXISTS schema_common.sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    module          VARCHAR(32) NOT NULL,
    permissions     JSONB NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_role_perm_role ON schema_common.sys_role_permission(role_id);

-- 表 3a：sys_menu（菜单树形）
CREATE TABLE IF NOT EXISTS schema_common.sys_menu (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT NOT NULL DEFAULT 0,
    menu_name       VARCHAR(64) NOT NULL,
    menu_code       VARCHAR(64) UNIQUE NOT NULL,
    menu_type       VARCHAR(16) NOT NULL,
    path            VARCHAR(128),
    component       VARCHAR(128),
    permission      VARCHAR(64),
    icon            VARCHAR(64),
    sort            INT NOT NULL DEFAULT 0,
    visible         SMALLINT NOT NULL DEFAULT 1,
    status          SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_menu_parent ON schema_common.sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_menu_code ON schema_common.sys_menu(menu_code);

-- 表 3b：sys_role_menu（角色-菜单关联）
CREATE TABLE IF NOT EXISTS schema_common.sys_role_menu (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    menu_id         BIGINT NOT NULL,
    UNIQUE(role_id, menu_id)
);
CREATE INDEX IF NOT EXISTS idx_role_menu_role ON schema_common.sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_menu ON schema_common.sys_role_menu(menu_id);

-- 表 4：sys_audit_log（审计日志，按月分区）
CREATE TABLE IF NOT EXISTS schema_common.sys_audit_log (
    id              BIGSERIAL,
    user_id         BIGINT NOT NULL,
    username        VARCHAR(64) NOT NULL,
    module          VARCHAR(32) NOT NULL,
    action          VARCHAR(64) NOT NULL,
    target_type     VARCHAR(32),
    target_id       VARCHAR(64),
    detail          JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_user ON schema_common.sys_audit_log(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_target ON schema_common.sys_audit_log(target_type, target_id);

-- 创建当前月和下月分区
CREATE TABLE IF NOT EXISTS schema_common.sys_audit_log_202607 PARTITION OF schema_common.sys_audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS schema_common.sys_audit_log_202608 PARTITION OF schema_common.sys_audit_log
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- 表 5：file_object
CREATE TABLE IF NOT EXISTS schema_common.file_object (
    id                  BIGSERIAL PRIMARY KEY,
    file_name           VARCHAR(255) NOT NULL,
    file_type           VARCHAR(32) NOT NULL,
    file_size           BIGINT NOT NULL,
    minio_bucket        VARCHAR(64) NOT NULL,
    minio_object_key    VARCHAR(255) NOT NULL,
    content_hash        VARCHAR(64) NOT NULL,
    uploaded_by         BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_file_hash ON schema_common.file_object(content_hash);
CREATE INDEX IF NOT EXISTS idx_file_uploader ON schema_common.file_object(uploaded_by);

-- 表 6：rule_definition
CREATE TABLE IF NOT EXISTS schema_common.rule_definition (
    id              BIGSERIAL PRIMARY KEY,
    rule_code       VARCHAR(64) UNIQUE NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    category        VARCHAR(32) NOT NULL,
    drl_content     TEXT NOT NULL,
    params          JSONB,
    status          SMALLINT NOT NULL DEFAULT 1,
    version         INT NOT NULL DEFAULT 1,
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_rule_category ON schema_common.rule_definition(category, status);
CREATE INDEX IF NOT EXISTS idx_rule_code ON schema_common.rule_definition(rule_code);

-- 表 7：rule_change_log（双岗）
CREATE TABLE IF NOT EXISTS schema_common.rule_change_log (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    rule_code       VARCHAR(64) NOT NULL,
    change_type     VARCHAR(16) NOT NULL,
    old_version     INT,
    new_version     INT NOT NULL,
    old_content     TEXT,
    new_content     TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    maker_id        BIGINT NOT NULL,
    checker_id      BIGINT,
    checked_at      TIMESTAMP,
    reject_reason   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rule_maker_checker CHECK (maker_id <> checker_id)
);
CREATE INDEX IF NOT EXISTS idx_rule_change_rule ON schema_common.rule_change_log(rule_id);
CREATE INDEX IF NOT EXISTS idx_rule_change_status ON schema_common.rule_change_log(status);

-- 表 8：risk_weight_config（双岗）
CREATE TABLE IF NOT EXISTS schema_common.risk_weight_config (
    id                      BIGSERIAL PRIMARY KEY,
    config_name             VARCHAR(64) NOT NULL,
    supply_chain_weight     INT NOT NULL,
    transaction_weight      INT NOT NULL,
    material_weight         INT NOT NULL,
    low_risk_threshold      INT NOT NULL DEFAULT 85,
    mid_risk_threshold      INT NOT NULL DEFAULT 70,
    high_risk_threshold     INT NOT NULL DEFAULT 50,
    status                  VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    version                 INT NOT NULL DEFAULT 1,
    maker_id                BIGINT NOT NULL,
    checker_id              BIGINT,
    checked_at              TIMESTAMP,
    reject_reason           TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_weight_sum CHECK (supply_chain_weight + transaction_weight + material_weight = 100),
    CONSTRAINT chk_weight_maker_checker CHECK (maker_id <> checker_id)
);

-- 表 20：material_checklist_template（双岗，schema_common 共享）
CREATE TABLE IF NOT EXISTS schema_common.material_checklist_template (
    id                  BIGSERIAL PRIMARY KEY,
    business_type       VARCHAR(32) UNIQUE NOT NULL,
    required_materials  JSONB NOT NULL,
    version             INT NOT NULL DEFAULT 1,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    maker_id            BIGINT NOT NULL,
    checker_id          BIGINT,
    checked_at          TIMESTAMP,
    reject_reason       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_template_maker_checker CHECK (maker_id <> checker_id)
);

-- ========== schema_graph ==========
-- 表 9：enterprise
CREATE TABLE IF NOT EXISTS schema_graph.enterprise (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(128) NOT NULL,
    uscc                    VARCHAR(18) UNIQUE NOT NULL,
    industry                VARCHAR(64),
    legal_person            VARCHAR(64),
    registered_capital      DECIMAL(18,2),
    establish_date          DATE,
    address                 VARCHAR(255),
    data_source             VARCHAR(16) NOT NULL DEFAULT 'MOCK',
    last_synced_at          TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_enterprise_uscc ON schema_graph.enterprise(uscc);
CREATE INDEX IF NOT EXISTS idx_enterprise_name ON schema_graph.enterprise(name);

-- 表 10：supply_chain_relation
CREATE TABLE IF NOT EXISTS schema_graph.supply_chain_relation (
    id                      BIGSERIAL PRIMARY KEY,
    from_enterprise_id      BIGINT NOT NULL,
    to_enterprise_id        BIGINT NOT NULL,
    relation_type           VARCHAR(16) NOT NULL,
    first_coop_date         DATE,
    last_coop_date          DATE,
    total_transactions      INT DEFAULT 0,
    total_amount            DECIMAL(18,2) DEFAULT 0,
    core_enterprise_id      BIGINT,
    level                   INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(from_enterprise_id, to_enterprise_id, relation_type)
);
CREATE INDEX IF NOT EXISTS idx_relation_from ON schema_graph.supply_chain_relation(from_enterprise_id);
CREATE INDEX IF NOT EXISTS idx_relation_to ON schema_graph.supply_chain_relation(to_enterprise_id);
CREATE INDEX IF NOT EXISTS idx_relation_core ON schema_graph.supply_chain_relation(core_enterprise_id);

-- 表 11：enterprise_role
CREATE TABLE IF NOT EXISTS schema_graph.enterprise_role (
    id                      BIGSERIAL PRIMARY KEY,
    enterprise_id           BIGINT NOT NULL,
    role                    VARCHAR(32) NOT NULL,
    core_enterprise_id      BIGINT,
    coop_duration_years     DECIMAL(5,1),
    coop_enterprise_count   INT,
    influence_level         VARCHAR(8) NOT NULL,
    credibility_level       VARCHAR(8) NOT NULL,
    calculated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_enterprise ON schema_graph.enterprise_role(enterprise_id);

-- 表 12：enterprise_position_analysis
CREATE TABLE IF NOT EXISTS schema_graph.enterprise_position_analysis (
    id                      BIGSERIAL PRIMARY KEY,
    enterprise_id           BIGINT NOT NULL,
    in_core_chain           BOOLEAN NOT NULL,
    distance_to_core        INT,
    upstream_stable         BOOLEAN,
    downstream_stable       BOOLEAN,
    credibility             VARCHAR(8) NOT NULL,
    credibility_reason      TEXT,
    calculated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_position_enterprise ON schema_graph.enterprise_position_analysis(enterprise_id);

-- 表 13：abnormal_relation
CREATE TABLE IF NOT EXISTS schema_graph.abnormal_relation (
    id                      BIGSERIAL PRIMARY KEY,
    enterprise_id           BIGINT NOT NULL,
    abnormal_type           VARCHAR(32) NOT NULL,
    severity                VARCHAR(8) NOT NULL,
    description             TEXT NOT NULL,
    evidence                JSONB,
    status                  VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    detected_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_abnormal_enterprise ON schema_graph.abnormal_relation(enterprise_id, abnormal_type);

-- ========== schema_verify ==========
-- 表 14：financing_application
CREATE TABLE IF NOT EXISTS schema_verify.financing_application (
    id                      BIGSERIAL PRIMARY KEY,
    app_no                  VARCHAR(32) UNIQUE NOT NULL,
    enterprise_id           BIGINT NOT NULL,
    business_type           VARCHAR(32) NOT NULL,
    financing_amount        DECIMAL(18,2) NOT NULL,
    submitted_by            BIGINT NOT NULL,
    status                  VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    current_handler         BIGINT,
    submitted_at            TIMESTAMP,
    approved_at             TIMESTAMP,
    version                 INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_app_no ON schema_verify.financing_application(app_no);
CREATE INDEX IF NOT EXISTS idx_app_enterprise ON schema_verify.financing_application(enterprise_id);
CREATE INDEX IF NOT EXISTS idx_app_status ON schema_verify.financing_application(status);

-- 表 15：application_status_history
CREATE TABLE IF NOT EXISTS schema_verify.application_status_history (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    from_status             VARCHAR(32),
    to_status               VARCHAR(32) NOT NULL,
    operator_id             BIGINT NOT NULL,
    remark                  TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_status_history_app ON schema_verify.application_status_history(application_id, created_at);

-- 表 16：application_material
CREATE TABLE IF NOT EXISTS schema_verify.application_material (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    file_object_id          BIGINT NOT NULL,
    material_type           VARCHAR(32) NOT NULL,
    identified_by           VARCHAR(16) NOT NULL DEFAULT 'AUTO',
    confidence              DECIMAL(5,2),
    status                  VARCHAR(16) NOT NULL DEFAULT 'IDENTIFIED',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_app_material_app ON schema_verify.application_material(application_id);

-- 表 17：material_recognition_result
CREATE TABLE IF NOT EXISTS schema_verify.material_recognition_result (
    id                          BIGSERIAL PRIMARY KEY,
    application_material_id     BIGINT UNIQUE NOT NULL,
    buyer_name                  VARCHAR(128),
    buyer_uscc                  VARCHAR(18),
    seller_name                 VARCHAR(128),
    seller_uscc                 VARCHAR(18),
    commodity                   TEXT,
    amount                      DECIMAL(18,2),
    amount_in_words             VARCHAR(128),
    contract_date               DATE,
    order_date                  DATE,
    invoice_date                DATE,
    logistics_date              DATE,
    acceptance_date             DATE,
    payment_date                DATE,
    contract_period             VARCHAR(64),
    payment_term                VARCHAR(64),
    transaction_no              VARCHAR(64),
    field_confidence            JSONB,
    raw_ocr_result              JSONB,
    field_positions             JSONB,
    recognized_at               TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recog_material ON schema_verify.material_recognition_result(application_material_id);

-- 表 18：verify_check_result
CREATE TABLE IF NOT EXISTS schema_verify.verify_check_result (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    check_type             VARCHAR(32) NOT NULL,
    result                  VARCHAR(16) NOT NULL,
    details                 JSONB,
    executed_rules          JSONB,
    executed_at             TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_verify_app ON schema_verify.verify_check_result(application_id, check_type);

-- 表 19：verify_report
CREATE TABLE IF NOT EXISTS schema_verify.verify_report (
    id                      BIGSERIAL PRIMARY KEY,
    report_no               VARCHAR(32) UNIQUE NOT NULL,
    application_id          BIGINT NOT NULL,
    version                 INT NOT NULL DEFAULT 1,
    overall_assessment      VARCHAR(16) NOT NULL,
    abnormal_count          INT NOT NULL,
    risk_hints              JSONB,
    content_snapshot        JSONB NOT NULL,
    content_hash            VARCHAR(64) NOT NULL,
    generated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_report_app ON schema_verify.verify_report(application_id);
CREATE INDEX IF NOT EXISTS idx_report_no ON schema_verify.verify_report(report_no);

-- ========== schema_preaudit ==========
-- 表 21：material_completeness_result
CREATE TABLE IF NOT EXISTS schema_preaudit.material_completeness_result (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    required_count          INT NOT NULL,
    submitted_count         INT NOT NULL,
    completeness_pct        DECIMAL(5,2) NOT NULL,
    missing_materials      JSONB,
    checked_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_completeness_app ON schema_preaudit.material_completeness_result(application_id);

-- 表 22：material_validity_result
CREATE TABLE IF NOT EXISTS schema_preaudit.material_validity_result (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    total_files             INT NOT NULL,
    expired_count           INT NOT NULL DEFAULT 0,
    incomplete_count        INT NOT NULL DEFAULT 0,
    abnormal_count          INT NOT NULL DEFAULT 0,
    details                 JSONB,
    checked_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 表 23a：enterprise_info_consistency_result（主表）
CREATE TABLE IF NOT EXISTS schema_preaudit.enterprise_info_consistency_result (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    overall_consistent      BOOLEAN NOT NULL,
    name_consistent         BOOLEAN NOT NULL,
    uscc_consistent         BOOLEAN NOT NULL,
    legal_person_consistent BOOLEAN NOT NULL,
    address_consistent      BOOLEAN NOT NULL,
    mismatch_count          INT NOT NULL DEFAULT 0,
    checked_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_consistency_app ON schema_preaudit.enterprise_info_consistency_result(application_id);

-- 表 23b：enterprise_info_mismatch_detail
CREATE TABLE IF NOT EXISTS schema_preaudit.enterprise_info_mismatch_detail (
    id                      BIGSERIAL PRIMARY KEY,
    result_id               BIGINT NOT NULL,
    field_type              VARCHAR(16) NOT NULL,
    field_name              VARCHAR(32) NOT NULL,
    consistent              BOOLEAN NOT NULL,
    source_values           JSONB NOT NULL,
    mismatch_detail         TEXT
);
CREATE INDEX IF NOT EXISTS idx_mismatch_result ON schema_preaudit.enterprise_info_mismatch_detail(result_id);
CREATE INDEX IF NOT EXISTS idx_mismatch_field ON schema_preaudit.enterprise_info_mismatch_detail(field_type);

-- 表 24：supplement_list
CREATE TABLE IF NOT EXISTS schema_preaudit.supplement_list (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    supplement_items        JSONB NOT NULL,
    status                  VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    deadline                DATE,
    generated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_supplement_app ON schema_preaudit.supplement_list(application_id, status);

-- ========== schema_risk ==========
-- 表 25：risk_profile
CREATE TABLE IF NOT EXISTS schema_risk.risk_profile (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL,
    enterprise_id           BIGINT NOT NULL,
    version                 INT NOT NULL DEFAULT 1,
    supply_chain_score      DECIMAL(5,2) NOT NULL,
    transaction_score       DECIMAL(5,2) NOT NULL,
    material_score          DECIMAL(5,2) NOT NULL,
    weighted_config_id      BIGINT NOT NULL,
    overall_score           DECIMAL(5,2) NOT NULL,
    risk_level              VARCHAR(16) NOT NULL,
    risk_reasons            JSONB NOT NULL,
    suggestions             JSONB NOT NULL,
    content_hash            VARCHAR(64) NOT NULL,
    generated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_risk_app ON schema_risk.risk_profile(application_id);
CREATE INDEX IF NOT EXISTS idx_risk_enterprise ON schema_risk.risk_profile(enterprise_id);

-- 表 26：transaction_stability
CREATE TABLE IF NOT EXISTS schema_risk.transaction_stability (
    id                      BIGSERIAL PRIMARY KEY,
    enterprise_id           BIGINT NOT NULL,
    score                   DECIMAL(5,2) NOT NULL,
    transaction_count_12m  INT,
    amount_std_dev          DECIMAL(18,2),
    trend_data              JSONB,
    calculated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_stability_enterprise ON schema_risk.transaction_stability(enterprise_id);
