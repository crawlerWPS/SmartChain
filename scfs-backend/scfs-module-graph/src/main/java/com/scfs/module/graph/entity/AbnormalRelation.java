package com.scfs.module.graph.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;

/**
 * 异常关系预警 - 对应 RFC 表13 abnormal_relation（schema_graph）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AbnormalRelation extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    private String enterpriseName;
    /** RAPID_EXPANSION/CIRCULAR/RELATED_PARTY */
    private String abnormalType;
    /** HIGH/MID/LOW */
    private String severity;
    private String description;
    /** 证据 JSONB（环路路径/关联企业/增长率等） */
    private Map<String, Object> evidence;
    /** OPEN/CONFIRMED/DISMISSED */
    private String status;
    private Instant detectedAt;
}
