package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 规则分类 - 对应 RFC 表6 rule_definition.category
 */
@Getter
@AllArgsConstructor
public enum RuleCategory {

    VERIFY("VERIFY", "核验规则"),
    PREAUDIT("PREAUDIT", "预审规则"),
    RISK("RISK", "风险评分规则"),
    GRAPH("GRAPH", "图谱规则");

    private final String code;
    private final String desc;

    public static RuleCategory fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule category: " + code));
    }
}
