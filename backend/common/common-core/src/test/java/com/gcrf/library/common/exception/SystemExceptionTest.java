package com.gcrf.library.common.exception;

import com.gcrf.library.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemException类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class SystemExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Database connection failed";
        SystemException exception = new SystemException(message);

        assertNotNull(exception);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithCodeAndMessage() {
        Integer code = 5500;
        String message = "Redis connection timeout";
        SystemException exception = new SystemException(code, message);

        assertNotNull(exception);
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithResultCode() {
        SystemException exception = new SystemException(ResultCode.SERVICE_UNAVAILABLE);

        assertNotNull(exception);
        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getMessage(), exception.getMessage());
    }

    @Test
    void testConstructorWithResultCodeAndCustomMessage() {
        String customMessage = "Nacos service unavailable";
        SystemException exception = new SystemException(ResultCode.SERVICE_UNAVAILABLE, customMessage);

        assertNotNull(exception);
        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Database error";
        Throwable cause = new RuntimeException("Connection timeout");
        SystemException exception = new SystemException(message, cause);

        assertNotNull(exception);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCodeMessageAndCause() {
        Integer code = 5500;
        String message = "Redis error";
        Throwable cause = new RuntimeException("Connection refused");
        SystemException exception = new SystemException(code, message, cause);

        assertNotNull(exception);
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithResultCodeAndCause() {
        Throwable cause = new RuntimeException("Gateway timeout");
        SystemException exception = new SystemException(ResultCode.GATEWAY_TIMEOUT, cause);

        assertNotNull(exception);
        assertEquals(ResultCode.GATEWAY_TIMEOUT.getCode(), exception.getCode());
        assertEquals(ResultCode.GATEWAY_TIMEOUT.getMessage(), exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithResultCodeMessageAndCause() {
        String customMessage = "Custom gateway error";
        Throwable cause = new RuntimeException("Timeout");
        SystemException exception = new SystemException(ResultCode.GATEWAY_TIMEOUT, customMessage, cause);

        assertNotNull(exception);
        assertEquals(ResultCode.GATEWAY_TIMEOUT.getCode(), exception.getCode());
        assertEquals(customMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionIsRuntimeException() {
        SystemException exception = new SystemException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(SystemException.class, () -> {
            throw new SystemException("Test system exception");
        });
    }

    @Test
    void testCausePreserved() {
        RuntimeException originalException = new RuntimeException("Original error");
        SystemException systemException = new SystemException("Wrapped error", originalException);

        assertNotNull(systemException.getCause());
        assertEquals(originalException, systemException.getCause());
        assertEquals("Original error", systemException.getCause().getMessage());
    }

    @Test
    void testCommonSystemErrors() {
        // 服务不可用
        SystemException serviceUnavailable = new SystemException(ResultCode.SERVICE_UNAVAILABLE);
        assertEquals(503, serviceUnavailable.getCode());

        // 网关超时
        SystemException gatewayTimeout = new SystemException(ResultCode.GATEWAY_TIMEOUT);
        assertEquals(504, gatewayTimeout.getCode());

        // 内部服务器错误
        SystemException internalError = new SystemException(ResultCode.INTERNAL_SERVER_ERROR);
        assertEquals(500, internalError.getCode());
    }

    @Test
    void testStackTracePreserved() {
        try {
            throwNestedSystemException();
            fail("Exception should have been thrown");
        } catch (SystemException e) {
            assertNotNull(e.getStackTrace());
            assertTrue(e.getStackTrace().length > 0);
            assertTrue(e.getStackTrace()[0].getMethodName().contains("throwNestedSystemException"));
        }
    }

    private void throwNestedSystemException() {
        throw new SystemException("Nested system exception");
    }
}
