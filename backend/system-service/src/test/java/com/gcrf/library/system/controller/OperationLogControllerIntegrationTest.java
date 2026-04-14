package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.system.entity.OperationLog;
import com.gcrf.library.system.mapper.OperationLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OperationLogController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询操作日志
 * - 按用户ID过滤
 * - 按操作类型过滤
 * - 按日期范围过滤
 * - 按用户名过滤
 * - 分页参数测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class OperationLogControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OperationLogMapper operationLogMapper;

    private OperationLog testLog1;
    private OperationLog testLog2;

    @BeforeEach
    void setUp() {
        // No manual cleanup needed - @Transactional will rollback after each test

        // Create test operation log 1 (CREATE operation)
        testLog1 = new OperationLog();
        testLog1.setUserId(1001L);
        testLog1.setUsername("inttest_user1");
        testLog1.setDeptName("测试部门");
        testLog1.setOperation("创建角色");
        testLog1.setOperationType("CREATE");
        testLog1.setBusinessType("ROLE");
        testLog1.setRequestMethod("RoleController.createRole");
        testLog1.setRequestUrl("/api/v1/system/roles");
        testLog1.setHttpMethod("POST");
        testLog1.setRequestParams("{\"roleCode\":\"TEST_ROLE\"}");
        testLog1.setResponseResult("{\"code\":200}");
        testLog1.setStatus("SUCCESS");
        testLog1.setIpAddress("192.168.1.100");
        testLog1.setLocation("北京");
        testLog1.setUserAgent("Mozilla/5.0");
        testLog1.setOsInfo("Windows 10");
        testLog1.setBrowserInfo("Chrome 120");
        testLog1.setExecutionTime(125);
        testLog1.setCreatedAt(LocalDateTime.now().minusDays(2));
        operationLogMapper.insert(testLog1);

        // Create test operation log 2 (UPDATE operation)
        testLog2 = new OperationLog();
        testLog2.setUserId(1002L);
        testLog2.setUsername("inttest_user2");
        testLog2.setDeptName("开发部门");
        testLog2.setOperation("更新权限");
        testLog2.setOperationType("UPDATE");
        testLog2.setBusinessType("PERMISSION");
        testLog2.setRequestMethod("PermissionController.updatePermission");
        testLog2.setRequestUrl("/api/v1/system/permissions/1");
        testLog2.setHttpMethod("PUT");
        testLog2.setRequestParams("{\"permissionName\":\"Updated\"}");
        testLog2.setResponseResult("{\"code\":200}");
        testLog2.setStatus("SUCCESS");
        testLog2.setIpAddress("192.168.1.101");
        testLog2.setLocation("上海");
        testLog2.setUserAgent("Mozilla/5.0");
        testLog2.setOsInfo("macOS");
        testLog2.setBrowserInfo("Safari 17");
        testLog2.setExecutionTime(85);
        testLog2.setCreatedAt(LocalDateTime.now().minusDays(1));
        operationLogMapper.insert(testLog2);
    }

    // No tearDown needed - @Transactional will rollback after each test

    // ========== 查询接口测试 (6 tests) ==========

    @Test
    void testQueryLogs_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void testQueryLogs_WithFilters() throws Exception {
        // Act & Assert - Filter by userId and operationType
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("userId", "1001")
                        .param("operationType", "CREATE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testQueryLogs_DateRange() throws Exception {
        // Arrange - Date range for last 3 days
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Act & Assert - Filter by date range
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("createdStart", startDate.format(formatter))
                        .param("createdEnd", endDate.format(formatter))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void testQueryLogs_ByUsername() throws Exception {
        // Act & Assert - Filter by username
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("username", "inttest_user1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testQueryLogs_Pagination() throws Exception {
        // Act & Assert - Test pagination with pageSize=1
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1));
    }

    @Test
    void testQueryLogs_EmptyResult() throws Exception {
        // Act & Assert - Query with non-matching filter
        mockMvc.perform(get("/api/v1/system/operation-logs")
                        .param("username", "nonexistent_user")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
