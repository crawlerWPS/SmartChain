package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风险等级 - 对应 RFC 表25 risk_profile.risk_level
 */
@Getter
@AllArgsConstructor
public enum RiskLevel {

    LOW("LOW", "低风险"),
    MID("MID", "中风险"),
    HIGH("HIGH", "高风险"),
    EXTREME("EXTREME", "极高风险");

    private final String code;
    private final String desc;

    public static RiskLevel fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown risk level: " + code));
    }

    /** 根据综合评分与阈值判定等级 - 对应 RFC S6-6 */
    public static RiskLevel judgeByScore(double score, int lowThreshold, int midThreshold, int highThreshold) {
        if (score >= lowThreshold) {
            return LOW;
        } else if (score >= midThreshold) {
            return MID;
        } else if (score >= highThreshold) {
            return HIGH;
        } else {
            return EXTREME;
        }
    }
}
