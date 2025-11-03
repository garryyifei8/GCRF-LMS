package com.gcrf.library.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.auth.dto.ChangePasswordRequest;
import com.gcrf.library.auth.dto.CreateUserRequest;
import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.UpdateUserRequest;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController集成测试
 *
 * 测试覆盖范围：
 * - 创建用户 (3个测试)
 * - 获取用户详情 (2个测试)
 * - 根据用户名获取用户 (2个测试)
 * - 更新用户信息 (3个测试)
 * - 删除用户 (2个测试)
 * - 修改密码 (3个测试)
 * - 分页查询用户列表 (3个测试)
 * - 健康检查 (1个测试)
 *
 * @author GCRF Team
 * @since 2025-10-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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
        // 清理测试数据
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, "testuser001");
        userMapper.delete(queryWrapper);

        // 创建测试用户
        testUser = new User();
        testUser.setUserId("USR_TEST_001");
        testUser.setUsername("testuser001");
        testUser.setPassword(passwordEncoder.encode("Password123"));
        testUser.setEmail("testuser001@test.com");
        testUser.setPhone("13900139001");
        testUser.setUserType("ADMIN"); // 设置为ADMIN以便有权限管理用户
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
        loginRequest.setUsername("testuser001");
        loginRequest.setPassword("Password123");

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
        // 清理测试数据 - 按照精确的用户ID和用户名
        try {
            // 清理主测试用户
            if (testUser != null) {
                LambdaQueryWrapper<User> mainUserQuery = new LambdaQueryWrapper<>();
                mainUserQuery.eq(User::getUsername, "testuser001");
                userMapper.delete(mainUserQuery);
            }

            // 清理创建用户测试数据
            LambdaQueryWrapper<User> newUserQuery = new LambdaQueryWrapper<>();
            newUserQuery.like(User::getUsername, "newuser");
            userMapper.delete(newUserQuery);

            // 清理删除测试用户
            LambdaQueryWrapper<User> deleteUserQuery = new LambdaQueryWrapper<>();
            deleteUserQuery.eq(User::getUsername, "userToDelete")
                    .or()
                    .eq(User::getUserId, "USR_TO_DELETE");
            userMapper.delete(deleteUserQuery);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    // ==================== 创建用户测试 (3个) ====================

    @Test
    void testCreateUser_Success() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser001");
        request.setPassword("NewPass123");
        request.setEmail("newuser001@test.com");
        request.setPhone("13900139002");
        request.setUserType("TEACHER");
        request.setAvatarUrl("https://example.com/avatar.jpg");

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("newuser001"))
                .andExpect(jsonPath("$.data.email").value("newuser001@test.com"))
                .andExpect(jsonPath("$.data.phone").value("13900139002"))
                .andExpect(jsonPath("$.data.userType").value("TEACHER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testCreateUser_InvalidUsername() throws Exception {
        // Arrange - 用户名太短
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("abc"); // 少于4位
        request.setPassword("ValidPass123");
        request.setEmail("test@test.com");
        request.setUserType("STUDENT");

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_InvalidPassword() throws Exception {
        // Arrange - 密码不符合规则（缺少大写字母）
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setPassword("password123"); // 缺少大写字母
        request.setEmail("test@test.com");
        request.setUserType("STUDENT");

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 获取用户详情测试 (2个) ====================

    @Test
    void testGetUserById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.username").value("testuser001"))
                .andExpect(jsonPath("$.data.email").value("testuser001@test.com"))
                .andExpect(jsonPath("$.data.phone").value("13900139001"))
                .andExpect(jsonPath("$.data.userType").value("ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", 999999L)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 根据用户名获取用户测试 (2个) ====================

    @Test
    void testGetUserByUsername_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/username/{username}", "testuser001")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser001"))
                .andExpect(jsonPath("$.data.email").value("testuser001@test.com"))
                .andExpect(jsonPath("$.data.userType").value("ADMIN"));
    }

    @Test
    void testGetUserByUsername_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/username/{username}", "nonexistent")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 更新用户信息测试 (3个) ====================

    @Test
    void testUpdateUser_Success() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@test.com");
        request.setPhone("13900139999");
        request.setAvatarUrl("https://example.com/new-avatar.jpg");
        request.setStatus("INACTIVE");

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"))
                .andExpect(jsonPath("$.data.phone").value("13900139999"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@test.com");

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", 999999L)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testUpdateUser_InvalidEmail() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("invalid-email"); // 无效邮箱格式

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 删除用户测试 (2个) ====================

    @Test
    void testDeleteUser_Success() throws Exception {
        // Arrange - 创建一个用于删除的用户
        User userToDelete = new User();
        userToDelete.setUserId("USR_TO_DELETE");
        userToDelete.setUsername("userToDelete");
        userToDelete.setPassword(passwordEncoder.encode("Password123"));
        userToDelete.setEmail("delete@test.com");
        userToDelete.setPhone("13900139003");
        userToDelete.setUserType("STUDENT");
        userToDelete.setStatus("ACTIVE");
        userToDelete.setCreatedAt(LocalDateTime.now());
        userToDelete.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(userToDelete);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", userToDelete.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify - 用户应该被删除
        User deleted = userMapper.selectById(userToDelete.getId());
        assert deleted == null;
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{userId}", 999999L)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== 修改密码测试 (3个) ====================

    @Test
    void testChangePassword_Success() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("Password123");
        request.setNewPassword("NewPassword456");

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/password", testUser.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify - 验证新密码是否生效
        User updated = userMapper.selectById(testUser.getId());
        assert passwordEncoder.matches("NewPassword456", updated.getPassword());
    }

    @Test
    void testChangePassword_WrongOldPassword() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongPassword123");
        request.setNewPassword("NewPassword456");

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/password", testUser.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testChangePassword_InvalidNewPassword() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("Password123");
        request.setNewPassword("weak"); // 不符合密码规则

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/password", testUser.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 分页查询用户列表测试 (3个) ====================

    @Test
    void testGetUserList_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    void testGetUserList_WithFilters() throws Exception {
        // Act & Assert - 按用户名模糊查询
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "testuser")
                        .param("userType", "ADMIN")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testGetUserList_EmptyResult() throws Exception {
        // Act & Assert - 查询不存在的用户名
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "nonexistent12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== 健康检查测试 (1个) ====================

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("User Service is running"));
    }
}
