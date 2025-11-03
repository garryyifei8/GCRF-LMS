package com.gcrf.library.circulation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.dto.RenewRequest;
import com.gcrf.library.circulation.dto.ReturnRequest;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.mapper.BorrowMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BorrowController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询借阅记录（带过滤条件）
 * - 根据ID获取借阅详情
 * - 借书（创建借阅记录）
 * - 还书（归还图书、计算罚金）
 * - 续借（延长借阅期限）
 * - 获取逾期借阅记录
 * - 更新逾期状态（定时任务）
 * - 健康检查
 *
 * 使用MockBean模拟Feign客户端调用，避免依赖外部服务
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BorrowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BorrowMapper borrowMapper;

    @MockBean
    private BookServiceClient bookServiceClient;

    @MockBean
    private ReaderServiceClient readerServiceClient;

    private Borrow testBorrow;
    private Borrow testOverdueBorrow;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        LambdaQueryWrapper<Borrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Borrow::getBorrowId, "INTTEST");
        borrowMapper.delete(queryWrapper);

        // Create test borrow record (BORROWED status)
        testBorrow = new Borrow();
        testBorrow.setBorrowId("INTTEST001");
        testBorrow.setReaderId(1001L);
        testBorrow.setBookId(2001L);
        testBorrow.setBookBarcode("BOOK001");
        testBorrow.setBorrowDate(LocalDateTime.now().minusDays(10));
        testBorrow.setDueDate(LocalDateTime.now().plusDays(20));
        testBorrow.setReturnDate(null);
        testBorrow.setRenewCount(0);
        testBorrow.setMaxRenewCount(2);
        testBorrow.setStatus("BORROWED");
        testBorrow.setFineAmount(BigDecimal.ZERO);
        testBorrow.setFinePaid(false);
        testBorrow.setFinePaidDate(null);
        testBorrow.setRemarks("集成测试借阅记录");
        testBorrow.setCreatedAt(LocalDateTime.now());
        testBorrow.setUpdatedAt(LocalDateTime.now());

        int insertResult = borrowMapper.insert(testBorrow);
        if (insertResult != 1) {
            throw new RuntimeException("Failed to insert test borrow record");
        }

        // Create test overdue borrow record
        testOverdueBorrow = new Borrow();
        testOverdueBorrow.setBorrowId("INTTEST002");
        testOverdueBorrow.setReaderId(1002L);
        testOverdueBorrow.setBookId(2002L);
        testOverdueBorrow.setBookBarcode("BOOK002");
        testOverdueBorrow.setBorrowDate(LocalDateTime.now().minusDays(40));
        testOverdueBorrow.setDueDate(LocalDateTime.now().minusDays(10)); // Overdue
        testOverdueBorrow.setReturnDate(null);
        testOverdueBorrow.setRenewCount(1);
        testOverdueBorrow.setMaxRenewCount(2);
        testOverdueBorrow.setStatus("OVERDUE");
        testOverdueBorrow.setFineAmount(new BigDecimal("5.00"));
        testOverdueBorrow.setFinePaid(false);
        testOverdueBorrow.setFinePaidDate(null);
        testOverdueBorrow.setRemarks("逾期测试记录");
        testOverdueBorrow.setCreatedAt(LocalDateTime.now().minusDays(40));
        testOverdueBorrow.setUpdatedAt(LocalDateTime.now());

        int overdueInsertResult = borrowMapper.insert(testOverdueBorrow);
        if (overdueInsertResult != 1) {
            throw new RuntimeException("Failed to insert test overdue borrow record");
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up all test data
        LambdaQueryWrapper<Borrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Borrow::getBorrowId, "INTTEST");
        borrowMapper.delete(queryWrapper);
    }

    // ========== 查询接口测试 (5 tests) ==========

    @Test
    void testQueryBorrows_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/borrows")
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
    void testQueryBorrows_WithReaderId() throws Exception {
        // Act & Assert - 按读者ID查询
        mockMvc.perform(get("/api/v1/borrows")
                        .param("readerId", "1001")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testQueryBorrows_WithStatus() throws Exception {
        // Act & Assert - 按状态查询
        mockMvc.perform(get("/api/v1/borrows")
                        .param("status", "BORROWED")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryBorrows_WithBookId() throws Exception {
        // Act & Assert - 按图书ID查询
        mockMvc.perform(get("/api/v1/borrows")
                        .param("bookId", "2001")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryBorrows_EmptyResult() throws Exception {
        // Act & Assert - 查询不存在的借阅记录
        mockMvc.perform(get("/api/v1/borrows")
                        .param("borrowId", "NOTEXIST999")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ========== 根据ID查询测试 (2 tests) ==========

    @Test
    void testGetBorrowById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/borrows/{id}", testBorrow.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testBorrow.getId()))
                .andExpect(jsonPath("$.data.borrowId").value("INTTEST001"))
                .andExpect(jsonPath("$.data.readerId").value(1001))
                .andExpect(jsonPath("$.data.bookId").value(2001))
                .andExpect(jsonPath("$.data.status").value("BORROWED"));
    }

    @Test
    void testGetBorrowById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/borrows/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 借书测试 (4 tests) ==========

    @Test
    void testBorrowBook_Success() throws Exception {
        // Arrange - Mock Feign client responses
        ReaderDTO mockReaderDTO = new ReaderDTO();
        mockReaderDTO.setId(1003L);
        mockReaderDTO.setReaderId("R1003");
        mockReaderDTO.setName("测试读者");
        mockReaderDTO.setStatus("ACTIVE");
        mockReaderDTO.setMaxBorrowCount(10);

        BookDTO mockBookDTO = new BookDTO();
        mockBookDTO.setId(2003L);
        mockBookDTO.setIsbn("978-7-111-12345-6");
        mockBookDTO.setTitle("测试图书");
        mockBookDTO.setAvailableCopies(5);

        when(readerServiceClient.getReaderById(1003L)).thenReturn(Result.success(mockReaderDTO));
        when(readerServiceClient.validateReaderStatus(1003L)).thenReturn(Result.success(true));
        when(readerServiceClient.hasOverdueBooks(1003L)).thenReturn(Result.success(false));
        when(readerServiceClient.hasUnpaidFine(1003L)).thenReturn(Result.success(false));
        when(bookServiceClient.getBookById(2003L)).thenReturn(Result.success(mockBookDTO));
        when(bookServiceClient.checkAvailability(2003L)).thenReturn(Result.success(true));
        when(bookServiceClient.decreaseAvailableCopies(2003L)).thenReturn(Result.success());

        BorrowRequest request = new BorrowRequest();
        request.setBookId(2003L);
        request.setReaderId(1003L);
        request.setBorrowDays(30);
        request.setRemark("集成测试借书");

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.readerId").value(1003))
                .andExpect(jsonPath("$.data.bookId").value(2003))
                .andExpect(jsonPath("$.data.status").value("BORROWED"));
    }

    @Test
    void testBorrowBook_InvalidData() throws Exception {
        // Arrange - Missing required fields
        BorrowRequest request = new BorrowRequest();
        request.setBookId(null); // Missing bookId
        request.setReaderId(null); // Missing readerId

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBorrowBook_ReaderNotActive() throws Exception {
        // Arrange - Mock reader with SUSPENDED status
        ReaderDTO mockReaderDTO = new ReaderDTO();
        mockReaderDTO.setId(1004L);
        mockReaderDTO.setReaderId("R1004");
        mockReaderDTO.setName("挂失读者");
        mockReaderDTO.setStatus("SUSPENDED");

        when(readerServiceClient.getReaderById(1004L)).thenReturn(Result.success(mockReaderDTO));
        when(readerServiceClient.validateReaderStatus(1004L)).thenReturn(Result.success(false));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(2004L);
        request.setReaderId(1004L);
        request.setBorrowDays(30);

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testBorrowBook_BookNotAvailable() throws Exception {
        // Arrange - Mock book with no available copies
        ReaderDTO mockReaderDTO = new ReaderDTO();
        mockReaderDTO.setId(1005L);
        mockReaderDTO.setReaderId("R1005");
        mockReaderDTO.setName("测试读者");
        mockReaderDTO.setStatus("ACTIVE");

        BookDTO mockBookDTO = new BookDTO();
        mockBookDTO.setId(2005L);
        mockBookDTO.setIsbn("978-7-111-99999-9");
        mockBookDTO.setTitle("无库存图书");
        mockBookDTO.setAvailableCopies(0);

        when(readerServiceClient.getReaderById(1005L)).thenReturn(Result.success(mockReaderDTO));
        when(readerServiceClient.validateReaderStatus(1005L)).thenReturn(Result.success(true));
        when(readerServiceClient.hasOverdueBooks(1005L)).thenReturn(Result.success(false));
        when(readerServiceClient.hasUnpaidFine(1005L)).thenReturn(Result.success(false));
        when(bookServiceClient.getBookById(2005L)).thenReturn(Result.success(mockBookDTO));
        when(bookServiceClient.checkAvailability(2005L)).thenReturn(Result.success(false));

        BorrowRequest request = new BorrowRequest();
        request.setBookId(2005L);
        request.setReaderId(1005L);
        request.setBorrowDays(30);

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 还书测试 (3 tests) ==========

    @Test
    void testReturnBook_Success() throws Exception {
        // Arrange - Mock Feign client response
        when(bookServiceClient.increaseAvailableCopies(anyLong())).thenReturn(Result.success());

        ReturnRequest request = new ReturnRequest();
        request.setBorrowId(testBorrow.getId());
        request.setPayFine(false);
        request.setRemarks("正常归还测试");

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
    }

    @Test
    void testReturnBook_InvalidData() throws Exception {
        // Arrange - Missing required field
        ReturnRequest request = new ReturnRequest();
        request.setBorrowId(null); // Missing borrowId

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReturnBook_NotFound() throws Exception {
        // Arrange
        ReturnRequest request = new ReturnRequest();
        request.setBorrowId(999999L);
        request.setPayFine(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 续借测试 (3 tests) ==========

    @Test
    void testRenewBook_Success() throws Exception {
        // Arrange
        RenewRequest request = new RenewRequest();
        request.setBorrowId(testBorrow.getId());
        request.setRenewDays(30);
        request.setReason("继续阅读");
        request.setRemarks("续借测试");

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.renewCount").value(1))
                .andExpect(jsonPath("$.data.status").value("BORROWED"));
    }

    @Test
    void testRenewBook_InvalidData() throws Exception {
        // Arrange - Missing required field
        RenewRequest request = new RenewRequest();
        request.setBorrowId(null); // Missing borrowId
        request.setRenewDays(30);

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRenewBook_NotFound() throws Exception {
        // Arrange
        RenewRequest request = new RenewRequest();
        request.setBorrowId(999999L);
        request.setRenewDays(30);

        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ========== 逾期记录测试 (1 test) ==========

    @Test
    void testGetOverdueBorrows_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/borrows/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("OVERDUE"));
    }

    // ========== 更新逾期状态测试 (1 test) ==========

    @Test
    void testUpdateOverdueStatus_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/borrows/update-overdue-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 健康检查测试 (1 test) ==========

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/borrows/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Circulation Service (Borrow) is running"));
    }
}
