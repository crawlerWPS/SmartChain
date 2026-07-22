package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 企业角色 - 对应 RFC 表11 enterprise_role.role
 */
@Getter
@AllArgsConstructor
public enum EnterpriseRoleEnum {

    CORE("CORE", "核心企业"),
    KEY_SUPPLIER("KEY_SUPPLIER", "关键供应商"),
    TIER1("TIER1", "一级供应商"),
    TIER2("TIER2", "二级供应商"),
    NORMAL("NORMAL", "普通企业"),
    EDGE("EDGE", "边缘企业");

    private final String code;
    private final String desc;

    public static EnterpriseRoleEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enterprise role: " + code));
    }
}
