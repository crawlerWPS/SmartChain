package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 风险权重配置 - 对应 RFC 表8 risk_weight_config（schema_common，双岗）
 *
 * <p>CHECK (supply_chain_weight + transaction_weight + material_weight = 100)</p>
 * <p>CHECK (maker_id &lt;&gt; checker_id)</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskWeightConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String configName;
    private Integer supplyChainWeight;
    private Integer transactionWeight;
    private Integer materialWeight;
    private Integer lowRiskThreshold;
    private Integer midRiskThreshold;
    private Integer highRiskThreshold;
    /** PENDING/APPROVED/REJECTED/ENABLED/DISABLED */
    private String status;
    private Integer version;
    private Long makerId;
    private Long checkerId;
    private Instant checkedAt;
    private String rejectReason;
}
