package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 融资申请材料关联 - 对应 RFC 表16 application_material（schema_verify）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationMaterial extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private Long fileObjectId;
    /** 关联文件对象，仅用于查询展示。 */
    private String fileName;
    private String fileType;
    private Long fileSize;
    /** CONTRACT/INVOICE/ORDER/LOGISTICS/ACCEPTANCE/PAYMENT/QUALIFICATION */
    private String materialType;
    /** AUTO/MANUAL */
    private String identifiedBy;
    /** 置信度（0-100） */
    private BigDecimal confidence;
    /** IDENTIFIED/PENDING_MANUAL/UNRECOGNIZED */
    private String status;
}
