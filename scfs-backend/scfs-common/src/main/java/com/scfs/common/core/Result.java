package com.scfs.common.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 全局统一响应体 - 对应 RFC 3.1 通用响应格式
 *
 * <pre>
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": { ... },
 *   "traceId": "uuid-v4"
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;

    private int code;
    private String message;
    private T data;
    private String traceId;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = UUID.randomUUID().toString();
    }

    public static <T> Result<T> success() {
        return new Result<>(CODE_SUCCESS, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CODE_SUCCESS, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }
}
