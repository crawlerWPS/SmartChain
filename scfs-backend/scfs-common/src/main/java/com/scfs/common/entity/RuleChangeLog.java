package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 规则变更日志 - 对应 RFC 表7 rule_change_log（schema_common，双岗）
 *
 * <p>CHECK (maker_id &lt;&gt; checker_id) — 经办与复核不能为同一人</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RuleChangeLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联规则 */
    private Long ruleId;
    /** 冗余 */
    private String ruleCode;
    /** CREATE/UPDATE/ENABLE/DISABLE */
    private String changeType;
    /** 旧版本 */
    private Integer oldVersion;
    /** 新版本 */
    private Integer newVersion;
    /** 旧 DRL/参数 */
    private String oldContent;
    /** 新 DRL/参数 */
    private String newContent;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    /** 经办人 (R-03a) */
    private Long makerId;
    /** 复核人 (R-03b) */
    private Long checkerId;
    /** 复核时间 */
    private Instant checkedAt;
    /** 拒绝原因 */
    private String rejectReason;
}
