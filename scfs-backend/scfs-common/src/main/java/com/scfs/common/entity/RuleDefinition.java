package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 规则定义 - 对应 RFC 表6 rule_definition（schema_common）
 *
 * <p>Drools DRL 规则内容存储，按 category 分类管理</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RuleDefinition extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 规则编码，如 R_AMOUNT_DIFF */
    private String ruleCode;
    /** 规则名称 */
    private String ruleName;
    /** 分类：VERIFY/PREAUDIT/RISK/GRAPH */
    private String category;
    /** Drools DRL 规则内容 */
    private String drlContent;
    /** 规则参数 JSONB（如金额阈值、时间窗口） */
    private Map<String, Object> params;
    /** 1=启用, 0=禁用 */
    private Short status;
    /** 版本号 */
    private Integer version;
    /** 创建人 */
    private Long createdBy;
}
