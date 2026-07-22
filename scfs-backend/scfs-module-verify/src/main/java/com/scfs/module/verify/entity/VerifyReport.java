package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 真实性核验报告 - 对应 RFC 表19 verify_report（schema_verify）
 *
 * <p>content_snapshot 不可篡改；content_hash 用于完整性校验（SHA-256）</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VerifyReport extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 报告编号 RPT-yyyymmdd-xxxx */
    private String reportNo;
    private Long applicationId;
    private Integer version;
    /** LOW/MID/HIGH risk */
    private String overallAssessment;
    private Integer abnormalCount;
    /** 风险提示列表 JSONB */
    private List<String> riskHints;
    /** 报告快照（不可篡改） JSONB */
    private Map<String, Object> contentSnapshot;
    /** 内容哈希（完整性校验） */
    private String contentHash;
    private Instant generatedAt;
}
