package com.scfs.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 融资申请状态机 - 对应 RFC 表14 financing_application.status
 *
 * <p>9 状态：DRAFT → SUBMITTED → PRE_AUDITING → PRE_AUDIT_PASSED → VERIFYING
 *         → VERIFY_PASSED → RISK_SCORING → PENDING_DECISION → APPROVED / REJECTED</p>
 *
 * <p>合法流转详见 {@link #canTransitTo(ApplicationStatus)}</p>
 *
 * <p>对应 RFC S8-6 状态机测试</p>
 */
@Getter
@AllArgsConstructor
public enum ApplicationStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    PRE_AUDITING("PRE_AUDITING", "预审中"),
    PRE_AUDIT_PASSED("PRE_AUDIT_PASSED", "预审通过"),
    PRE_AUDIT_FAILED("PRE_AUDIT_FAILED", "预审失败"),
    VERIFYING("VERIFYING", "核验中"),
    VERIFY_PASSED("VERIFY_PASSED", "核验通过"),
    VERIFY_FAILED("VERIFY_FAILED", "核验失败"),
    RISK_SCORING("RISK_SCORING", "风险评分中"),
    PENDING_DECISION("PENDING_DECISION", "等待人工决策"),
    APPROVED("APPROVED", "审核通过"),
    REJECTED("REJECTED", "审核拒绝"),
    APPROVED_REVOKED("APPROVED_REVOKED", "通过后撤销"),
    REJECTED_REVOKED("REJECTED_REVOKED", "拒绝后撤销");

    private final String code;
    private final String desc;

    public static ApplicationStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown application status: " + code));
    }

    /** 判断是否为终态（不可再变更） */
    public boolean isFinalState() {
        return this == APPROVED || this == REJECTED
                || this == APPROVED_REVOKED || this == REJECTED_REVOKED;
    }

    /**
     * 校验状态流转合法性 - 对应 RFC S8-6 状态机测试
     *
     * <p>终态不可再流转：对应 RFC 硬约束 "Final states (APPROVED/REJECTED) cannot be reopened"</p>
     */
    public boolean canTransitTo(ApplicationStatus target) {
        if (this.isFinalState()) {
            return false;
        }
        return allowedTransitions().contains(target);
    }

    private Set<ApplicationStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> EnumSet.of(SUBMITTED);
            case SUBMITTED -> EnumSet.of(PRE_AUDITING, PENDING_DECISION, APPROVED, REJECTED);
            case PRE_AUDITING -> EnumSet.of(PRE_AUDIT_PASSED, PRE_AUDIT_FAILED, PENDING_DECISION, APPROVED);
            case PRE_AUDIT_PASSED -> EnumSet.of(VERIFYING, PENDING_DECISION, APPROVED);
            case PRE_AUDIT_FAILED -> EnumSet.of(SUBMITTED); // 重新提交补正
            case VERIFYING -> EnumSet.of(VERIFY_PASSED, VERIFY_FAILED, PENDING_DECISION, APPROVED);
            case VERIFY_PASSED -> EnumSet.of(RISK_SCORING, PENDING_DECISION, APPROVED);
            case VERIFY_FAILED -> EnumSet.of(SUBMITTED); // 重新提交补正
            case RISK_SCORING -> EnumSet.of(PENDING_DECISION, APPROVED);
            case PENDING_DECISION -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(APPROVED_REVOKED);
            case REJECTED -> EnumSet.of(REJECTED_REVOKED);
            default -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }
}
