package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.dto.UserInfoResponse;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import com.gcrf.library.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private AuthService authService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setUserType("READER");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void testLogin_Success() {
        // Arrange
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("mock-jwt-token");

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals(7200L, response.getExpiresIn());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getUserType(), response.getUserType());

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(jwtUtil).generateToken(eq("1"), anyMap());
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        loginRequest.setPassword("wrongpassword");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(ResultCode.USER_CREDENTIALS_ERROR.getCode(), exception.getCode());
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    void testLogin_UserDisabled() {
        // Arrange
        testUser.setStatus("DISABLED");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateToken_Success() {
        // Arrange
        String token = "valid-token";
        RBucket mockBucket = mock(RBucket.class);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());
        when(mockBucket.isExists()).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(true);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertTrue(result);
        verify(redissonClient).getBucket("auth:blacklist:" + token);
        verify(jwtUtil).validateToken(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateToken_Blacklisted() {
        // Arrange
        String token = "blacklisted-token";
        RBucket mockBucket = mock(RBucket.class);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());
        when(mockBucket.isExists()).thenReturn(true);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertFalse(result);
        verify(redissonClient).getBucket("auth:blacklist:" + token);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateToken_Invalid() {
        // Arrange
        String token = "invalid-token";
        RBucket mockBucket = mock(RBucket.class);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());
        when(mockBucket.isExists()).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // Act
        boolean result = authService.validateToken(token);

        // Assert
        assertFalse(result);
        verify(jwtUtil).validateToken(token);
    }

    @Test
    void testGetUserIdFromToken_Success() {
        // Arrange
        String token = "valid-token";
        Long expectedUserId = 1L;
        when(jwtUtil.getUserId(token)).thenReturn(expectedUserId);

        // Act
        Long userId = authService.getUserIdFromToken(token);

        // Assert
        assertEquals(expectedUserId, userId);
        verify(jwtUtil).getUserId(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLogout_Success() {
        // Arrange
        String token = "valid-token";
        RBucket mockBucket = mock(RBucket.class);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());

        // Act
        authService.logout(token);

        // Assert
        verify(jwtUtil).validateToken(token);
        verify(redissonClient).getBucket("auth:blacklist:" + token);
        verify(mockBucket).set(eq("logged_out"), eq(7200L), any());
    }

    @Test
    void testLogout_InvalidToken() {
        // Arrange
        String token = "invalid-token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.logout(token);
        });

        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
        verify(jwtUtil).validateToken(token);
        verify(redissonClient, never()).getBucket(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRefreshToken_Success() {
        // Arrange
        String oldToken = "old-token";
        String newToken = "new-token";
        RBucket mockBucket = mock(RBucket.class);

        when(jwtUtil.validateToken(oldToken)).thenReturn(true);
        when(jwtUtil.getUserId(oldToken)).thenReturn(1L);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn(newToken);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());

        // Act
        LoginResponse response = authService.refreshToken(oldToken);

        // Assert
        assertNotNull(response);
        assertEquals(newToken, response.getAccessToken());
        assertEquals(testUser.getId(), response.getUserId());
        verify(jwtUtil).validateToken(oldToken);
        verify(redissonClient, times(2)).getBucket("auth:blacklist:" + oldToken);
        verify(mockBucket).set(eq("refreshed"), eq(7200L), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRefreshToken_InvalidToken() {
        // Arrange
        String token = "invalid-token";
        RBucket mockBucket = mock(RBucket.class);
        doReturn(mockBucket).when(redissonClient).getBucket(anyString());
        when(mockBucket.isExists()).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.refreshToken(token);
        });

        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
        verify(jwtUtil).validateToken(token);
    }

    @Test
    void testGetUserInfo_Success() {
        // Arrange
        Long userId = 1L;
        when(userMapper.selectById(userId)).thenReturn(testUser);

        // Act
        UserInfoResponse response = authService.getUserInfo(userId);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUserType(), response.getUserType());
        assertEquals(testUser.getStatus(), response.getStatus());
        verify(userMapper).selectById(userId);
    }

    @Test
    void testGetUserInfo_UserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userMapper.selectById(userId)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.getUserInfo(userId);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectById(userId);
    }
}
