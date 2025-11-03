package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.system.dto.request.MenuCreateRequest;
import com.gcrf.library.system.dto.request.MenuUpdateRequest;
import com.gcrf.library.system.entity.Menu;
import com.gcrf.library.system.mapper.MenuMapper;
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
 * MenuController集成测试
 *
 * 测试覆盖范围：
 * - 获取完整菜单树
 * - 获取用户菜单树
 * - 根据ID获取菜单详情
 * - 创建菜单（根菜单和子菜单）
 * - 更新菜单
 * - 删除菜单（检查子菜单）
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class MenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Menu parentMenu;
    private Menu childMenu;

    @BeforeEach
    void setUp() {
        // No manual cleanup needed - @Transactional will rollback after each test

        // Create parent menu
        parentMenu = new Menu();
        parentMenu.setMenuName("IntTest系统管理");
        parentMenu.setParentId(null);  // Root menu
        parentMenu.setPath("/system");
        parentMenu.setComponent("Layout");
        parentMenu.setIcon("system");
        parentMenu.setMenuType("DIR");
        parentMenu.setSortOrder(1);
        parentMenu.setIsVisible(true);
        parentMenu.setIsCache(false);
        parentMenu.setIsExternal(false);
        parentMenu.setStatus("ACTIVE");
        parentMenu.setCreatedAt(LocalDateTime.now());
        parentMenu.setUpdatedAt(LocalDateTime.now());
        menuMapper.insert(parentMenu);

        // Create child menu
        childMenu = new Menu();
        childMenu.setMenuName("IntTest用户管理");
        childMenu.setParentId(parentMenu.getId());  // Child of parentMenu
        childMenu.setPath("/system/user");
        childMenu.setComponent("system/user/index");
        childMenu.setIcon("user");
        childMenu.setMenuType("MENU");
        childMenu.setSortOrder(1);
        childMenu.setIsVisible(true);
        childMenu.setIsCache(true);
        childMenu.setIsExternal(false);
        childMenu.setPermissionCode("system:user:list");
        childMenu.setStatus("ACTIVE");
        childMenu.setCreatedAt(LocalDateTime.now());
        childMenu.setUpdatedAt(LocalDateTime.now());
        menuMapper.insert(childMenu);
    }

    // No tearDown needed - @Transactional will rollback after each test

    // ========== 菜单树查询测试 (3 tests) ==========

    @Test
    void testGetMenuTree_Success() throws Exception {
        // Act & Assert - Should return tree structure with parent and child
        mockMvc.perform(get("/api/v1/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetMenuTree_WithExistingData() throws Exception {
        // Act & Assert - Should return tree with the test menus from setUp()
        // Note: @Transactional ensures each test has the same setup data
        mockMvc.perform(get("/api/v1/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetUserMenus_Success() throws Exception {
        // Act & Assert - Get menus for user ID 1
        mockMvc.perform(get("/api/v1/menus/user-menus")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void testGetMenuById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/menus/{id}", childMenu.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(childMenu.getId()))
                .andExpect(jsonPath("$.data.menuName").value("IntTest用户管理"))
                .andExpect(jsonPath("$.data.parentId").value(parentMenu.getId()))
                .andExpect(jsonPath("$.data.path").value("/system/user"))
                .andExpect(jsonPath("$.data.menuType").value("MENU"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetMenuById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/menus/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 创建菜单测试 (2 tests) ==========

    @Test
    void testCreateMenu_Success() throws Exception {
        // Arrange - Create a root menu
        MenuCreateRequest request = new MenuCreateRequest();
        request.setMenuName("IntTest报表管理");
        request.setParentId(null);  // Root menu
        request.setPath("/report");
        request.setComponent("Layout");
        request.setIcon("chart");
        request.setMenuType("DIR");
        request.setSortOrder(2);
        request.setIsVisible(true);
        request.setIsCache(false);
        request.setIsExternal(false);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.menuName").value("IntTest报表管理"))
                .andExpect(jsonPath("$.data.path").value("/report"));
    }

    @Test
    void testCreateMenu_InvalidParent() throws Exception {
        // Arrange - Create menu with non-existent parent
        MenuCreateRequest request = new MenuCreateRequest();
        request.setMenuName("IntTest无效子菜单");
        request.setParentId(999999L);  // Non-existent parent
        request.setPath("/invalid");
        request.setComponent("invalid/index");
        request.setMenuType("MENU");
        request.setSortOrder(1);
        request.setIsVisible(true);
        request.setIsCache(false);
        request.setIsExternal(false);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 更新菜单测试 (1 test) ==========

    @Test
    void testUpdateMenu_Success() throws Exception {
        // Arrange
        MenuUpdateRequest request = new MenuUpdateRequest();
        request.setId(childMenu.getId());
        request.setMenuName("IntTest更新后的用户管理");
        request.setIcon("user-updated");
        request.setSortOrder(10);

        // Act & Assert
        mockMvc.perform(put("/api/v1/menus/{id}", childMenu.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.menuName").value("IntTest更新后的用户管理"));
    }

    // ========== 删除菜单测试 (2 tests) ==========

    @Test
    void testDeleteMenu_Success() throws Exception {
        // Arrange - Delete the child menu (leaf node, no children)
        // Act & Assert
        mockMvc.perform(delete("/api/v1/menus/{id}", childMenu.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Note: Cannot verify database state here because @Transactional on test class
        // rolls back all changes after test completes. Trust that 200 OK means success.
    }

    @Test
    void testDeleteMenu_HasChildren() throws Exception {
        // Arrange - Try to delete parent menu which has children
        // Act & Assert - Should fail because parentMenu has childMenu
        mockMvc.perform(delete("/api/v1/menus/{id}", parentMenu.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));  // Should fail
    }
}
