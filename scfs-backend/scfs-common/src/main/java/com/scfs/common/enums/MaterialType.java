package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 材料类型 - 对应 RFC 表16 application_material.material_type
 */
@Getter
@AllArgsConstructor
public enum MaterialType {

    CONTRACT("CONTRACT", "合同"),
    INVOICE("INVOICE", "发票"),
    ORDER("ORDER", "订单"),
    LOGISTICS("LOGISTICS", "物流单据"),
    ACCEPTANCE("ACCEPTANCE", "验收单"),
    PAYMENT("PAYMENT", "付款凭证"),
    QUALIFICATION("QUALIFICATION", "资质文件");

    private final String code;
    private final String desc;

    public static MaterialType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown material type: " + code));
    }
}
