package com.gcrf.library.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.RefreshTokenRequest;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * AuthController集成测试
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        // 清理并创建测试用户
        // 使用WHERE条件删除,避免触发MyBatis-Plus的BlockAttackInnerInterceptor
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, "integrationtest");
        userMapper.delete(queryWrapper);

        testUser = new User();
        testUser.setUserId("INTTEST001"); // 设置业务ID
        testUser.setUsername("integrationtest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("integration@test.com");
        testUser.setPhone("13900139000");
        testUser.setUserType("READER");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        int insertResult = userMapper.insert(testUser);

        // 验证插入成功
        if (insertResult != 1) {
            throw new RuntimeException("Failed to insert test user");
        }

        // 登录获取有效令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("integrationtest");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        validToken = objectMapper.readTree(responseJson)
                .path("data")
                .path("accessToken")
                .asText();
    }

    @AfterEach
    void tearDown() {
        // Clean up test user after each test
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, "integrationtest");
        userMapper.delete(queryWrapper);
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("integrationtest");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("integrationtest"))
                .andExpect(jsonPath("$.data.userType").value("READER"))
                .andExpect(jsonPath("$.data.expiresIn").exists());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("integrationtest");
        request.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testValidateToken_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testValidateToken_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/current-user")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void testGetCurrentUser_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify token is blacklisted
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testLogout_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken(validToken);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").value(not(validToken)))
                .andReturn();

        // Verify old token is blacklisted
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken("invalid-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testGetUserInfo_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/info")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("integrationtest"))
                .andExpect(jsonPath("$.data.email").value("integration@test.com"))
                .andExpect(jsonPath("$.data.userType").value("READER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetUserInfo_NoToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("User Service is running"));
    }
}
