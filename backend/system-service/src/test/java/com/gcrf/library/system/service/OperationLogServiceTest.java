package com.gcrf.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.OperationLogQueryRequest;
import com.gcrf.library.system.dto.response.OperationLogVO;
import com.gcrf.library.system.entity.OperationLog;
import com.gcrf.library.system.mapper.OperationLogMapper;
import com.gcrf.library.system.service.impl.OperationLogServiceImpl;
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
 * OperationLogService单元测试
 */
@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    private OperationLog testLog;
    private OperationLogQueryRequest queryRequest;

    @BeforeEach
    void setUp() {
        testLog = new OperationLog();
        testLog.setId(1L);
        testLog.setUserId(100L);
        testLog.setUsername("admin");
        testLog.setDeptName("技术部");
        testLog.setOperation("创建用户");
        testLog.setOperationType("CREATE");
        testLog.setBusinessType("USER");
        testLog.setRequestMethod("UserController.createUser");
        testLog.setRequestUrl("/api/v1/users");
        testLog.setHttpMethod("POST");
        testLog.setStatus("SUCCESS");
        testLog.setIpAddress("192.168.1.100");
        testLog.setExecutionTime(120);
        testLog.setCreatedAt(LocalDateTime.now());

        queryRequest = new OperationLogQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
    }

    @Test
    void testQueryLogs_Success() {
        // Given
        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

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
        queryRequest.setOperationType("CREATE");
        queryRequest.setStatus("SUCCESS");

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        verify(operationLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryLogs_DateRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        queryRequest.setStartTime(startTime);
        queryRequest.setEndTime(endTime);

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isNotEmpty();
    }

    @Test
    void testQueryLogs_ByUsername() {
        // Given
        queryRequest.setUsername("admin");

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> log.getUsername().contains("admin"));
    }

    @Test
    void testQueryLogs_ByOperationType() {
        // Given
        queryRequest.setOperationType("CREATE");

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> "CREATE".equals(log.getOperationType()));
    }

    @Test
    void testQueryLogs_ByStatus() {
        // Given
        queryRequest.setStatus("SUCCESS");

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(testLog));
        page.setTotal(1);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).allMatch(log -> "SUCCESS".equals(log.getStatus()));
    }

    @Test
    void testCreateLog_Success() {
        // Given
        when(operationLogMapper.insert(any(OperationLog.class)))
            .thenReturn(1);

        // When
        operationLogService.createLog(testLog);

        // Then
        verify(operationLogMapper).insert(testLog);
    }

    @Test
    void testQueryLogs_Pagination() {
        // Given
        queryRequest.setPageNum(2);
        queryRequest.setPageSize(5);

        Page<OperationLog> page = new Page<>();
        page.setRecords(List.of());
        page.setTotal(10);
        page.setCurrent(2);
        page.setSize(5);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotal()).isEqualTo(10);
    }

    @Test
    void testQueryLogs_Sorting() {
        // Given
        OperationLog log1 = new OperationLog();
        log1.setId(1L);
        log1.setUsername("user1");
        log1.setCreatedAt(LocalDateTime.now().minusHours(2));

        OperationLog log2 = new OperationLog();
        log2.setId(2L);
        log2.setUsername("user2");
        log2.setCreatedAt(LocalDateTime.now().minusHours(1));

        Page<OperationLog> page = new Page<>();
        page.setRecords(Arrays.asList(log2, log1)); // 按时间倒序
        page.setTotal(2);

        when(operationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<OperationLogVO> result = operationLogService.queryLogs(queryRequest);

        // Then
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getId()).isEqualTo(2L); // 最新的在前
    }
}
