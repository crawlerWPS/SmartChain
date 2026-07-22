package com.scfs.module.graph.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应链关系 - 对应 RFC 表10 supply_chain_relation（schema_graph）
 *
 * <p>UNIQUE(from_enterprise_id, to_enterprise_id, relation_type)</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplyChainRelation extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 上游企业 */
    private Long fromEnterpriseId;
    /** 下游企业 */
    private Long toEnterpriseId;
    /** PURCHASE/SUPPLY/CONTRACT/INVOICE/LOGISTICS/FUND */
    private String relationType;
    /** 首次合作 */
    private LocalDate firstCoopDate;
    /** 最近合作 */
    private LocalDate lastCoopDate;
    /** 累计交易笔数 */
    private Integer totalTransactions;
    /** 累计交易金额 */
    private BigDecimal totalAmount;
    /** 所属核心企业 */
    private Long coreEnterpriseId;
    /** 层级（1=一级, 2=二级） */
    private Integer level;
}
