package com.gcrf.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.LoginLogQueryRequest;
import com.gcrf.library.system.dto.response.LoginLogVO;
import com.gcrf.library.system.entity.LoginLog;
import com.gcrf.library.system.mapper.LoginLogMapper;
import com.gcrf.library.system.service.impl.LoginLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LoginLogService单元测试
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceTest {

    @Mock
    private LoginLogMapper loginLogMapper;

    @InjectMocks
    private LoginLogServiceImpl loginLogService;

    private LoginLog testLog;
    private LoginLogQueryRequest queryRequest;

    @BeforeEach
    void setUp() {
        testLog = new LoginLog();
        testLog.setId(1L);
        testLog.setUserId(100L);
        testLog.setUsername("admin");
        testLog.setDeptName("技术部");
        testLog.setLoginType("WEB");
        testLog.setLoginMethod("PASSWORD");
        testLog.setIpAddress("192.168.1.100");
        testLog.setLocation("北京市");
        testLog.setBrowser("Chrome");
        testLog.setOs("Windows 10");
        testLog.setStatus("SUCCESS");
        testLog.setCreatedAt(LocalDateTime.now());

        queryRequest = new LoginLogQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
    }

    @Test
    void testQueryLogs_Success() {
        // Given
        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void testQueryLogs_WithFilters() {
        // Given
        queryRequest.setUserId(100L);
        queryRequest.setUsername("admin");
        queryRequest.setLoginType("WEB");
        queryRequest.setStatus("SUCCESS");

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        verify(loginLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryLogs_DateRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        queryRequest.setStartTime(startTime);
        queryRequest.setEndTime(endTime);

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isNotEmpty();
    }

    @Test
    void testQueryLogs_ByUsername() {
        // Given
        queryRequest.setUsername("admin");

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> log.getUsername().contains("admin"));
    }

    @Test
    void testQueryLogs_ByStatus() {
        // Given
        queryRequest.setStatus("SUCCESS");

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> "SUCCESS".equals(log.getStatus()));
    }

    @Test
    void testQueryLogs_ByLoginType() {
        // Given
        queryRequest.setLoginType("WEB");

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> "WEB".equals(log.getLoginType()));
    }

    @Test
    void testRecordLogin_Success() {
        // Given
        when(loginLogMapper.insert(any(LoginLog.class)))
            .thenReturn(1);

        // When
        loginLogService.recordLogin(testLog);

        // Then
        verify(loginLogMapper).insert(testLog);
    }

    @Test
    void testQueryLogs_Pagination() {
        // Given
        queryRequest.setPageNum(2);
        queryRequest.setPageSize(5);

        Page<LoginLog> page = new Page<>();
        page.setRecords(List.of());
        page.setTotal(10);
        page.setCurrent(2);
        page.setSize(5);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotal()).isEqualTo(10);
    }

    @Test
    void testQueryLogs_Sorting() {
        // Given
        LoginLog log1 = new LoginLog();
        log1.setId(1L);
        log1.setUsername("user1");
        log1.setCreatedAt(LocalDateTime.now().minusHours(2));

        LoginLog log2 = new LoginLog();
        log2.setId(2L);
        log2.setUsername("user2");
        log2.setCreatedAt(LocalDateTime.now().minusHours(1));

        Page<LoginLog> page = new Page<>();
        page.setRecords(Arrays.asList(log2, log1)); // 按时间倒序
        page.setTotal(2);

        when(loginLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<LoginLogVO> result = loginLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(2L); // 最新的在前
    }
}
