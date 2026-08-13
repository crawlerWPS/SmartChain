package com.scfs.module.graph.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 关系导入行。买方为关系终点，卖方为关系起点。 */
@Data
public class RelationImportRow {
    private Integer rowNumber;
    private String buyerName;
    private String buyerUscc;
    private String sellerName;
    private String sellerUscc;
    private String relationType;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String remark;
}
