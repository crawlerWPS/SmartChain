package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 融资申请 - 对应 RFC 表14 financing_application（schema_verify）
 *
 * <p>状态机 9 状态详见 {@link com.scfs.common.enums.ApplicationStatus}</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FinancingApplication extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 申请编号 APP-yyyymmdd-xxxx */
    private String appNo;
    /** 融资企业 */
    private Long enterpriseId;
    /** AR_FINANCING/FACTORING/ORDER_FINANCING */
    private String businessType;
    /** 融资金额 */
    private BigDecimal financingAmount;
    /** 提交人（客户经理） */
    private Long submittedBy;
    /** 状态机 9 状态 */
    private String status;
    /** 当前处理人 */
    private Long currentHandler;
    /** 提交时间 */
    private Instant submittedAt;
    /** 审批完成时间 */
    private Instant approvedAt;
    /** 乐观锁版本号 */
    private Integer version;
}
