package com.scfs.module.verify.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * 材料识别结构化结果 - 对应 RFC 表17 material_recognition_result（schema_verify）
 *
 * <p>1:1 关联 application_material</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialRecognitionResult extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long applicationMaterialId;
    private String buyerName;
    private String buyerUscc;
    private String sellerName;
    private String sellerUscc;
    private String commodity;
    private BigDecimal amount;
    private String amountInWords;
    private LocalDate contractDate;
    private LocalDate orderDate;
    private LocalDate invoiceDate;
    private LocalDate logisticsDate;
    private LocalDate acceptanceDate;
    private LocalDate paymentDate;
    private String contractPeriod;
    private String paymentTerm;
    private String transactionNo;
    /** 各字段置信度 JSONB {field: confidence} */
    private Map<String, Object> fieldConfidence;
    /** 原始 OCR 结果 JSONB */
    private Map<String, Object> rawOcrResult;
    /** 字段位置标注 JSONB */
    private Map<String, Object> fieldPositions;
    private Instant recognizedAt;
}
