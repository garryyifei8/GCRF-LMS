package com.gcrf.library.common.result;

import com.gcrf.library.common.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class ResultTest {

    @Test
    void testSuccessWithData() {
        String data = "test data";
        Result<String> result = Result.success(data);

        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals(data, result.getData());
        assertNotNull(result.getTimestamp());
        assertTrue(result.isSuccess());
    }

    @Test
    void testSuccessWithoutData() {
        Result<Object> result = Result.success();

        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertNull(result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    void testSuccessWithCustomMessage() {
        String message = "Custom success message";
        String data = "test data";
        Result<String> result = Result.success(message, data);

        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    void testErrorDefault() {
        Result<Object> result = Result.error();

        assertNotNull(result);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getMessage(), result.getMessage());
        assertNull(result.getData());
        assertFalse(result.isSuccess());
    }

    @Test
    void testErrorWithMessage() {
        String message = "Custom error message";
        Result<Object> result = Result.error(message);

        assertNotNull(result);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(message, result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void testErrorWithResultCode() {
        Result<Object> result = Result.error(ResultCode.PARAM_ERROR);

        assertNotNull(result);
        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertEquals(ResultCode.PARAM_ERROR.getMessage(), result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        Integer code = 5000;
        String message = "Business error";
        Result<Object> result = Result.error(code, message);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(message, result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void testBuildCustomResult() {
        Integer code = 2000;
        String message = "Custom result";
        String data = "custom data";
        Result<String> result = Result.build(code, message, data);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    void testIsSuccess() {
        Result<Object> successResult = Result.success();
        assertTrue(successResult.isSuccess());

        Result<Object> errorResult = Result.error();
        assertFalse(errorResult.isSuccess());
    }

    @Test
    void testJsonSerialization() {
        Result<String> result = Result.success("test");
        String json = JsonUtil.toJson(result);

        assertNotNull(json);
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"data\":\"test\""));
    }

    @Test
    void testJsonDeserialization() {
        String json = "{\"code\":200,\"message\":\"操作成功\",\"data\":\"test\",\"timestamp\":1234567890}";
        Result<?> result = JsonUtil.parseObject(json, Result.class);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNotNull(result.getData());
    }

    @Test
    void testTimestampNotNull() {
        Result<Object> result = Result.success();
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void testResultCodeEnum() {
        assertEquals(200, ResultCode.SUCCESS.getCode());
        assertEquals("操作成功", ResultCode.SUCCESS.getMessage());

        assertEquals(400, ResultCode.PARAM_ERROR.getCode());
        assertEquals(401, ResultCode.UNAUTHORIZED.getCode());
        assertEquals(403, ResultCode.FORBIDDEN.getCode());
        assertEquals(404, ResultCode.NOT_FOUND.getCode());
        assertEquals(500, ResultCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
