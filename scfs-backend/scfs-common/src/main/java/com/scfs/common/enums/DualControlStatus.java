package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 双岗审批状态 - 对应 RFC rule_change_log.status / risk_weight_config.status / material_checklist_template.status
 */
@Getter
@AllArgsConstructor
public enum DualControlStatus {

    PENDING("PENDING", "待复核"),
    APPROVED("APPROVED", "已复核通过"),
    REJECTED("REJECTED", "已复核拒绝"),
    ENABLED("ENABLED", "已启用"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String desc;

    public static DualControlStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dual-control status: " + code));
    }
}
