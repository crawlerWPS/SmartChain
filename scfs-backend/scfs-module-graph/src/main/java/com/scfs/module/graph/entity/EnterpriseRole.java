package com.scfs.module.graph.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 企业角色识别结果 - 对应 RFC 表11 enterprise_role（schema_graph）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnterpriseRole extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    /** CORE/KEY_SUPPLIER/TIER1/TIER2/NORMAL/EDGE */
    private String role;
    private Long coreEnterpriseId;
    private BigDecimal coopDurationYears;
    private Integer coopEnterpriseCount;
    /** HIGH/MID/LOW */
    private String influenceLevel;
    /** HIGH/MID/LOW */
    private String credibilityLevel;
    private Instant calculatedAt;
}
