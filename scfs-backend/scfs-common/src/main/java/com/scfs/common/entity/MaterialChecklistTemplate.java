package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 材料清单模板 - 对应 RFC 表20 material_checklist_template（schema_common，双岗）
 *
 * <p>CHECK (maker_id &lt;&gt; checker_id)</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialChecklistTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** AR_FINANCING/FACTORING/ORDER_FINANCING */
    private String businessType;
    /** 必备材料列表 JSONB */
    private List<String> requiredMaterials;
    private Integer version;
    /** PENDING/APPROVED/REJECTED/ENABLED/DISABLED */
    private String status;
    private Long makerId;
    private Long checkerId;
    private java.time.Instant checkedAt;
    private String rejectReason;
}
