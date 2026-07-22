package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;

/**
 * 审计日志实体 - 对应 RFC 表4 sys_audit_log（schema_common，按月分区）
 *
 * <p>分区策略：按月分区表 sys_audit_log_yyyymm，查询时自动路由</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysAuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 操作用户 */
    private Long userId;
    /** 冗余，便于查询 */
    private String username;
    /** 模块：GRAPH/VERIFY/PREAUDIT/RISK/RULE/USER */
    private String module;
    /** 操作：LOGIN/CREATE/UPDATE/DELETE/EXPORT/APPROVE/REJECT */
    private String action;
    /** 操作对象类型 */
    private String targetType;
    /** 操作对象 ID */
    private String targetId;
    /** 操作详情 JSONB（变更前后） */
    private Map<String, Object> detail;
    /** IP */
    private String ipAddress;
}
