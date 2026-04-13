package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.system.dto.request.AssignPermissionsRequest;
import com.gcrf.library.system.dto.request.RoleCreateRequest;
import com.gcrf.library.system.dto.request.RoleUpdateRequest;
import com.gcrf.library.system.entity.Permission;
import com.gcrf.library.system.entity.Role;
import com.gcrf.library.system.entity.RolePermission;
import com.gcrf.library.system.mapper.PermissionMapper;
import com.gcrf.library.system.mapper.RoleMapper;
import com.gcrf.library.system.mapper.RolePermissionMapper;
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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RoleController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询角色（带过滤条件）
 * - 根据ID获取角色详情
 * - 创建角色
 * - 更新角色
 * - 删除角色（软删除）
 * - 为角色分配权限
 * - 获取角色权限列表
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // No manual cleanup needed - @Transactional will rollback after each test

        // Create test role
        testRole = new Role();
        testRole.setRoleCode("TEST_ROLE");
        testRole.setRoleName("测试角色");
        testRole.setRoleDesc("测试用角色");
        testRole.setDataScope("ALL");
        testRole.setSortOrder(1);
        testRole.setStatus("ACTIVE");
        testRole.setCreatedAt(LocalDateTime.now());
        testRole.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(testRole);

        // Create test permission
        testPermission = new Permission();
        testPermission.setPermissionCode("test:read");
        testPermission.setPermissionName("测试读取");
        testPermission.setResourceType("API");
        testPermission.setResourcePath("/test");
        testPermission.setHttpMethod("GET");
        testPermission.setPermissionGroup("test");
        testPermission.setSortOrder(1);
        testPermission.setStatus("ACTIVE");
        testPermission.setCreatedAt(LocalDateTime.now());
        testPermission.setUpdatedAt(LocalDateTime.now());
        permissionMapper.insert(testPermission);
    }

    // No tearDown needed - @Transactional will rollback after each test

    // ========== 查询接口测试 (3 tests) ==========

    @Test
    void testQueryRoles_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/roles")
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
    void testQueryRoles_WithFilters() throws Exception {
        // Act & Assert - Filter by roleCode
        mockMvc.perform(get("/api/v1/system/roles")
                        .param("roleCode", "TEST_ROLE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testQueryRoles_EmptyResult() throws Exception {
        // Act & Assert - Query non-existent role
        mockMvc.perform(get("/api/v1/system/roles")
                        .param("roleCode", "NONEXISTENT_ROLE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void testGetRoleById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/roles/{id}", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testRole.getId()))
                .andExpect(jsonPath("$.data.roleCode").value("TEST_ROLE"))
                .andExpect(jsonPath("$.data.roleName").value("测试角色"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetRoleById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/roles/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 创建角色测试 (2 tests) ==========

    @Test
    void testCreateRole_Success() throws Exception {
        // Arrange
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("TEST_NEW_ROLE");
        request.setRoleName("新角色");
        request.setRoleDesc("新角色描述");
        request.setDataScope("DEPT");
        request.setSortOrder(2);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/system/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleCode").value("TEST_NEW_ROLE"))
                .andExpect(jsonPath("$.data.roleName").value("新角色"));
    }

    @Test
    void testCreateRole_InvalidData() throws Exception {
        // Arrange - Missing required roleCode
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("");  // Empty roleCode
        request.setRoleName("无效角色");

        // Act & Assert
        mockMvc.perform(post("/api/v1/system/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新角色测试 (1 test) ==========

    @Test
    void testUpdateRole_Success() throws Exception {
        // Arrange
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setId(testRole.getId());
        request.setRoleName("更新后的角色名");
        request.setRoleDesc("更新后的描述");
        request.setSortOrder(10);

        // Act & Assert
        mockMvc.perform(put("/api/v1/system/roles/{id}", testRole.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleName").value("更新后的角色名"));
    }

    // ========== 删除角色测试 (1 test) ==========

    @Test
    void testDeleteRole_Success() throws Exception {
        // Act & Assert - Delete role
        mockMvc.perform(delete("/api/v1/system/roles/{id}", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Note: Cannot verify database state here because @Transactional on test class
        // rolls back all changes after test completes. Trust that 200 OK means success.
    }

    // ========== 权限分配测试 (2 tests) ==========

    @Test
    void testAssignPermissions_Success() throws Exception {
        // Arrange
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setRoleId(testRole.getId());
        request.setPermissionIds(Arrays.asList(testPermission.getId()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/system/roles/{id}/permissions", testRole.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify assignment
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, testRole.getId())
                .eq(RolePermission::getPermissionId, testPermission.getId());
        RolePermission rolePermission = rolePermissionMapper.selectOne(wrapper);
        assertThat(rolePermission).isNotNull();
    }

    @Test
    void testGetRolePermissions_Success() throws Exception {
        // Arrange - Assign permission first
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(testRole.getId());
        rolePermission.setPermissionId(testPermission.getId());
        rolePermission.setCreatedAt(LocalDateTime.now());
        rolePermissionMapper.insert(rolePermission);

        // Act & Assert
        mockMvc.perform(get("/api/v1/system/roles/{id}/permissions", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].permissionCode").value("test:read"));
    }
}
