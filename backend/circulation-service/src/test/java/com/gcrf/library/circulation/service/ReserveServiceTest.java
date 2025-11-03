package com.gcrf.library.circulation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.ReserveRequest;
import com.gcrf.library.circulation.dto.response.ReserveDetailVO;
import com.gcrf.library.circulation.dto.response.ReserveVO;
import com.gcrf.library.circulation.entity.Reserve;
import com.gcrf.library.circulation.mapper.ReserveMapper;
import com.gcrf.library.circulation.service.impl.ReserveServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.exception.SystemException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ReserveService 单元测试
 * <p>
 * 测试覆盖：
 * - 预约操作 (4个测试)
 * - 取书操作 (3个测试)
 * - 取消操作 (3个测试)
 * - 查询操作 (3个测试)
 * - 批量过期 (2个测试)
 * - 待通知查询 (2个测试)
 *
 * @author GCRF Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveService 单元测试")
class ReserveServiceTest {

    @Mock
    private ReserveMapper reserveMapper;

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private ReaderServiceClient readerServiceClient;

    @InjectMocks
    private ReserveServiceImpl reserveService;

    private Reserve testReserve;
    private ReaderDTO testReader;
    private BookDTO testBook;

    @BeforeEach
    void setUp() {
        testReserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        testReader = createTestReader(1L, "TEST001", "张三", "STUDENT");
        testBook = createTestBook(100L, "9787111111111", "Java编程思想", "Bruce Eckel");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的Reserve对象
     */
    private Reserve createTestReserve(Long id, String reserveId, Long readerId, Long bookId, String status) {
        Reserve reserve = new Reserve();
        reserve.setId(id);
        reserve.setReserveId(reserveId);
        reserve.setReaderId(readerId);
        reserve.setBookId(bookId);
        reserve.setReserveDate(LocalDateTime.now());
        reserve.setExpiryDate(LocalDateTime.now().plusDays(7));
        reserve.setStatus(status);
        reserve.setNotifySent(false);
        reserve.setNotifyCount(0);
        reserve.setCreatedAt(LocalDateTime.now());
        reserve.setUpdatedAt(LocalDateTime.now());
        return reserve;
    }

    /**
     * 创建测试用的ReaderDTO对象
     */
    private ReaderDTO createTestReader(Long id, String readerId, String name, String readerType) {
        ReaderDTO reader = new ReaderDTO();
        reader.setId(id);
        reader.setReaderId(readerId);
        reader.setName(name);
        reader.setReaderType(readerType);
        reader.setPhone("13800138000");
        return reader;
    }

    /**
     * 创建测试用的BookDTO对象
     */
    private BookDTO createTestBook(Long id, String isbn, String title, String author) {
        BookDTO book = new BookDTO();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    /**
     * 创建测试用的ReserveRequest对象
     */
    private ReserveRequest createReserveRequest(Long readerId, Long bookId, Integer reserveDays) {
        ReserveRequest request = new ReserveRequest();
        request.setReaderId(readerId);
        request.setBookId(bookId);
        request.setReserveDays(reserveDays);
        request.setRemarks("测试预约");
        return request;
    }

    // ==================== 预约操作测试 (4个) ====================

    @Test
    @DisplayName("预约图书 - 成功")
    void testReserveBook_Success() {
        // Arrange
        ReserveRequest request = createReserveRequest(1L, 100L, 7);

        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));
        when(reserveMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);
        when(reserveMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null); // No existing reserve ID for generateReserveId
        when(reserveMapper.insert(any(Reserve.class)))
                .thenReturn(1);

        // Act
        ReserveDetailVO result = reserveService.reserveBook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReaderId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("RESERVED");
        assertThat(result.getReserveId()).startsWith("RV-");

        ArgumentCaptor<Reserve> reserveCaptor = ArgumentCaptor.forClass(Reserve.class);
        verify(reserveMapper, times(1)).insert(reserveCaptor.capture());

        Reserve capturedReserve = reserveCaptor.getValue();
        assertThat(capturedReserve.getReaderId()).isEqualTo(1L);
        assertThat(capturedReserve.getBookId()).isEqualTo(100L);
        assertThat(capturedReserve.getStatus()).isEqualTo("RESERVED");
        assertThat(capturedReserve.getNotifySent()).isFalse();
        assertThat(capturedReserve.getNotifyCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("预约图书 - 读者状态无效")
    void testReserveBook_InvalidReaderStatus() {
        // Arrange
        ReserveRequest request = createReserveRequest(1L, 100L, 7);

        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(false));

        // Act & Assert
        assertThatThrownBy(() -> reserveService.reserveBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("读者状态不允许预约图书");

        verify(reserveMapper, never()).insert(any(Reserve.class));
    }

    @Test
    @DisplayName("预约图书 - 读者不存在")
    void testReserveBook_ReaderNotFound() {
        // Arrange
        ReserveRequest request = createReserveRequest(999L, 100L, 7);

        when(readerServiceClient.validateReaderStatus(999L))
                .thenReturn(Result.success(true));
        when(readerServiceClient.getReaderById(999L))
                .thenReturn(Result.error("读者不存在"));

        // Act & Assert
        assertThatThrownBy(() -> reserveService.reserveBook(request))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法获取读者信息");

        verify(reserveMapper, never()).insert(any(Reserve.class));
    }

    @Test
    @DisplayName("预约图书 - 重复预约")
    void testReserveBook_DuplicateReservation() {
        // Arrange
        ReserveRequest request = createReserveRequest(1L, 100L, 7);

        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));
        when(reserveMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L); // Active reservation exists

        // Act & Assert
        assertThatThrownBy(() -> reserveService.reserveBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已预约过此图书");

        verify(reserveMapper, never()).insert(any(Reserve.class));
    }

    // ==================== 取书操作测试 (3个) ====================

    @Test
    @DisplayName("取书 - 成功")
    void testPickupReserve_Success() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);
        when(reserveMapper.updateById(any(Reserve.class))).thenReturn(1);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));

        // Act
        ReserveDetailVO result = reserveService.pickupReserve(reserveId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PICKED_UP");

        ArgumentCaptor<Reserve> reserveCaptor = ArgumentCaptor.forClass(Reserve.class);
        verify(reserveMapper, times(1)).updateById(reserveCaptor.capture());

        Reserve updatedReserve = reserveCaptor.getValue();
        assertThat(updatedReserve.getStatus()).isEqualTo("PICKED_UP");
        assertThat(updatedReserve.getPickupDate()).isNotNull();
    }

    @Test
    @DisplayName("取书 - 预约记录不存在")
    void testPickupReserve_NotFound() {
        // Arrange
        Long reserveId = 999L;
        when(reserveMapper.selectById(reserveId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.pickupReserve(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约记录不存在");

        verify(reserveMapper, never()).updateById(any(Reserve.class));
    }

    @Test
    @DisplayName("取书 - 状态不允许")
    void testPickupReserve_InvalidStatus() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "CANCELLED");

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.pickupReserve(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态不允许取书");

        verify(reserveMapper, never()).updateById(any(Reserve.class));
    }

    @Test
    @DisplayName("取书 - 已过期")
    void testPickupReserve_Expired() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        reserve.setExpiryDate(LocalDateTime.now().minusDays(1)); // Set expiry to past

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);
        when(reserveMapper.updateById(any(Reserve.class))).thenReturn(1);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.pickupReserve(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约已过期");

        // Verify that status was updated to EXPIRED
        ArgumentCaptor<Reserve> reserveCaptor = ArgumentCaptor.forClass(Reserve.class);
        verify(reserveMapper, times(1)).updateById(reserveCaptor.capture());
        assertThat(reserveCaptor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    // ==================== 取消操作测试 (3个) ====================

    @Test
    @DisplayName("取消预约 - 成功")
    void testCancelReserve_Success() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);
        when(reserveMapper.updateById(any(Reserve.class))).thenReturn(1);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));

        // Act
        ReserveDetailVO result = reserveService.cancelReserve(reserveId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        ArgumentCaptor<Reserve> reserveCaptor = ArgumentCaptor.forClass(Reserve.class);
        verify(reserveMapper, times(1)).updateById(reserveCaptor.capture());

        Reserve updatedReserve = reserveCaptor.getValue();
        assertThat(updatedReserve.getStatus()).isEqualTo("CANCELLED");
        assertThat(updatedReserve.getCancelDate()).isNotNull();
    }

    @Test
    @DisplayName("取消预约 - 预约记录不存在")
    void testCancelReserve_NotFound() {
        // Arrange
        Long reserveId = 999L;
        when(reserveMapper.selectById(reserveId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.cancelReserve(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约记录不存在");

        verify(reserveMapper, never()).updateById(any(Reserve.class));
    }

    @Test
    @DisplayName("取消预约 - 状态不允许")
    void testCancelReserve_InvalidStatus() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "PICKED_UP");

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.cancelReserve(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态不允许取消");

        verify(reserveMapper, never()).updateById(any(Reserve.class));
    }

    // ==================== 查询操作测试 (3个) ====================

    @Test
    @DisplayName("分页查询预约 - 按读者ID筛选")
    void testQueryReserves_WithReaderId() {
        // Arrange
        Long readerId = 1L;
        Page<Reserve> resultPage = new Page<>(1, 10);
        Reserve reserve1 = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        Reserve reserve2 = createTestReserve(2L, "RV-20251028-0002", 1L, 101L, "PICKED_UP");
        resultPage.setRecords(Arrays.asList(reserve1, reserve2));
        resultPage.setTotal(2);

        when(reserveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(anyLong()))
                .thenReturn(Result.success(testBook));

        // Act
        PageResult<ReserveVO> result = reserveService.queryReserves(readerId, null, 1, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getRecords()).allMatch(r -> r.getReaderId().equals(1L));

        verify(reserveMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询预约 - 按状态筛选")
    void testQueryReserves_WithStatus() {
        // Arrange
        String status = "RESERVED";
        Page<Reserve> resultPage = new Page<>(1, 10);
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        resultPage.setRecords(Collections.singletonList(reserve));
        resultPage.setTotal(1);

        when(reserveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));

        // Act
        PageResult<ReserveVO> result = reserveService.queryReserves(null, status, 1, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("RESERVED");

        verify(reserveMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据ID获取预约详情 - 成功")
    void testGetReserveById_Success() {
        // Arrange
        Long reserveId = 1L;
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");

        when(reserveMapper.selectById(reserveId)).thenReturn(reserve);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(100L))
                .thenReturn(Result.success(testBook));

        // Act
        ReserveDetailVO result = reserveService.getReserveById(reserveId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getReserveId()).isEqualTo("RV-20251028-0001");
        assertThat(result.getReaderId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(100L);
        assertThat(result.getReaderName()).isEqualTo("张三");
        assertThat(result.getBookTitle()).isEqualTo("Java编程思想");

        verify(reserveMapper, times(1)).selectById(reserveId);
    }

    @Test
    @DisplayName("根据ID获取预约详情 - 不存在")
    void testGetReserveById_NotFound() {
        // Arrange
        Long reserveId = 999L;
        when(reserveMapper.selectById(reserveId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> reserveService.getReserveById(reserveId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约记录不存在");

        verify(reserveMapper, times(1)).selectById(reserveId);
    }

    // ==================== 批量过期测试 (2个) ====================

    @Test
    @DisplayName("批量过期处理 - 成功")
    void testExpireReserves_Success() {
        // Arrange
        Reserve expiredReserve1 = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        expiredReserve1.setExpiryDate(LocalDateTime.now().minusDays(1));

        Reserve expiredReserve2 = createTestReserve(2L, "RV-20251028-0002", 2L, 101L, "RESERVED");
        expiredReserve2.setExpiryDate(LocalDateTime.now().minusDays(2));

        when(reserveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(expiredReserve1, expiredReserve2));
        when(reserveMapper.updateById(any(Reserve.class))).thenReturn(1);

        // Act
        reserveService.expireReserves();

        // Assert
        ArgumentCaptor<Reserve> reserveCaptor = ArgumentCaptor.forClass(Reserve.class);
        verify(reserveMapper, times(2)).updateById(reserveCaptor.capture());

        List<Reserve> updatedReserves = reserveCaptor.getAllValues();
        assertThat(updatedReserves).hasSize(2);
        assertThat(updatedReserves).allMatch(r -> r.getStatus().equals("EXPIRED"));
    }

    @Test
    @DisplayName("批量过期处理 - 无过期记录")
    void testExpireReserves_NoExpiredRecords() {
        // Arrange
        when(reserveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        reserveService.expireReserves();

        // Assert
        verify(reserveMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(reserveMapper, never()).updateById(any(Reserve.class));
    }

    // ==================== 待通知查询测试 (2个) ====================

    @Test
    @DisplayName("获取待通知预约 - 成功")
    void testGetPendingNotifications_Success() {
        // Arrange
        Reserve reserve1 = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        reserve1.setNotifySent(false);
        reserve1.setNotifyCount(0);

        Reserve reserve2 = createTestReserve(2L, "RV-20251028-0002", 2L, 101L, "RESERVED");
        reserve2.setNotifySent(false);
        reserve2.setNotifyCount(2);

        when(reserveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(reserve1, reserve2));
        when(readerServiceClient.getReaderById(anyLong()))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(anyLong()))
                .thenReturn(Result.success(testBook));

        // Act
        List<ReserveVO> result = reserveService.getPendingNotifications();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getStatus().equals("RESERVED"));

        verify(reserveMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取待通知预约 - 无待通知记录")
    void testGetPendingNotifications_NoRecords() {
        // Arrange
        when(reserveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<ReserveVO> result = reserveService.getPendingNotifications();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(reserveMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== 边界情况测试 (2个) ====================

    @Test
    @DisplayName("预约图书 - Feign客户端异常处理")
    void testReserveBook_FeignClientException() {
        // Arrange
        ReserveRequest request = createReserveRequest(1L, 100L, 7);

        when(readerServiceClient.validateReaderStatus(1L))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        assertThatThrownBy(() -> reserveService.reserveBook(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Network error");

        verify(reserveMapper, never()).insert(any(Reserve.class));
    }

    @Test
    @DisplayName("查询预约 - Feign客户端失败不影响核心数据")
    void testQueryReserves_FeignClientFailureGraceful() {
        // Arrange
        Page<Reserve> resultPage = new Page<>(1, 10);
        Reserve reserve = createTestReserve(1L, "RV-20251028-0001", 1L, 100L, "RESERVED");
        resultPage.setRecords(Collections.singletonList(reserve));
        resultPage.setTotal(1);

        when(reserveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);
        // Simulate Feign client failures
        when(readerServiceClient.getReaderById(1L))
                .thenThrow(new RuntimeException("Service unavailable"));
        when(bookServiceClient.getBookById(100L))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        PageResult<ReserveVO> result = reserveService.queryReserves(1L, null, 1, 10);

        // Assert - Should still return core data even if enrichment fails
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getReaderId()).isEqualTo(1L);
        assertThat(result.getRecords().get(0).getBookId()).isEqualTo(100L);
        // Reader and book names will be null due to Feign failure
        assertThat(result.getRecords().get(0).getReaderName()).isNull();
        assertThat(result.getRecords().get(0).getBookTitle()).isNull();
    }
}
