package com.scfs.module.preaudit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;

/**
 * 有效性检查结果 - 对应 RFC 表22 material_validity_result（schema_preaudit）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialValidityResult extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Integer totalFiles;
    private Integer expiredCount;
    private Integer incompleteCount;
    private Integer abnormalCount;
    /** 异常项详情 JSONB */
    private Map<String, Object> details;
    private Instant checkedAt;
}
