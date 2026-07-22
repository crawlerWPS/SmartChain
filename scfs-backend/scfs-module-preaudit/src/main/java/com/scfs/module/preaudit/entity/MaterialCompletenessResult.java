package com.scfs.module.preaudit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 完整性检查结果 - 对应 RFC 表21 material_completeness_result（schema_preaudit）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialCompletenessResult extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Integer requiredCount;
    private Integer submittedCount;
    private BigDecimal completenessPct;
    /** 缺失材料列表 JSONB */
    private List<String> missingMaterials;
    private Instant checkedAt;
}
