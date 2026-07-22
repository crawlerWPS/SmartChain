package com.scfs.module.preaudit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 企业信息不一致明细 - 对应 RFC 表23b enterprise_info_mismatch_detail（schema_preaudit）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnterpriseInfoMismatchDetail extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long resultId;
    /** NAME/USCC/LEGAL_PERSON/ADDRESS */
    private String fieldType;
    private String fieldName;
    private Boolean consistent;
    /** 各材料中的值 JSONB [{material_id, material_type, value}] */
    private List<Map<String, Object>> sourceValues;
    private String mismatchDetail;
}
