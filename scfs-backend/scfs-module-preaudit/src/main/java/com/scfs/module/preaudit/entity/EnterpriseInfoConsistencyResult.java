package com.scfs.module.preaudit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 企业信息一致性检查-主表 - 对应 RFC 表23a enterprise_info_consistency_result（schema_preaudit）
 *
 * <p>4 要素：name/uscc/legal_person/address</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnterpriseInfoConsistencyResult extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Boolean overallConsistent;
    private Boolean nameConsistent;
    private Boolean usccConsistent;
    private Boolean legalPersonConsistent;
    private Boolean addressConsistent;
    private Integer mismatchCount;
    private Instant checkedAt;
}
