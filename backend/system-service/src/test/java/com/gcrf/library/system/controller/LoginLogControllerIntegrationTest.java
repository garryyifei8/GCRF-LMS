package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.system.entity.LoginLog;
import com.gcrf.library.system.mapper.LoginLogMapper;
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
 * LoginLogController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询登录日志
 * - 按用户ID、登录类型、状态过滤
 * - 按日期范围过滤
 * - 按用户名过滤
 * - 按登录状态过滤
 * - 分页参数测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // Rollback after each test
class LoginLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginLogMapper loginLogMapper;

    private LoginLog successLog;
    private LoginLog failureLog;

    @BeforeEach
    void setUp() {
        // No manual cleanup needed - @Transactional will rollback after each test

        // Create successful login log
        successLog = new LoginLog();
        successLog.setUserId(2001L);
        successLog.setUsername("inttest_admin");
        successLog.setDeptName("管理部门");
        successLog.setLoginType("WEB");
        successLog.setLoginMethod("PASSWORD");
        successLog.setIpAddress("192.168.1.200");
        successLog.setLocation("北京");
        successLog.setBrowser("Chrome 120");
        successLog.setOs("Windows 10");
        successLog.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        successLog.setStatus("SUCCESS");
        successLog.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test");
        successLog.setSessionId("session_001");
        successLog.setCreatedAt(LocalDateTime.now().minusDays(1));
        loginLogMapper.insert(successLog);

        // Create failed login log
        failureLog = new LoginLog();
        failureLog.setUserId(null);  // Failed login, no user ID
        failureLog.setUsername("inttest_hacker");
        failureLog.setDeptName(null);
        failureLog.setLoginType("WEB");
        failureLog.setLoginMethod("PASSWORD");
        failureLog.setIpAddress("192.168.1.201");
        failureLog.setLocation("未知");
        failureLog.setBrowser("Firefox 121");
        failureLog.setOs("Linux");
        failureLog.setUserAgent("Mozilla/5.0 (X11; Linux x86_64)");
        failureLog.setStatus("FAILURE");
        failureLog.setErrorMsg("用户名或密码错误");
        failureLog.setToken(null);
        failureLog.setSessionId(null);
        failureLog.setCreatedAt(LocalDateTime.now().minusHours(2));
        loginLogMapper.insert(failureLog);
    }

    // No tearDown needed - @Transactional will rollback after each test

    // ========== 查询接口测试 (6 tests) ==========

    @Test
    void testQueryLogs_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/login-logs")
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
        // Act & Assert - Filter by userId, loginType, and status
        mockMvc.perform(get("/api/v1/login-logs")
                        .param("userId", "2001")
                        .param("loginType", "WEB")
                        .param("status", "SUCCESS")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testQueryLogs_DateRange() throws Exception {
        // Arrange - Date range for last 2 days
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Act & Assert - Filter by date range
        mockMvc.perform(get("/api/v1/login-logs")
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
        mockMvc.perform(get("/api/v1/login-logs")
                        .param("username", "inttest_admin")
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
        mockMvc.perform(get("/api/v1/login-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1));
    }

    @Test
    void testQueryLogs_StatusFilter() throws Exception {
        // Act & Assert - Filter by FAILURE status
        mockMvc.perform(get("/api/v1/login-logs")
                        .param("status", "FAILURE")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }
}
