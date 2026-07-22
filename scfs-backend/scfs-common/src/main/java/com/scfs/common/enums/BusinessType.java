package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 业务类型 - 对应 RFC 表14 financing_application.business_type
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    AR_FINANCING("AR_FINANCING", "应收账款融资"),
    FACTORING("FACTORING", "保理"),
    ORDER_FINANCING("ORDER_FINANCING", "订单融资");

    private final String code;
    private final String desc;

    public static BusinessType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown business type: " + code));
    }
}
