package com.scfs.common.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一响应体单元测试
 */
@DisplayName("统一响应体 Result 测试")
class ResultTest {

    @Nested
    @DisplayName("success 工厂方法")
    class SuccessFactory {

        @Test
        @DisplayName("success() 应返回 code=0, message=success, data=null")
        void success_noData() {
            Result<Void> result = Result.success();

            assertEquals(Result.CODE_SUCCESS, result.getCode());
            assertEquals("success", result.getMessage());
            assertNull(result.getData());
            assertNotNull(result.getTraceId());
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("success(data) 应携带 data")
        void success_withData() {
            Result<String> result = Result.success("hello");

            assertEquals(0, result.getCode());
            assertEquals("success", result.getMessage());
            assertEquals("hello", result.getData());
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("success(message, data) 应使用自定义 message")
        void success_withMessageAndData() {
            Result<Integer> result = Result.success("操作成功", 42);

            assertEquals(0, result.getCode());
            assertEquals("操作成功", result.getMessage());
            assertEquals(42, result.getData());
        }

        @Test
        @DisplayName("每次构造应生成不同的 traceId")
        void traceId_shouldBeUnique() {
            Result<Void> r1 = Result.success();
            Result<Void> r2 = Result.success();

            assertNotNull(r1.getTraceId());
            assertNotNull(r2.getTraceId());
            assertNotEquals(r1.getTraceId(), r2.getTraceId());
        }
    }

    @Nested
    @DisplayName("fail 工厂方法")
    class FailFactory {

        @Test
        @DisplayName("fail(code, message) 应返回错误码和消息")
        void fail_withCodeAndMessage() {
            Result<Void> result = Result.fail(1001, "参数错误");

            assertEquals(1001, result.getCode());
            assertEquals("参数错误", result.getMessage());
            assertNull(result.getData());
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("fail(ErrorCode) 应从错误码枚举构造")
        void fail_withErrorCode() {
            Result<Void> result = Result.fail(ErrorCode.UNAUTHORIZED);

            assertEquals(1002, result.getCode());
            assertEquals("未认证", result.getMessage());
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("fail(ErrorCode, message) 应使用自定义消息覆盖枚举消息")
        void fail_withErrorCodeAndCustomMessage() {
            Result<Void> result = Result.fail(ErrorCode.PARAM_ERROR, "用户名不能为空");

            assertEquals(1001, result.getCode());
            assertEquals("用户名不能为空", result.getMessage());
        }
    }

    @Nested
    @DisplayName("isSuccess 判断")
    class IsSuccess {

        @Test
        @DisplayName("code=0 时 isSuccess 为 true")
        void isSuccess_trueWhenCodeIsZero() {
            assertTrue(Result.success().isSuccess());
            assertTrue(Result.success("data").isSuccess());
        }

        @Test
        @DisplayName("code!=0 时 isSuccess 为 false")
        void isSuccess_falseWhenCodeIsNotZero() {
            assertFalse(Result.fail(1, "fail").isSuccess());
            assertFalse(Result.fail(ErrorCode.UNKNOWN_ERROR).isSuccess());
        }
    }

    @Test
    @DisplayName("ErrorCode 枚举应包含所有预定义错误码")
    void errorCode_shouldContainAllPredefinedCodes() {
        assertEquals(0, ErrorCode.SUCCESS.getCode());
        assertEquals(1001, ErrorCode.PARAM_ERROR.getCode());
        assertEquals(1002, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals(1003, ErrorCode.PERMISSION_DENIED.getCode());
        assertEquals(1004, ErrorCode.NOT_FOUND.getCode());
        assertEquals(9999, ErrorCode.UNKNOWN_ERROR.getCode());

        assertEquals(400, ErrorCode.PARAM_ERROR.getHttpStatus());
        assertEquals(401, ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertEquals(500, ErrorCode.UNKNOWN_ERROR.getHttpStatus());
    }
}
