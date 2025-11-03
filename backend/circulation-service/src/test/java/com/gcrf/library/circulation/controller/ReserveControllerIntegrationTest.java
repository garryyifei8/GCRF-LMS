package com.gcrf.library.circulation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.ReserveRequest;
import com.gcrf.library.circulation.entity.Reserve;
import com.gcrf.library.circulation.mapper.ReserveMapper;
import com.gcrf.library.common.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReserveController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询预约记录（按readerId、status过滤）
 * - 根据ID获取预约详情
 * - 预约图书（包括验证逻辑）
 * - 取书（完成预约）
 * - 取消预约
 * - 批量过期处理
 * - 获取待通知预约记录
 * - 健康检查
 *
 * 使用@MockBean模拟Feign客户端，避免依赖真实的book-service和reader-service
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReserveControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReserveMapper reserveMapper;

    @MockBean
    private BookServiceClient bookServiceClient;

    @MockBean
    private ReaderServiceClient readerServiceClient;

    private Reserve testReserve;
    private Long testBookId;
    private Long testReaderId;

    @BeforeEach
    void setUp() {
        // Initialize test data IDs
        testBookId = 1001L;
        testReaderId = 2001L;

        // Clean up existing test data
        LambdaQueryWrapper<Reserve> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Reserve::getReserveId, "INTTEST%");
        reserveMapper.delete(queryWrapper);

        // Create test reserve record
        testReserve = new Reserve();
        testReserve.setReserveId("INTTEST001");
        testReserve.setReaderId(testReaderId);
        testReserve.setBookId(testBookId);
        testReserve.setReserveDate(LocalDateTime.now());
        testReserve.setExpiryDate(LocalDateTime.now().plusDays(7));
        testReserve.setPickupDate(null);
        testReserve.setCancelDate(null);
        testReserve.setStatus("RESERVED");
        testReserve.setNotifySent(false);
        testReserve.setNotifySentDate(null);
        testReserve.setNotifyCount(0);
        testReserve.setRemarks("集成测试预约记录");
        testReserve.setCreatedAt(LocalDateTime.now());
        testReserve.setUpdatedAt(LocalDateTime.now());

        int insertResult = reserveMapper.insert(testReserve);
        if (insertResult != 1) {
            throw new RuntimeException("Failed to insert test reserve record");
        }

        // Setup default mock responses for Feign clients
        setupDefaultMocks();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data - delete by reserve_id pattern AND by test reader/book IDs
        // This ensures cleanup of both manually created test records and service-generated records
        LambdaQueryWrapper<Reserve> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Reserve::getReserveId, "INTTEST%")
                .or().eq(Reserve::getReaderId, testReaderId);
        reserveMapper.delete(queryWrapper);
    }

    /**
     * 设置默认的Mock响应
     */
    private void setupDefaultMocks() {
        // Mock BookServiceClient responses
        BookDTO mockBook = new BookDTO();
        mockBook.setId(testBookId);
        mockBook.setIsbn("978-7-121-12345-6");
        mockBook.setTitle("集成测试图书");
        mockBook.setAvailableCopies(5);
        when(bookServiceClient.getBookById(anyLong())).thenReturn(Result.success(mockBook));
        when(bookServiceClient.checkAvailability(anyLong())).thenReturn(Result.success(true));
        when(bookServiceClient.decreaseAvailableCopies(anyLong())).thenReturn(Result.success());
        when(bookServiceClient.increaseAvailableCopies(anyLong())).thenReturn(Result.success());

        // Mock ReaderServiceClient responses
        ReaderDTO mockReader = new ReaderDTO();
        mockReader.setId(testReaderId);
        mockReader.setReaderId("READER001");
        mockReader.setName("集成测试读者");
        mockReader.setStatus("ACTIVE");
        when(readerServiceClient.getReaderById(anyLong())).thenReturn(Result.success(mockReader));
        when(readerServiceClient.validateReaderStatus(anyLong())).thenReturn(Result.success(true));
        when(readerServiceClient.hasOverdueBooks(anyLong())).thenReturn(Result.success(false));
        when(readerServiceClient.hasUnpaidFine(anyLong())).thenReturn(Result.success(false));
    }

    // ========== 查询接口测试 (5 tests) ==========

    @Test
    void testQueryReserves_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reserves")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void testQueryReserves_WithReaderId() throws Exception {
        // Act & Assert - 按读者ID过滤
        mockMvc.perform(get("/api/v1/reserves")
                        .param("readerId", testReaderId.toString())
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReserves_WithStatus() throws Exception {
        // Act & Assert - 按状态过滤
        mockMvc.perform(get("/api/v1/reserves")
                        .param("status", "RESERVED")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReserves_WithReaderIdAndStatus() throws Exception {
        // Act & Assert - 同时按读者ID和状态过滤
        mockMvc.perform(get("/api/v1/reserves")
                        .param("readerId", testReaderId.toString())
                        .param("status", "RESERVED")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryReserves_EmptyResult() throws Exception {
        // Act & Assert - 查询不存在的读者
        mockMvc.perform(get("/api/v1/reserves")
                        .param("readerId", "999999")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void testGetReserveById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reserves/{id}", testReserve.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testReserve.getId()))
                .andExpect(jsonPath("$.data.reserveId").value("INTTEST001"))
                .andExpect(jsonPath("$.data.readerId").value(testReaderId))
                .andExpect(jsonPath("$.data.bookId").value(testBookId))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    @Test
    void testGetReserveById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reserves/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 预约图书测试 (5 tests) ==========

    @Test
    void testReserveBook_Success() throws Exception {
        // Arrange - Use a different bookId to avoid duplicate reservation error
        Long newBookId = 1002L;
        ReserveRequest request = new ReserveRequest();
        request.setBookId(newBookId);
        request.setReaderId(testReaderId);
        request.setReserveDays(7);
        request.setRemarks("集成测试预约");

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerId").value(testReaderId))
                .andExpect(jsonPath("$.data.bookId").value(newBookId))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    @Test
    void testReserveBook_MissingBookId() throws Exception {
        // Arrange - Missing required field
        ReserveRequest request = new ReserveRequest();
        request.setBookId(null); // Missing bookId
        request.setReaderId(testReaderId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReserveBook_MissingReaderId() throws Exception {
        // Arrange - Missing required field
        ReserveRequest request = new ReserveRequest();
        request.setBookId(testBookId);
        request.setReaderId(null); // Missing readerId

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReserveBook_BookNotAvailable() throws Exception {
        // Arrange - Mock book not available
        when(bookServiceClient.checkAvailability(anyLong())).thenReturn(Result.success(false));

        ReserveRequest request = new ReserveRequest();
        request.setBookId(testBookId);
        request.setReaderId(testReaderId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testReserveBook_ReaderInvalid() throws Exception {
        // Arrange - Mock reader status invalid
        when(readerServiceClient.validateReaderStatus(anyLong())).thenReturn(Result.success(false));

        ReserveRequest request = new ReserveRequest();
        request.setBookId(testBookId);
        request.setReaderId(testReaderId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 取书测试 (3 tests) ==========

    @Test
    void testPickupReserve_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/{id}/pickup", testReserve.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testReserve.getId()))
                .andExpect(jsonPath("$.data.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.data.pickupDate").isNotEmpty());
    }

    @Test
    void testPickupReserve_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/{id}/pickup", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testPickupReserve_AlreadyPickedUp() throws Exception {
        // Arrange - Mark reserve as already picked up
        testReserve.setStatus("PICKED_UP");
        testReserve.setPickupDate(LocalDateTime.now());
        reserveMapper.updateById(testReserve);

        // Act & Assert - Should fail because already picked up
        mockMvc.perform(post("/api/v1/reserves/{id}/pickup", testReserve.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));

        // Restore status for other tests
        testReserve.setStatus("RESERVED");
        testReserve.setPickupDate(null);
        reserveMapper.updateById(testReserve);
    }

    // ========== 取消预约测试 (3 tests) ==========

    @Test
    void testCancelReserve_Success() throws Exception {
        // Arrange - Create a new reserve for cancellation
        Reserve reserveToCancel = new Reserve();
        reserveToCancel.setReserveId("INTTEST002");
        reserveToCancel.setReaderId(testReaderId);
        reserveToCancel.setBookId(testBookId);
        reserveToCancel.setReserveDate(LocalDateTime.now());
        reserveToCancel.setExpiryDate(LocalDateTime.now().plusDays(7));
        reserveToCancel.setStatus("RESERVED");
        reserveToCancel.setNotifySent(false);
        reserveToCancel.setNotifyCount(0);
        reserveToCancel.setCreatedAt(LocalDateTime.now());
        reserveToCancel.setUpdatedAt(LocalDateTime.now());
        reserveMapper.insert(reserveToCancel);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/{id}/cancel", reserveToCancel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(reserveToCancel.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelDate").isNotEmpty());
    }

    @Test
    void testCancelReserve_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/{id}/cancel", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testCancelReserve_AlreadyCancelled() throws Exception {
        // Arrange - Create a cancelled reserve
        Reserve cancelledReserve = new Reserve();
        cancelledReserve.setReserveId("INTTEST003");
        cancelledReserve.setReaderId(testReaderId);
        cancelledReserve.setBookId(testBookId);
        cancelledReserve.setReserveDate(LocalDateTime.now());
        cancelledReserve.setExpiryDate(LocalDateTime.now().plusDays(7));
        cancelledReserve.setStatus("CANCELLED");
        cancelledReserve.setCancelDate(LocalDateTime.now());
        cancelledReserve.setNotifySent(false);
        cancelledReserve.setNotifyCount(0);
        cancelledReserve.setCreatedAt(LocalDateTime.now());
        cancelledReserve.setUpdatedAt(LocalDateTime.now());
        reserveMapper.insert(cancelledReserve);

        // Act & Assert - Should fail because already cancelled
        mockMvc.perform(post("/api/v1/reserves/{id}/cancel", cancelledReserve.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 批量过期处理测试 (1 test) ==========

    @Test
    void testExpireReserves_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reserves/expire-reserves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 待通知预约记录测试 (1 test) ==========

    @Test
    void testGetPendingNotifications_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reserves/pending-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== 健康检查测试 (1 test) ==========

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/reserves/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Circulation Service (Reserve) is running"));
    }
}
