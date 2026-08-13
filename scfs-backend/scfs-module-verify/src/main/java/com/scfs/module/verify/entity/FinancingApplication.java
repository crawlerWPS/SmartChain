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
    /** 买方客户号，直接使用 schema_graph.enterprise.id */
    private Long buyerEnterpriseId;
    /** 卖方客户号，直接使用 schema_graph.enterprise.id */
    private Long sellerEnterpriseId;
    /** 查询时由企业主数据联表返回 */
    private String buyerName;
    /** 买方统一社会信用代码（查询时由企业主数据联表返回） */
    private String buyerUscc;
    /** 查询时由企业主数据联表返回 */
    private String sellerName;
    /** 卖方统一社会信用代码（查询时由企业主数据联表返回） */
    private String sellerUscc;
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
