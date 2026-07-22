package com.scfs.module.graph.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 企业位置分析结果 - 对应 RFC 表12 enterprise_position_analysis（schema_graph）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnterprisePositionAnalysis extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    /** 是否在核心企业体系 */
    private Boolean inCoreChain;
    /** 距核心企业层级 */
    private Integer distanceToCore;
    /** 上游稳定 */
    private Boolean upstreamStable;
    /** 下游稳定 */
    private Boolean downstreamStable;
    /** HIGH/MID/LOW/INSUFFICIENT */
    private String credibility;
    private String credibilityReason;
    private Instant calculatedAt;
}
