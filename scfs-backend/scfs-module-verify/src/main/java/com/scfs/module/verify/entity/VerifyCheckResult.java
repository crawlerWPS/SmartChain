package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 核验项检查结果 - 对应 RFC 表18 verify_check_result（schema_verify）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VerifyCheckResult extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    /** SUBJECT/AMOUNT/TIME/REPEAT */
    private String checkType;
    /** PASS/ABNORMAL/MISSING */
    private String result;
    /** 检查明细 JSONB */
    private Map<String, Object> details;
    /** 执行的规则编码列表 JSONB */
    private List<String> executedRules;
    private Instant executedAt;
}
