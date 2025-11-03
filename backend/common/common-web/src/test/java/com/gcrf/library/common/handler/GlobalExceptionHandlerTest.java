package com.gcrf.library.common.handler;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleBusinessException() {
        // Arrange
        BusinessException exception = new BusinessException(ResultCode.USER_NOT_FOUND);

        // Act
        Result<?> result = exceptionHandler.handleBusinessException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), result.getCode());
        assertEquals(ResultCode.USER_NOT_FOUND.getMessage(), result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void testHandleBusinessExceptionWithCustomMessage() {
        // Arrange
        String customMessage = "Custom error message";
        BusinessException exception = new BusinessException(5001, customMessage);

        // Act
        Result<?> result = exceptionHandler.handleBusinessException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(5001, result.getCode());
        assertEquals(customMessage, result.getMessage());
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);

        FieldError fieldError1 = new FieldError("user", "name", "姓名不能为空");
        FieldError fieldError2 = new FieldError("user", "email", "邮箱格式不正确");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.Arrays.asList(fieldError1, fieldError2));

        // Act
        Result<?> result = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("姓名不能为空"));
        assertTrue(result.getMessage().contains("邮箱格式不正确"));
    }

    @Test
    void testHandleBindException() {
        // Arrange
        BindException exception = new BindException(new Object(), "testObject");
        exception.addError(new FieldError("user", "age", "年龄必须大于0"));

        // Act
        Result<?> result = exceptionHandler.handleBindException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("年龄必须大于0"));
    }

    @Test
    void testHandleConstraintViolationException() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数验证失败");
        violations.add(violation);

        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // Act
        Result<?> result = exceptionHandler.handleConstraintViolationException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("参数验证失败"));
    }

    @Test
    void testHandleHttpRequestMethodNotSupportedException() {
        // Arrange
        HttpRequestMethodNotSupportedException exception =
            new HttpRequestMethodNotSupportedException("POST");

        // Act
        Result<?> result = exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.METHOD_NOT_ALLOWED.getCode(), result.getCode());
        assertEquals(ResultCode.METHOD_NOT_ALLOWED.getMessage(), result.getMessage());
    }

    @Test
    void testHandleNoHandlerFoundException() {
        // Arrange
        NoHandlerFoundException exception =
            new NoHandlerFoundException("GET", "/api/notfound", null);

        // Act
        Result<?> result = exceptionHandler.handleNoHandlerFoundException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals(ResultCode.NOT_FOUND.getMessage(), result.getMessage());
    }

    @Test
    void testHandleGeneralException() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");

        // Act
        Result<?> result = exceptionHandler.handleException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getMessage(), result.getMessage());
    }

    @Test
    void testHandleNullPointerException() {
        // Arrange
        NullPointerException exception = new NullPointerException("Null value encountered");

        // Act
        Result<?> result = exceptionHandler.handleException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
    }

    @Test
    void testHandleIllegalArgumentException() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // Act
        Result<?> result = exceptionHandler.handleException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
    }

    @Test
    void testMultipleValidationErrors() {
        // Arrange
        BindException exception = new BindException(new Object(), "testObject");
        exception.addError(new FieldError("user", "name", "姓名不能为空"));
        exception.addError(new FieldError("user", "email", "邮箱格式不正确"));
        exception.addError(new FieldError("user", "age", "年龄必须大于0"));

        // Act
        Result<?> result = exceptionHandler.handleBindException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("姓名不能为空"));
        assertTrue(result.getMessage().contains("邮箱格式不正确"));
        assertTrue(result.getMessage().contains("年龄必须大于0"));
    }
}
