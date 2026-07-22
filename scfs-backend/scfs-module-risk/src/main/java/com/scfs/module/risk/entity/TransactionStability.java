package com.scfs.module.risk.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 交易稳定性评分 - 对应 RFC 表26 transaction_stability（schema_risk）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionStability extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    /** 稳定性评分（0-100） */
    private BigDecimal score;
    private Integer transactionCount12m;
    private BigDecimal amountStdDev;
    /** 近 12 月金额趋势 JSONB [{month, amount}] */
    private List<Map<String, Object>> trendData;
    private Instant calculatedAt;
}
