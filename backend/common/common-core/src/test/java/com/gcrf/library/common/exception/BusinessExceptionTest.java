package com.gcrf.library.common.exception;

import com.gcrf.library.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException类单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class BusinessExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Business error occurred";
        BusinessException exception = new BusinessException(message);

        assertNotNull(exception);
        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithCodeAndMessage() {
        Integer code = 5001;
        String message = "Custom business error";
        BusinessException exception = new BusinessException(code, message);

        assertNotNull(exception);
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithResultCode() {
        BusinessException exception = new BusinessException(ResultCode.USER_NOT_FOUND);

        assertNotNull(exception);
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(ResultCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void testConstructorWithResultCodeAndCustomMessage() {
        String customMessage = "User with ID 123 not found";
        BusinessException exception = new BusinessException(ResultCode.USER_NOT_FOUND, customMessage);

        assertNotNull(exception);
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testExceptionIsRuntimeException() {
        BusinessException exception = new BusinessException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("Test exception");
        });
    }

    @Test
    void testExceptionMessage() {
        String message = "Test message";
        try {
            throw new BusinessException(message);
        } catch (BusinessException e) {
            assertEquals(message, e.getMessage());
            assertEquals(ResultCode.BUSINESS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testCommonBusinessErrors() {
        // 用户不存在
        BusinessException userNotFound = new BusinessException(ResultCode.USER_NOT_FOUND);
        assertEquals(5001, userNotFound.getCode());

        // 用户名或密码错误
        BusinessException credentialsError = new BusinessException(ResultCode.USER_CREDENTIALS_ERROR);
        assertEquals(5002, credentialsError.getCode());

        // 读者证已存在
        BusinessException readerCardExists = new BusinessException(ResultCode.READER_CARD_EXISTS);
        assertEquals(5202, readerCardExists.getCode());

        // 图书不存在
        BusinessException bookNotFound = new BusinessException(ResultCode.BOOK_NOT_FOUND);
        assertEquals(5101, bookNotFound.getCode());
    }
}
