package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 核验类型 - 对应 RFC 表18 verify_check_result.check_type
 */
@Getter
@AllArgsConstructor
public enum CheckType {

    SUBJECT("SUBJECT", "主体一致性核验"),
    AMOUNT("AMOUNT", "金额一致性核验"),
    TIME("TIME", "时间逻辑核验"),
    REPEAT("REPEAT", "重复融资核验");

    private final String code;
    private final String desc;

    public static CheckType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown check type: " + code));
    }
}
