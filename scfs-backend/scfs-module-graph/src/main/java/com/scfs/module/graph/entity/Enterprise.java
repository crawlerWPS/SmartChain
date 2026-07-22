package com.scfs.module.graph.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 企业实体 - 对应 RFC 表9 enterprise（schema_graph）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Enterprise extends com.scfs.common.entity.BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 企业名称 */
    private String name;
    /** 统一社会信用代码 */
    private String uscc;
    /** 所属行业 */
    private String industry;
    /** 法定代表人 */
    private String legalPerson;
    /** 注册资本（万元） */
    private BigDecimal registeredCapital;
    /** 成立日期 */
    private LocalDate establishDate;
    /** 注册地址 */
    private String address;
    /** MOCK/CIF */
    private String dataSource;
    /** 最后同步时间 */
    private Instant lastSyncedAt;
}
