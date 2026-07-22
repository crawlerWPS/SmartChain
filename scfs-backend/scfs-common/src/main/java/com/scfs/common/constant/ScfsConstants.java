package com.scfs.common.constant;

/**
 * SCFS 系统常量
 */
public final class ScfsConstants {

    private ScfsConstants() {}

    /** Schema 名称 */
    public static final String SCHEMA_COMMON = "schema_common";
    public static final String SCHEMA_GRAPH = "schema_graph";
    public static final String SCHEMA_VERIFY = "schema_verify";
    public static final String SCHEMA_PREAUDIT = "schema_preaudit";
    public static final String SCHEMA_RISK = "schema_risk";

    /** 角色编码（对应 sys_role.role_code） */
    public static final String ROLE_RM = "RM";                    // 客户经理 R-01
    public static final String ROLE_RCO = "RCO";                  // 风控审核员 R-02
    public static final String ROLE_OPS_MAKER = "OPS_MAKER";       // 规则经办岗 R-03a
    public static final String ROLE_OPS_CHECKER = "OPS_CHECKER";  // 规则复核岗 R-03b
    public static final String ROLE_OPS = "OPS";                   // 运营主管 R-03c
    public static final String ROLE_AUDIT = "AUDIT";               // 审计 R-04
    public static final String ROLE_ADMIN = "ADMIN";               // 系统管理员 R-05

    /** 模块编码（用于权限校验） */
    public static final String MODULE_GRAPH = "GRAPH";
    public static final String MODULE_VERIFY = "VERIFY";
    public static final String MODULE_PREAUDIT = "PREAUDIT";
    public static final String MODULE_RISK = "RISK";
    public static final String MODULE_RULE = "RULE";
    public static final String MODULE_USER = "USER";
    public static final String MODULE_AUDIT = "AUDIT";

    /** 权限操作 */
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_EXPORT = "export";
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";

    /** 默认权重（RFC 默认 40/30/30） */
    public static final int DEFAULT_SUPPLY_CHAIN_WEIGHT = 40;
    public static final int DEFAULT_TRANSACTION_WEIGHT = 30;
    public static final int DEFAULT_MATERIAL_WEIGHT = 30;

    /** 默认阈值 */
    public static final int DEFAULT_LOW_RISK_THRESHOLD = 85;
    public static final int DEFAULT_MID_RISK_THRESHOLD = 70;
    public static final int DEFAULT_HIGH_RISK_THRESHOLD = 50;

    /** 文件 Bucket */
    public static final String BUCKET_MATERIALS = "scfs-materials";
    public static final String BUCKET_REPORTS = "scfs-reports";
    public static final String BUCKET_EXPORTS = "scfs-exports";

    /** 缓存键前缀 */
    public static final String CACHE_USER_MENU = "scfs:user:menu:";
    public static final String CACHE_USER_PERMISSION = "scfs:user:perm:";
    public static final String CACHE_RULE_VERSION = "scfs:rule:version:";
    public static final String CACHE_ENTERPRISE_GRAPH = "scfs:graph:enterprise:";

    /** 图谱算法常量 */
    public static final int CORE_ENTERPRISE_DEGREE_THRESHOLD = 20;
    public static final int CORE_ENTERPRISE_OUT_DEGREE_MIN = 5;
    public static final int CIRCULAR_TRADE_MAX_DEPTH = 5;
    public static final int RAPID_EXPANSION_MONTHS = 6;
    public static final double RAPID_EXPANSION_RATE = 0.3;

    /** 双岗机制 */
    public static final String DUAL_STATUS_PENDING = "PENDING";
    public static final String DUAL_STATUS_APPROVED = "APPROVED";
    public static final String DUAL_STATUS_REJECTED = "REJECTED";
    public static final String DUAL_STATUS_ENABLED = "ENABLED";
    public static final String DUAL_STATUS_DISABLED = "DISABLED";
}
