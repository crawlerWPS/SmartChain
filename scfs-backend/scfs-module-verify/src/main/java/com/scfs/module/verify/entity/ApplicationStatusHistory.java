package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 申请状态流转历史 - 对应 RFC 表15 application_status_history（schema_verify）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationStatusHistory extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    /** 原状态 */
    private String fromStatus;
    /** 新状态 */
    private String toStatus;
    /** 操作人 */
    private Long operatorId;
    /** 备注（判定理由、撤销原因等） */
    private String remark;
}
