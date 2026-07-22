package com.scfs.module.preaudit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 补正清单 - 对应 RFC 表24 supplement_list（schema_preaudit）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplementList extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    /** 补正项列表 JSONB [{type, reason, suggestion}] */
    private List<Map<String, Object>> supplementItems;
    /** PENDING/COMPLETED */
    private String status;
    private LocalDate deadline;
    private Instant generatedAt;
}
