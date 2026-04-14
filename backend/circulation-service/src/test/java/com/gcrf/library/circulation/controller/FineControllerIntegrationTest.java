package com.gcrf.library.circulation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.dto.request.FinePaymentRequest;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.mapper.BorrowMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FineController集成测试
 *
 * 测试覆盖范围：
 * - 分页查询罚金记录（带过滤条件）
 * - 查询逾期记录
 * - 计算罚金（逾期和未逾期场景）
 * - 支付罚金（成功和失败场景）
 * - 批量归还处理
 * - 罚金统计
 * - 健康检查
 *
 * @author GCRF Team
 * @since 2025-11-08
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class FineControllerIntegrationTest extends BaseIntegrationTest {

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

    private Borrow overdueUnpaidBorrow;
    private Borrow overduePaidBorrow;
    private Borrow activeBorrow;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        LambdaQueryWrapper<Borrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Borrow::getBorrowId, "FINEIT");
        borrowMapper.delete(queryWrapper);

        // 1. Overdue unpaid borrow (fineAmount > 0)
        overdueUnpaidBorrow = new Borrow();
        overdueUnpaidBorrow.setBorrowId("FINEIT001");
        overdueUnpaidBorrow.setReaderId(9001L);
        overdueUnpaidBorrow.setBookId(8001L);
        overdueUnpaidBorrow.setBookBarcode("FB001");
        overdueUnpaidBorrow.setBorrowDate(LocalDateTime.now().minusDays(40));
        overdueUnpaidBorrow.setDueDate(LocalDateTime.now().minusDays(15));
        overdueUnpaidBorrow.setReturnDate(null);
        overdueUnpaidBorrow.setRenewCount(0);
        overdueUnpaidBorrow.setMaxRenewCount(2);
        overdueUnpaidBorrow.setStatus("OVERDUE");
        overdueUnpaidBorrow.setFineAmount(new BigDecimal("1.20"));
        overdueUnpaidBorrow.setFinePaid(false);
        overdueUnpaidBorrow.setFinePaidDate(null);
        overdueUnpaidBorrow.setRemarks("罚金未支付测试");
        overdueUnpaidBorrow.setCreatedAt(LocalDateTime.now().minusDays(40));
        overdueUnpaidBorrow.setUpdatedAt(LocalDateTime.now());
        borrowMapper.insert(overdueUnpaidBorrow);

        // 2. Overdue paid borrow
        overduePaidBorrow = new Borrow();
        overduePaidBorrow.setBorrowId("FINEIT002");
        overduePaidBorrow.setReaderId(9002L);
        overduePaidBorrow.setBookId(8002L);
        overduePaidBorrow.setBookBarcode("FB002");
        overduePaidBorrow.setBorrowDate(LocalDateTime.now().minusDays(50));
        overduePaidBorrow.setDueDate(LocalDateTime.now().minusDays(20));
        overduePaidBorrow.setReturnDate(LocalDateTime.now().minusDays(5));
        overduePaidBorrow.setRenewCount(1);
        overduePaidBorrow.setMaxRenewCount(2);
        overduePaidBorrow.setStatus("OVERDUE");
        overduePaidBorrow.setFineAmount(new BigDecimal("2.50"));
        overduePaidBorrow.setFinePaid(true);
        overduePaidBorrow.setFinePaidDate(LocalDateTime.now().minusDays(4));
        overduePaidBorrow.setRemarks("罚金已支付测试");
        overduePaidBorrow.setCreatedAt(LocalDateTime.now().minusDays(50));
        overduePaidBorrow.setUpdatedAt(LocalDateTime.now());
        borrowMapper.insert(overduePaidBorrow);

        // 3. Active (non-overdue) borrow - for calculateFine zero test
        activeBorrow = new Borrow();
        activeBorrow.setBorrowId("FINEIT003");
        activeBorrow.setReaderId(9003L);
        activeBorrow.setBookId(8003L);
        activeBorrow.setBookBarcode("FB003");
        activeBorrow.setBorrowDate(LocalDateTime.now().minusDays(5));
        activeBorrow.setDueDate(LocalDateTime.now().plusDays(25));
        activeBorrow.setReturnDate(null);
        activeBorrow.setRenewCount(0);
        activeBorrow.setMaxRenewCount(2);
        activeBorrow.setStatus("BORROWED");
        activeBorrow.setFineAmount(BigDecimal.ZERO);
        activeBorrow.setFinePaid(false);
        activeBorrow.setFinePaidDate(null);
        activeBorrow.setRemarks("未逾期借阅测试");
        activeBorrow.setCreatedAt(LocalDateTime.now().minusDays(5));
        activeBorrow.setUpdatedAt(LocalDateTime.now());
        borrowMapper.insert(activeBorrow);
    }

    @AfterEach
    void tearDown() {
        LambdaQueryWrapper<Borrow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Borrow::getBorrowId, "FINEIT");
        borrowMapper.delete(queryWrapper);
    }

    @Test
    void queryFines_withFilters_shouldReturnPaged() throws Exception {
        mockMvc.perform(get("/api/v1/fines")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("readerId", "9001")
                        .param("paid", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void queryOverdueRecords_shouldReturnOverdueFines() throws Exception {
        mockMvc.perform(get("/api/v1/fines/overdue")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void calculateFine_forOverdueBorrow_shouldReturnAmount() throws Exception {
        mockMvc.perform(post("/api/v1/fines/calculate/{borrowId}", overdueUnpaidBorrow.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.borrowId").value(overdueUnpaidBorrow.getId()))
                .andExpect(jsonPath("$.data.fineAmount").exists())
                .andExpect(jsonPath("$.data.overdueDays").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void calculateFine_forNonOverdueBorrow_shouldReturnZero() throws Exception {
        mockMvc.perform(post("/api/v1/fines/calculate/{borrowId}", activeBorrow.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.borrowId").value(activeBorrow.getId()))
                .andExpect(jsonPath("$.data.fineAmount").value(0))
                .andExpect(jsonPath("$.data.overdueDays").value(0));
    }

    @Test
    void payFine_success_shouldMarkAsPaid() throws Exception {
        FinePaymentRequest request = new FinePaymentRequest();
        request.setBorrowId(overdueUnpaidBorrow.getId());
        request.setPaymentMethod("CASH");
        request.setRemarks("集成测试支付");

        mockMvc.perform(post("/api/v1/fines/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.borrowId").value(overdueUnpaidBorrow.getId()))
                .andExpect(jsonPath("$.data.finePaid").value(true));
    }

    @Test
    void payFine_forNonExistentBorrow_shouldReturnError() throws Exception {
        FinePaymentRequest request = new FinePaymentRequest();
        request.setBorrowId(999999999L);
        request.setPaymentMethod("CASH");
        request.setRemarks("测试失败场景");

        mockMvc.perform(post("/api/v1/fines/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    @Test
    void batchReturn_shouldProcessMultipleBorrows() throws Exception {
        List<Long> borrowIds = Arrays.asList(overdueUnpaidBorrow.getId(), activeBorrow.getId());

        mockMvc.perform(post("/api/v1/fines/batch-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrowIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.results").isArray());
    }

    @Test
    void getFineStatistics_shouldReturnAggregation() throws Exception {
        mockMvc.perform(get("/api/v1/fines/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").exists())
                .andExpect(jsonPath("$.data.paidCount").exists())
                .andExpect(jsonPath("$.data.unpaidCount").exists())
                .andExpect(jsonPath("$.data.totalFine").exists());
    }

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/fines/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Fine Service is running"));
    }
}
