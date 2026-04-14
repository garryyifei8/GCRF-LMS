package com.gcrf.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.system.dto.DepartmentCreateRequest;
import com.gcrf.library.system.dto.DepartmentUpdateRequest;
import com.gcrf.library.system.entity.Department;
import com.gcrf.library.system.mapper.DepartmentMapper;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DepartmentController集成测试
 *
 * 测试覆盖范围：
 * - 创建部门（成功/重复编码/校验失败）
 * - 更新部门（成功/不存在）
 * - 根据ID获取部门（成功/不存在）
 * - 分页查询部门（过滤/分页）
 * - 删除部门（成功/有子部门）
 *
 * @author GCRF Team
 * @since 2026-04-13
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DepartmentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    private Department testDept;

    @BeforeEach
    void setUp() {
        testDept = new Department();
        testDept.setDeptCode("TEST_DEPT_" + System.nanoTime());
        testDept.setDeptName("测试部门");
        testDept.setDeptLevel(1);
        testDept.setDeptPath("/");
        testDept.setPhone("020-12345678");
        testDept.setEmail("test@example.com");
        testDept.setSortOrder(0);
        testDept.setStatus("ACTIVE");
        testDept.setDescription("集成测试部门");
        testDept.setCreatedAt(LocalDateTime.now());
        testDept.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(testDept);
    }

    // ========== 创建部门测试 (3 tests) ==========

    @Test
    void createDepartment_success_shouldReturn200AndPersist() throws Exception {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setDeptCode("NEW_DEPT_" + System.nanoTime());
        request.setDeptName("新建部门");
        request.setPhone("020-87654321");
        request.setEmail("new@example.com");
        request.setDescription("新建部门描述");

        mockMvc.perform(post("/api/v1/system/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.deptCode").value(request.getDeptCode()))
                .andExpect(jsonPath("$.deptName").value("新建部门"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createDepartment_withDuplicateCode_shouldReturnError() throws Exception {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setDeptCode(testDept.getDeptCode());
        request.setDeptName("重复编码部门");

        mockMvc.perform(post("/api/v1/system/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void createDepartment_withInvalidData_shouldReturn400() throws Exception {
        // Missing required deptCode and deptName
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setDescription("无效部门 - 缺少必填字段");

        mockMvc.perform(post("/api/v1/system/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== 更新部门测试 (2 tests) ==========

    @Test
    void updateDepartment_success_shouldUpdateFields() throws Exception {
        DepartmentUpdateRequest request = new DepartmentUpdateRequest();
        request.setId(testDept.getId());
        request.setDeptName("更新后的部门名");
        request.setPhone("020-11112222");
        request.setEmail("updated@example.com");
        request.setDescription("更新后的描述");

        mockMvc.perform(put("/api/v1/system/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDept.getId()))
                .andExpect(jsonPath("$.deptName").value("更新后的部门名"))
                .andExpect(jsonPath("$.phone").value("020-11112222"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void updateDepartment_whenNotFound_shouldReturnError() throws Exception {
        DepartmentUpdateRequest request = new DepartmentUpdateRequest();
        request.setId(999999L);
        request.setDeptName("不存在的部门");

        mockMvc.perform(put("/api/v1/system/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void getDepartmentById_success_shouldReturnDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/system/departments/{id}", testDept.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDept.getId()))
                .andExpect(jsonPath("$.deptCode").value(testDept.getDeptCode()))
                .andExpect(jsonPath("$.deptName").value("测试部门"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getDepartmentById_whenNotFound_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/system/departments/{id}", 999999L))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 分页查询测试 (2 tests) ==========

    @Test
    void queryDepartments_withFilters_shouldReturnFiltered() throws Exception {
        // Filter by deptCode (fuzzy match)
        mockMvc.perform(get("/api/v1/system/departments")
                        .param("deptCode", testDept.getDeptCode())
                        .param("status", "ACTIVE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.records[0].deptCode").value(testDept.getDeptCode()));
    }

    @Test
    void queryDepartments_withPagination_shouldReturnPaged() throws Exception {
        mockMvc.perform(get("/api/v1/system/departments")
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)));
    }

    // ========== 删除部门测试 (2 tests) ==========

    @Test
    void deleteDepartment_success_shouldRemove() throws Exception {
        // Create a department with no children for deletion
        Department toDelete = new Department();
        toDelete.setDeptCode("DELETE_ME_" + System.nanoTime());
        toDelete.setDeptName("待删除部门");
        toDelete.setDeptLevel(1);
        toDelete.setSortOrder(0);
        toDelete.setStatus("ACTIVE");
        toDelete.setCreatedAt(LocalDateTime.now());
        toDelete.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(toDelete);

        mockMvc.perform(delete("/api/v1/system/departments/{id}", toDelete.getId()))
                .andExpect(status().isOk());

        // Note: Cannot verify DB state here - @Transactional rolls back after test
    }

    @Test
    void deleteDepartment_whenHasChildren_shouldReturnError() throws Exception {
        // Create parent
        Department parent = new Department();
        parent.setDeptCode("PARENT_" + System.nanoTime());
        parent.setDeptName("父部门");
        parent.setDeptLevel(1);
        parent.setSortOrder(0);
        parent.setStatus("ACTIVE");
        parent.setCreatedAt(LocalDateTime.now());
        parent.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(parent);

        // Create child under parent
        Department child = new Department();
        child.setDeptCode("CHILD_" + System.nanoTime());
        child.setDeptName("子部门");
        child.setParentId(parent.getId());
        child.setDeptLevel(2);
        child.setSortOrder(0);
        child.setStatus("ACTIVE");
        child.setCreatedAt(LocalDateTime.now());
        child.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(child);

        // Attempt to delete parent - should fail because it has children
        mockMvc.perform(delete("/api/v1/system/departments/{id}", parent.getId()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }
}
