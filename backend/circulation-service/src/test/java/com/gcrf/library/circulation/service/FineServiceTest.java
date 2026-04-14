package com.gcrf.library.circulation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.dto.request.FinePaymentRequest;
import com.gcrf.library.circulation.dto.response.FineVO;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.mapper.BorrowMapper;
import com.gcrf.library.circulation.service.impl.FineServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FineService 单元测试
 *
 * 测试覆盖:
 * - calculateFine() 罚金计算 (考虑3天宽限期, 每天0.1元, 最高50元)
 * - payFine() 罚金支付
 * - queryOverdueRecords() 逾期记录查询
 * - queryFines() 罚金记录查询
 * - getFineStatistics() 罚金统计
 * - batchReturn() 批量归还
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FineService 单元测试")
class FineServiceTest {

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private CirculationEventPublisher eventPublisher;

    @InjectMocks
    private FineServiceImpl fineService;

    private Borrow testBorrow;

    @BeforeEach
    void setUp() {
        testBorrow = createTestBorrow(1L, "BW-20251130-0001", 1L, 1L, "BORROWED");
    }

    // ==================== 辅助方法 ====================

    private Borrow createTestBorrow(Long id, String borrowId, Long readerId, Long bookId, String status) {
        Borrow borrow = new Borrow();
        borrow.setId(id);
        borrow.setBorrowId(borrowId);
        borrow.setReaderId(readerId);
        borrow.setBookId(bookId);
        borrow.setBookBarcode("BC-" + bookId);
        borrow.setBorrowDate(LocalDateTime.now().minusDays(10));
        borrow.setDueDate(LocalDateTime.now().plusDays(20));
        borrow.setReturnDate(null);
        borrow.setRenewCount(0);
        borrow.setMaxRenewCount(2);
        borrow.setStatus(status);
        borrow.setFineAmount(BigDecimal.ZERO);
        borrow.setFinePaid(false);
        borrow.setFinePaidDate(null);
        borrow.setRemarks(null);
        borrow.setCreatedAt(LocalDateTime.now().minusDays(10));
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow.setDeletedAt(null);
        return borrow;
    }

    private Borrow createOverdueBorrow(Long id, String borrowId, Long readerId, Long bookId, int overdueDays) {
        Borrow borrow = createTestBorrow(id, borrowId, readerId, bookId, "OVERDUE");
        borrow.setBorrowDate(LocalDateTime.now().minusDays(30 + overdueDays));
        borrow.setDueDate(LocalDateTime.now().minusDays(overdueDays));
        return borrow;
    }

    // ==================== calculateFine() 罚金计算测试 ====================

    @Test
    @DisplayName("计算罚金 - 未逾期无罚金")
    void testCalculateFine_NoOverdue() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setDueDate(LocalDateTime.now().plusDays(5)); // 5天后到期
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act
        Map<String, Object> result = fineService.calculateFine(borrowId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("overdueDays")).isEqualTo(0);
        assertThat((BigDecimal) result.get("fineAmount")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get("message")).isEqualTo("未逾期,无需支付罚金");
    }

    @Test
    @DisplayName("计算罚金 - 逾期1天（在宽限期内，无罚金）")
    void testCalculateFine_WithinGracePeriod_1Day() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setDueDate(LocalDateTime.now().minusDays(1)); // 逾期1天
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act
        Map<String, Object> result = fineService.calculateFine(borrowId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("overdueDays")).isEqualTo(0);
        assertThat((BigDecimal) result.get("fineAmount")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("计算罚金 - 逾期3天（宽限期边界，无罚金）")
    void testCalculateFine_GracePeriodBoundary() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setDueDate(LocalDateTime.now().minusDays(3)); // 逾期3天
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act
        Map<String, Object> result = fineService.calculateFine(borrowId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("overdueDays")).isEqualTo(0);
        assertThat((BigDecimal) result.get("fineAmount")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("计算罚金 - 逾期5天（宽限期外2天，罚金0.2元）")
    void testCalculateFine_AfterGracePeriod() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setDueDate(LocalDateTime.now().minusDays(5)); // 逾期5天，实际计算2天
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act
        Map<String, Object> result = fineService.calculateFine(borrowId);

        // Assert
        assertThat(result).isNotNull();
        assertThat((Long) result.get("overdueDays")).isEqualTo(2); // 5 - 3 = 2天
        assertThat((BigDecimal) result.get("fineAmount")).isEqualByComparingTo(new BigDecimal("0.20")); // 2 * 0.1
    }

    @Test
    @DisplayName("计算罚金 - 逾期503天（超过最高50元上限）")
    void testCalculateFine_MaxFineLimit() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setDueDate(LocalDateTime.now().minusDays(503)); // 逾期503天，实际计算500天
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act
        Map<String, Object> result = fineService.calculateFine(borrowId);

        // Assert
        assertThat(result).isNotNull();
        // 500 * 0.1 = 50元，超过最高限制
        assertThat((BigDecimal) result.get("fineAmount")).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("计算罚金 - 借阅记录不存在")
    void testCalculateFine_BorrowNotFound() {
        // Arrange
        Long borrowId = 999L;
        when(borrowMapper.selectById(borrowId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> fineService.calculateFine(borrowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借阅记录不存在");
    }

    // ==================== payFine() 罚金支付测试 ====================

    @Test
    @DisplayName("支付罚金 - 成功")
    void testPayFine_Success() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setFineAmount(new BigDecimal("5.00"));
        testBorrow.setFinePaid(false);

        FinePaymentRequest request = new FinePaymentRequest();
        request.setBorrowId(borrowId);
        request.setPaymentMethod("CASH");
        request.setRemarks("现金支付");

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);
        doNothing().when(eventPublisher).publishFinePaidEvent(any());

        // Act
        FineVO result = fineService.payFine(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFinePaid()).isTrue();
        assertThat(result.getFineAmount()).isEqualByComparingTo(new BigDecimal("5.00"));

        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper).updateById(borrowCaptor.capture());
        Borrow capturedBorrow = borrowCaptor.getValue();
        assertThat(capturedBorrow.getFinePaid()).isTrue();
        assertThat(capturedBorrow.getFinePaidDate()).isNotNull();
    }

    @Test
    @DisplayName("支付罚金 - 已支付无需重复支付")
    void testPayFine_AlreadyPaid() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setFineAmount(new BigDecimal("5.00"));
        testBorrow.setFinePaid(true);
        testBorrow.setFinePaidDate(LocalDateTime.now().minusDays(1));

        FinePaymentRequest request = new FinePaymentRequest();
        request.setBorrowId(borrowId);
        request.setPaymentMethod("CASH");

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act & Assert
        assertThatThrownBy(() -> fineService.payFine(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("罚金已支付");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
    }

    @Test
    @DisplayName("支付罚金 - 无罚金无需支付")
    void testPayFine_NoFine() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setFineAmount(BigDecimal.ZERO);
        testBorrow.setFinePaid(false);

        FinePaymentRequest request = new FinePaymentRequest();
        request.setBorrowId(borrowId);
        request.setPaymentMethod("CASH");

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act & Assert
        assertThatThrownBy(() -> fineService.payFine(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无需支付罚金");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
    }

    // ==================== queryOverdueRecords() 逾期记录查询测试 ====================

    @Test
    @DisplayName("查询逾期记录 - 成功")
    void testQueryOverdueRecords_Success() {
        // Arrange
        Borrow overdueBorrow1 = createOverdueBorrow(1L, "BW-001", 1L, 1L, 5);
        overdueBorrow1.setFineAmount(new BigDecimal("0.20"));

        Page<Borrow> resultPage = new Page<>(1, 10);
        resultPage.setRecords(Collections.singletonList(overdueBorrow1));
        resultPage.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);

        // Act
        PageResult<FineVO> result = fineService.queryOverdueRecords(null, null, 1, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);

        verify(borrowMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== getFineStatistics() 罚金统计测试 ====================

    @Test
    @DisplayName("获取罚金统计 - 成功")
    void testGetFineStatistics_Success() {
        // Arrange
        Borrow borrow1 = createTestBorrow(1L, "BW-001", 1L, 1L, "OVERDUE");
        borrow1.setFineAmount(new BigDecimal("5.00"));
        borrow1.setFinePaid(true);

        Borrow borrow2 = createTestBorrow(2L, "BW-002", 2L, 2L, "OVERDUE");
        borrow2.setFineAmount(new BigDecimal("3.00"));
        borrow2.setFinePaid(false);

        when(borrowMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(borrow1, borrow2));

        // Act
        Map<String, Object> result = fineService.getFineStatistics(null);

        // Assert
        assertThat(result).isNotNull();
        assertThat((Integer) result.get("totalCount")).isEqualTo(2);
        assertThat((Integer) result.get("paidCount")).isEqualTo(1);
        assertThat((Integer) result.get("unpaidCount")).isEqualTo(1);
        assertThat((BigDecimal) result.get("totalFine")).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat((BigDecimal) result.get("paidFine")).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat((BigDecimal) result.get("unpaidFine")).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    // ==================== batchReturn() 批量归还测试 ====================

    @Test
    @DisplayName("批量归还 - 成功")
    void testBatchReturn_Success() {
        // Arrange
        Borrow borrow1 = createTestBorrow(1L, "BW-001", 1L, 1L, "BORROWED");
        borrow1.setDueDate(LocalDateTime.now().plusDays(5)); // 未逾期

        Borrow borrow2 = createOverdueBorrow(2L, "BW-002", 2L, 2L, 5);
        borrow2.setStatus("BORROWED");

        when(borrowMapper.selectById(1L)).thenReturn(borrow1);
        when(borrowMapper.selectById(2L)).thenReturn(borrow2);
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);
        doNothing().when(eventPublisher).publishReturnCompletedEvent(any());

        List<Long> borrowIds = Arrays.asList(1L, 2L);

        // Act
        Map<String, Object> result = fineService.batchReturn(borrowIds);

        // Assert
        assertThat(result).isNotNull();
        assertThat((Integer) result.get("totalCount")).isEqualTo(2);
        assertThat((Integer) result.get("successCount")).isEqualTo(2);
        assertThat((Integer) result.get("failedCount")).isEqualTo(0);

        verify(borrowMapper, times(2)).updateById(any(Borrow.class));
    }

    @Test
    @DisplayName("批量归还 - 空列表")
    void testBatchReturn_EmptyList() {
        // Act & Assert
        assertThatThrownBy(() -> fineService.batchReturn(Collections.emptyList()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借阅记录ID列表不能为空");

        verify(borrowMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("批量归还 - 部分失败")
    void testBatchReturn_PartialFailure() {
        // Arrange
        Borrow borrow1 = createTestBorrow(1L, "BW-001", 1L, 1L, "BORROWED");
        borrow1.setDueDate(LocalDateTime.now().plusDays(5));

        Borrow borrow2 = createTestBorrow(2L, "BW-002", 2L, 2L, "RETURNED");
        borrow2.setReturnDate(LocalDateTime.now().minusDays(1));

        when(borrowMapper.selectById(1L)).thenReturn(borrow1);
        when(borrowMapper.selectById(2L)).thenReturn(borrow2);
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);
        doNothing().when(eventPublisher).publishReturnCompletedEvent(any());

        List<Long> borrowIds = Arrays.asList(1L, 2L);

        // Act
        Map<String, Object> result = fineService.batchReturn(borrowIds);

        // Assert
        assertThat(result).isNotNull();
        assertThat((Integer) result.get("totalCount")).isEqualTo(2);
        assertThat((Integer) result.get("successCount")).isEqualTo(1);
        assertThat((Integer) result.get("failedCount")).isEqualTo(1);

        verify(borrowMapper, times(1)).updateById(any(Borrow.class));
    }
}
