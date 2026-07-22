package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 异常关系类型 - 对应 RFC 表13 abnormal_relation.abnormal_type
 */
@Getter
@AllArgsConstructor
public enum AbnormalType {

    RAPID_EXPANSION("RAPID_EXPANSION", "快速扩张"),
    CIRCULAR("CIRCULAR", "循环贸易"),
    RELATED_PARTY("RELATED_PARTY", "关联方");

    private final String code;
    private final String desc;

    public static AbnormalType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown abnormal type: " + code));
    }
}
