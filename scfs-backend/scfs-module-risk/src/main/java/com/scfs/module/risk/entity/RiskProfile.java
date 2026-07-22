package com.scfs.module.risk.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 风险画像 - 对应 RFC 表25 risk_profile（schema_risk）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskProfile extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Long enterpriseId;
    private Integer version;
    private BigDecimal supplyChainScore;
    private BigDecimal transactionScore;
    private BigDecimal materialScore;
    private Long weightedConfigId;
    private BigDecimal overallScore;
    /** LOW/MID/HIGH/EXTREME */
    private String riskLevel;
    /** 风险原因列表 JSONB */
    private List<String> riskReasons;
    /** 建议关注事项 JSONB */
    private List<String> suggestions;
    private String contentHash;
    private Instant generatedAt;
}
