package com.scfs.common.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码定义 - 对应 RFC 3.1 错误码定义表
 *
 * | code | HTTP Status | 说明 |
 * | 0    | 200         | 成功 |
 * | 1001 | 400         | 参数错误 |
 * | 1002 | 401         | 未认证 |
 * | 1003 | 403         | 无权限 |
 * | 1004 | 404         | 资源不存在 |
 * | 1005 | 409         | 状态冲突 |
 * | 1006 | 422         | 业务校验失败 |
 * | 2001 | 500         | OCR 服务异常 |
 * | 2002 | 503         | 外部数据源不可用 |
 * | 9999 | 500         | 未知错误 |
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, 200, "成功"),

    PARAM_ERROR(1001, 400, "参数错误"),
    UNAUTHORIZED(1002, 401, "未认证"),
    PERMISSION_DENIED(1003, 403, "无权限"),
    NOT_FOUND(1004, 404, "资源不存在"),
    STATE_CONFLICT(1005, 409, "状态冲突"),
    BUSINESS_VALIDATION_FAILED(1006, 422, "业务校验失败"),

    OCR_SERVICE_ERROR(2001, 500, "OCR 服务异常"),
    EXTERNAL_DATA_UNAVAILABLE(2002, 503, "外部数据源不可用"),

    DUAL_CONTROL_VIOLATION(1007, 422, "双岗机制违反：经办与复核不能为同一人"),
    RULE_VERSION_OUTDATED(1008, 409, "规则版本已过期"),
    FILE_UPLOAD_REJECTED(1009, 400, "文件上传被拒绝"),
    STATE_MACHINE_ILLEGAL(1010, 409, "状态机非法流转"),
    OPTIMISTIC_LOCK_CONFLICT(1011, 409, "乐观锁冲突，请重试"),

    UNKNOWN_ERROR(9999, 500, "未知错误");

    private final int code;
    private final int httpStatus;
    private final String message;
}
