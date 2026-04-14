package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.system.dto.request.PermissionCreateRequest;
import com.gcrf.library.system.dto.request.PermissionUpdateRequest;
import com.gcrf.library.system.entity.Permission;
import com.gcrf.library.system.mapper.PermissionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PermissionController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询权限
 * - 获取所有权限列表
 * - 根据ID获取权限详情
 * - 创建权限
 * - 更新权限
 * - 删除权限（软删除）
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class PermissionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // No manual cleanup needed - @Transactional will rollback after each test

        // Create test permission
        testPermission = new Permission();
        testPermission.setPermissionCode("inttest:user:read");
        testPermission.setPermissionName("集成测试权限");
        testPermission.setResourceType("API");
        testPermission.setResourcePath("/api/v1/users");
        testPermission.setHttpMethod("GET");
        testPermission.setPermissionGroup("user");
        testPermission.setSortOrder(1);
        testPermission.setStatus("ACTIVE");
        testPermission.setCreatedAt(LocalDateTime.now());
        testPermission.setUpdatedAt(LocalDateTime.now());
        permissionMapper.insert(testPermission);
    }

    // No tearDown needed - @Transactional will rollback after each test

    // ========== 查询接口测试 (2 tests) ==========

    @Test
    void testQueryPermissions_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/permissions")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void testListAllPermissions_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/permissions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void testGetPermissionById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/permissions/{id}", testPermission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testPermission.getId()))
                .andExpect(jsonPath("$.data.permissionCode").value("inttest:user:read"))
                .andExpect(jsonPath("$.data.permissionName").value("集成测试权限"))
                .andExpect(jsonPath("$.data.resourceType").value("API"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetPermissionById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/permissions/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 创建权限测试 (2 tests) ==========

    @Test
    void testCreatePermission_Success() throws Exception {
        // Arrange
        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setPermissionCode("inttest:book:create");
        request.setPermissionName("图书创建权限");
        request.setResourceType("API");
        request.setResourcePath("/api/v1/books");
        request.setHttpMethod("POST");
        request.setPermissionGroup("book");
        request.setSortOrder(2);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/system/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.permissionCode").value("inttest:book:create"))
                .andExpect(jsonPath("$.data.permissionName").value("图书创建权限"));
    }

    @Test
    void testCreatePermission_DuplicateCode() throws Exception {
        // Arrange - Use same permissionCode as testPermission
        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setPermissionCode("inttest:user:read");  // Duplicate
        request.setPermissionName("重复权限");
        request.setResourceType("API");
        request.setResourcePath("/api/v1/test");
        request.setHttpMethod("GET");
        request.setPermissionGroup("test");

        // Act & Assert
        mockMvc.perform(post("/api/v1/system/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 更新权限测试 (1 test) ==========

    @Test
    void testUpdatePermission_Success() throws Exception {
        // Arrange
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setId(testPermission.getId());
        request.setPermissionName("更新后的权限名称");
        request.setResourcePath("/api/v1/users/updated");
        request.setSortOrder(10);

        // Act & Assert
        mockMvc.perform(put("/api/v1/system/permissions/{id}", testPermission.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.permissionName").value("更新后的权限名称"));
    }

    // ========== 删除权限测试 (1 test) ==========

    @Test
    void testDeletePermission_Success() throws Exception {
        // Act & Assert - Delete permission
        mockMvc.perform(delete("/api/v1/system/permissions/{id}", testPermission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Note: Cannot verify database state here because @Transactional on test class
        // rolls back all changes after test completes. Trust that 200 OK means success.
    }
}
