package com.gcrf.library.circulation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.BorrowQueryRequest;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.dto.RenewRequest;
import com.gcrf.library.circulation.dto.ReturnRequest;
import com.gcrf.library.circulation.dto.response.BorrowDetailVO;
import com.gcrf.library.circulation.dto.response.BorrowVO;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.mapper.BorrowMapper;
import com.gcrf.library.circulation.service.impl.BorrowServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * BorrowService 单元测试
 *
 * 测试覆盖：
 * - borrowBook() 借书操作 (4个测试)
 * - returnBook() 还书操作 (3个测试)
 * - renewBook() 续借操作 (2个测试)
 * - queryBorrows() 查询操作 (4个测试)
 * - getBorrowById() 查询详情 (2个测试)
 * - getOverdueBorrows() 逾期查询 (1个测试)
 * - updateOverdueStatus() 状态更新 (1个测试)
 *
 * @author GCRF Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowService 单元测试")
class BorrowServiceTest {

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private ReaderServiceClient readerServiceClient;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private Borrow testBorrow;
    private ReaderDTO testReader;
    private BookDTO testBook;

    @BeforeEach
    void setUp() {
        testBorrow = createTestBorrow(1L, "BW-20251028-0001", 1L, 1L, "BORROWED");
        testReader = createTestReader(1L, "R001", "张三", "STUDENT", "ACTIVE");
        testBook = createTestBook(1L, "978-7-111-12345-6", "Java编程思想", "Bruce Eckel", 5);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的Borrow对象
     */
    private Borrow createTestBorrow(Long id, String borrowId, Long readerId, Long bookId, String status) {
        Borrow borrow = new Borrow();
        borrow.setId(id);
        borrow.setBorrowId(borrowId);
        borrow.setReaderId(readerId);
        borrow.setBookId(bookId);
        borrow.setBookBarcode("BC-" + bookId);
        borrow.setBorrowDate(LocalDateTime.now().minusDays(5));
        borrow.setDueDate(LocalDateTime.now().plusDays(25));
        borrow.setReturnDate(null);
        borrow.setRenewCount(0);
        borrow.setMaxRenewCount(2);
        borrow.setStatus(status);
        borrow.setFineAmount(BigDecimal.ZERO);
        borrow.setFinePaid(false);
        borrow.setFinePaidDate(null);
        borrow.setRemarks(null);
        borrow.setCreatedAt(LocalDateTime.now().minusDays(5));
        borrow.setUpdatedAt(LocalDateTime.now().minusDays(5));
        borrow.setDeletedAt(null);
        return borrow;
    }

    /**
     * 创建逾期的Borrow对象
     */
    private Borrow createOverdueBorrow(Long id, String borrowId, Long readerId, Long bookId, int overdueDays) {
        Borrow borrow = createTestBorrow(id, borrowId, readerId, bookId, "BORROWED");
        borrow.setBorrowDate(LocalDateTime.now().minusDays(30 + overdueDays));
        borrow.setDueDate(LocalDateTime.now().minusDays(overdueDays));
        return borrow;
    }

    /**
     * 创建测试用的ReaderDTO
     */
    private ReaderDTO createTestReader(Long id, String readerId, String name, String readerType, String status) {
        ReaderDTO reader = new ReaderDTO();
        reader.setId(id);
        reader.setReaderId(readerId);
        reader.setName(name);
        reader.setPhone("13800138000");
        reader.setEmail(name.toLowerCase() + "@example.com");
        reader.setReaderType(readerType);
        reader.setDepartment("计算机学院");
        reader.setMaxBorrowCount(10);
        reader.setMaxBorrowDays(30);
        reader.setStatus(status);
        reader.setExpiryDate(LocalDate.of(2025, 12, 31));
        return reader;
    }

    /**
     * 创建测试用的BookDTO
     */
    private BookDTO createTestBook(Long id, String isbn, String title, String author, int availableCopies) {
        BookDTO book = new BookDTO();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher("机械工业出版社");
        book.setBarcode("BC-" + id);
        book.setTotalCopies(10);
        book.setAvailableCopies(availableCopies);
        book.setStatus("AVAILABLE");
        return book;
    }

    /**
     * 创建测试用的BorrowRequest
     */
    private BorrowRequest createBorrowRequest(Long readerId, Long bookId) {
        BorrowRequest request = new BorrowRequest();
        request.setReaderId(readerId);
        request.setBookId(bookId);
        request.setBorrowDays(30);
        request.setRemark("测试借阅");
        return request;
    }

    /**
     * 创建测试用的ReturnRequest
     */
    private ReturnRequest createReturnRequest(Long borrowId, Boolean payFine) {
        ReturnRequest request = new ReturnRequest();
        request.setBorrowId(borrowId);
        request.setPayFine(payFine);
        request.setPaymentMethod("CASH");
        request.setRemarks("测试还书");
        return request;
    }

    /**
     * 创建测试用的RenewRequest
     */
    private RenewRequest createRenewRequest(Long borrowId, Integer renewDays) {
        RenewRequest request = new RenewRequest();
        request.setBorrowId(borrowId);
        request.setRenewDays(renewDays);
        request.setReason("需要继续阅读");
        return request;
    }

    /**
     * 创建测试用的BorrowQueryRequest
     */
    private BorrowQueryRequest createQueryRequest(Long readerId, Long bookId, String status) {
        BorrowQueryRequest request = new BorrowQueryRequest();
        request.setReaderId(readerId);
        request.setBookId(bookId);
        request.setStatus(status);
        request.setPageNum(1);
        request.setPageSize(10);
        return request;
    }

    // ==================== borrowBook() 借书操作测试 (4个) ====================

    @Test
    @DisplayName("借书 - 成功")
    void testBorrowBook_Success() {
        // Arrange
        BorrowRequest request = createBorrowRequest(1L, 1L);

        // Mock reader validation
        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));

        // Mock reader info
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));

        // Mock book availability check
        when(bookServiceClient.checkAvailability(1L))
                .thenReturn(Result.success(true));

        // Mock book info
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Mock borrow ID generation query
        when(borrowMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        // Mock decrease available copies
        when(bookServiceClient.decreaseAvailableCopies(1L))
                .thenReturn(Result.success(null));

        // Mock insert borrow record
        when(borrowMapper.insert(any(Borrow.class))).thenReturn(1);

        // Act
        BorrowDetailVO result = borrowService.borrowBook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReaderId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("BORROWED");
        assertThat(result.getRenewCount()).isEqualTo(0);
        assertThat(result.getMaxRenewCount()).isEqualTo(2);
        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getFinePaid()).isFalse();

        // Verify interactions
        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper, times(1)).insert(borrowCaptor.capture());
        Borrow capturedBorrow = borrowCaptor.getValue();
        assertThat(capturedBorrow.getBorrowId()).startsWith("BW-");
        assertThat(capturedBorrow.getStatus()).isEqualTo("BORROWED");
        assertThat(capturedBorrow.getRenewCount()).isEqualTo(0);

        verify(readerServiceClient, times(1)).validateReaderStatus(1L);
        verify(bookServiceClient, times(1)).checkAvailability(1L);
        verify(bookServiceClient, times(1)).decreaseAvailableCopies(1L);
    }

    @Test
    @DisplayName("借书 - 读者状态无效")
    void testBorrowBook_ReaderStatusInvalid() {
        // Arrange
        BorrowRequest request = createBorrowRequest(1L, 1L);

        // Mock reader validation failure
        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(false));

        // Act & Assert
        assertThatThrownBy(() -> borrowService.borrowBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("读者状态不允许借书");

        verify(borrowMapper, never()).insert(any(Borrow.class));
        verify(bookServiceClient, never()).decreaseAvailableCopies(anyLong());
    }

    @Test
    @DisplayName("借书 - 图书无可借副本")
    void testBorrowBook_BookUnavailable() {
        // Arrange
        BorrowRequest request = createBorrowRequest(1L, 1L);

        // Mock reader validation
        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));

        // Mock book unavailable
        when(bookServiceClient.checkAvailability(1L))
                .thenReturn(Result.success(false));

        // Act & Assert
        assertThatThrownBy(() -> borrowService.borrowBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图书暂无可借副本");

        verify(borrowMapper, never()).insert(any(Borrow.class));
        verify(bookServiceClient, never()).decreaseAvailableCopies(anyLong());
    }

    @Test
    @DisplayName("借书 - 减少图书可借数量失败")
    void testBorrowBook_DecreaseAvailableCopiesFails() {
        // Arrange
        BorrowRequest request = createBorrowRequest(1L, 1L);

        // Mock successful validation
        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.checkAvailability(1L))
                .thenReturn(Result.success(true));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));
        when(borrowMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        // Mock decrease failure
        when(bookServiceClient.decreaseAvailableCopies(1L))
                .thenReturn(Result.error("库存不足"));

        // Act & Assert
        assertThatThrownBy(() -> borrowService.borrowBook(request))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("减少图书可借数量失败");

        verify(borrowMapper, never()).insert(any(Borrow.class));
    }

    // ==================== returnBook() 还书操作测试 (3个) ====================

    @Test
    @DisplayName("还书 - 成功（无逾期）")
    void testReturnBook_Success_NoOverdue() {
        // Arrange
        Long borrowId = 1L;
        ReturnRequest request = createReturnRequest(borrowId, false);

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);
        when(bookServiceClient.increaseAvailableCopies(1L))
                .thenReturn(Result.success(null));
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);

        // Mock Feign client calls for BorrowDetailVO conversion
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Act
        BorrowDetailVO result = borrowService.returnBook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RETURNED");
        assertThat(result.getReturnDate()).isNotNull();
        assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getFinePaid()).isFalse();

        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper, times(1)).updateById(borrowCaptor.capture());
        Borrow capturedBorrow = borrowCaptor.getValue();
        assertThat(capturedBorrow.getStatus()).isEqualTo("RETURNED");
        assertThat(capturedBorrow.getReturnDate()).isNotNull();

        verify(bookServiceClient, times(1)).increaseAvailableCopies(1L);
    }

    @Test
    @DisplayName("还书 - 成功（有逾期罚金并支付）")
    void testReturnBook_Success_WithOverdueFine() {
        // Arrange
        Long borrowId = 1L;
        Borrow overdueBorrow = createOverdueBorrow(1L, "BW-20251028-0001", 1L, 1L, 5);
        ReturnRequest request = createReturnRequest(borrowId, true);

        when(borrowMapper.selectById(borrowId)).thenReturn(overdueBorrow);
        when(bookServiceClient.increaseAvailableCopies(1L))
                .thenReturn(Result.success(null));
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);

        // Mock Feign client calls for BorrowDetailVO conversion
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Act
        BorrowDetailVO result = borrowService.returnBook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RETURNED");
        assertThat(result.getReturnDate()).isNotNull();
        assertThat(result.getFineAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getFineAmount()).isEqualByComparingTo(new BigDecimal("5.00")); // 5 days * 1.00
        assertThat(result.getFinePaid()).isTrue();
        assertThat(result.getFinePaidDate()).isNotNull();

        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper, times(1)).updateById(borrowCaptor.capture());
        Borrow capturedBorrow = borrowCaptor.getValue();
        assertThat(capturedBorrow.getFinePaid()).isTrue();
        assertThat(capturedBorrow.getFineAmount()).isEqualByComparingTo(new BigDecimal("5.00"));

        verify(bookServiceClient, times(1)).increaseAvailableCopies(1L);
    }

    @Test
    @DisplayName("还书 - 图书已归还")
    void testReturnBook_AlreadyReturned() {
        // Arrange
        Long borrowId = 1L;
        Borrow returnedBorrow = createTestBorrow(1L, "BW-20251028-0001", 1L, 1L, "RETURNED");
        returnedBorrow.setReturnDate(LocalDateTime.now().minusDays(1));

        ReturnRequest request = createReturnRequest(borrowId, false);

        when(borrowMapper.selectById(borrowId)).thenReturn(returnedBorrow);

        // Act & Assert
        assertThatThrownBy(() -> borrowService.returnBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该图书已归还");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
        verify(bookServiceClient, never()).increaseAvailableCopies(anyLong());
    }

    // ==================== renewBook() 续借操作测试 (2个) ====================

    @Test
    @DisplayName("续借 - 成功")
    void testRenewBook_Success() {
        // Arrange
        Long borrowId = 1L;
        RenewRequest request = createRenewRequest(borrowId, 30);

        LocalDateTime originalDueDate = testBorrow.getDueDate();
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);

        // Mock Feign client calls for BorrowDetailVO conversion
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Act
        BorrowDetailVO result = borrowService.renewBook(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRenewCount()).isEqualTo(1);
        assertThat(result.getDueDate()).isAfter(originalDueDate);

        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper, times(1)).updateById(borrowCaptor.capture());
        Borrow capturedBorrow = borrowCaptor.getValue();
        assertThat(capturedBorrow.getRenewCount()).isEqualTo(1);
        assertThat(capturedBorrow.getDueDate()).isAfter(originalDueDate);
        assertThat(capturedBorrow.getRemarks()).contains("续借 30 天");
        assertThat(capturedBorrow.getRemarks()).contains("第 1 次续借");
    }

    @Test
    @DisplayName("续借 - 已达最大续借次数")
    void testRenewBook_MaxRenewCountReached() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setRenewCount(2); // Already renewed 2 times
        testBorrow.setMaxRenewCount(2); // Max is 2

        RenewRequest request = createRenewRequest(borrowId, 30);

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act & Assert
        assertThatThrownBy(() -> borrowService.renewBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已达到最大续借次数");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
    }

    // ==================== queryBorrows() 查询操作测试 (4个) ====================

    @Test
    @DisplayName("查询借阅记录 - 按读者ID筛选")
    void testQueryBorrows_ByReaderId() {
        // Arrange
        BorrowQueryRequest request = createQueryRequest(1L, null, null);
        Page<Borrow> resultPage = new Page<>(1, 10);
        resultPage.setRecords(Collections.singletonList(testBorrow));
        resultPage.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Act
        PageResult<BorrowVO> result = borrowService.queryBorrows(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getReaderId()).isEqualTo(1L);
        assertThat(result.getTotal()).isEqualTo(1);

        verify(borrowMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询借阅记录 - 按状态筛选")
    void testQueryBorrows_ByStatus() {
        // Arrange
        BorrowQueryRequest request = createQueryRequest(null, null, "BORROWED");
        Page<Borrow> resultPage = new Page<>(1, 10);

        Borrow borrow1 = createTestBorrow(1L, "BW-20251028-0001", 1L, 1L, "BORROWED");
        Borrow borrow2 = createTestBorrow(2L, "BW-20251028-0002", 2L, 2L, "BORROWED");
        resultPage.setRecords(Arrays.asList(borrow1, borrow2));
        resultPage.setTotal(2);

        when(borrowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);

        // Mock Feign client calls (service catches exceptions, so we can return null or throw)
        when(readerServiceClient.getReaderById(anyLong()))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(anyLong()))
                .thenReturn(Result.success(testBook));

        // Act
        PageResult<BorrowVO> result = borrowService.queryBorrows(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords()).allMatch(vo -> vo.getStatus().equals("BORROWED"));
        assertThat(result.getTotal()).isEqualTo(2);

        verify(borrowMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询借阅记录 - 仅查询逾期")
    void testQueryBorrows_OverdueOnly() {
        // Arrange
        BorrowQueryRequest request = new BorrowQueryRequest();
        request.setOverdueOnly(true);
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Borrow> resultPage = new Page<>(1, 10);
        Borrow overdueBorrow = createOverdueBorrow(1L, "BW-20251028-0001", 1L, 1L, 5);
        resultPage.setRecords(Collections.singletonList(overdueBorrow));
        resultPage.setTotal(1);

        when(borrowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);

        // Mock Feign client calls
        when(readerServiceClient.getReaderById(anyLong()))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(anyLong()))
                .thenReturn(Result.success(testBook));

        // Act
        PageResult<BorrowVO> result = borrowService.queryBorrows(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);

        verify(borrowMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询借阅记录 - 空结果")
    void testQueryBorrows_EmptyResult() {
        // Arrange
        BorrowQueryRequest request = createQueryRequest(999L, null, null);
        Page<Borrow> resultPage = new Page<>(1, 10);
        resultPage.setRecords(Collections.emptyList());
        resultPage.setTotal(0);

        when(borrowMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(resultPage);

        // Act
        PageResult<BorrowVO> result = borrowService.queryBorrows(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);

        verify(borrowMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== getBorrowById() 查询详情测试 (2个) ====================

    @Test
    @DisplayName("根据ID获取借阅详情 - 成功")
    void testGetBorrowById_Success() {
        // Arrange
        Long borrowId = 1L;
        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(1L))
                .thenReturn(Result.success(testBook));

        // Act
        BorrowDetailVO result = borrowService.getBorrowById(borrowId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBorrowId()).isEqualTo("BW-20251028-0001");
        assertThat(result.getReaderId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("BORROWED");

        verify(borrowMapper, times(1)).selectById(borrowId);
    }

    @Test
    @DisplayName("根据ID获取借阅详情 - 不存在")
    void testGetBorrowById_NotFound() {
        // Arrange
        Long borrowId = 999L;
        when(borrowMapper.selectById(borrowId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> borrowService.getBorrowById(borrowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借阅记录不存在");

        verify(borrowMapper, times(1)).selectById(borrowId);
    }

    // ==================== getOverdueBorrows() 逾期查询测试 (1个) ====================

    @Test
    @DisplayName("获取逾期借阅记录 - 成功")
    void testGetOverdueBorrows_Success() {
        // Arrange
        Borrow overdueBorrow1 = createOverdueBorrow(1L, "BW-20251028-0001", 1L, 1L, 3);
        Borrow overdueBorrow2 = createOverdueBorrow(2L, "BW-20251028-0002", 2L, 2L, 7);

        when(borrowMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(overdueBorrow1, overdueBorrow2));

        // Mock Feign client calls
        when(readerServiceClient.getReaderById(anyLong()))
                .thenReturn(Result.success(testReader));
        when(bookServiceClient.getBookById(anyLong()))
                .thenReturn(Result.success(testBook));

        // Act
        List<BorrowVO> result = borrowService.getOverdueBorrows();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(borrowMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== updateOverdueStatus() 状态更新测试 (1个) ====================

    @Test
    @DisplayName("批量更新逾期状态 - 成功")
    void testUpdateOverdueStatus_Success() {
        // Arrange
        Borrow overdueBorrow1 = createOverdueBorrow(1L, "BW-20251028-0001", 1L, 1L, 3);
        overdueBorrow1.setStatus("BORROWED"); // Still marked as BORROWED

        Borrow overdueBorrow2 = createOverdueBorrow(2L, "BW-20251028-0002", 2L, 2L, 7);
        overdueBorrow2.setStatus("BORROWED"); // Still marked as BORROWED

        when(borrowMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(overdueBorrow1, overdueBorrow2));
        when(borrowMapper.updateById(any(Borrow.class))).thenReturn(1);

        // Act
        borrowService.updateOverdueStatus();

        // Assert
        ArgumentCaptor<Borrow> borrowCaptor = ArgumentCaptor.forClass(Borrow.class);
        verify(borrowMapper, times(2)).updateById(borrowCaptor.capture());

        List<Borrow> capturedBorrows = borrowCaptor.getAllValues();
        assertThat(capturedBorrows).hasSize(2);
        assertThat(capturedBorrows).allMatch(b -> b.getStatus().equals("OVERDUE"));

        verify(borrowMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== 额外业务逻辑测试 (3个) ====================

    @Test
    @DisplayName("续借 - 状态非BORROWED无法续借")
    void testRenewBook_InvalidStatus() {
        // Arrange
        Long borrowId = 1L;
        testBorrow.setStatus("RETURNED");

        RenewRequest request = createRenewRequest(borrowId, 30);

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Act & Assert
        assertThatThrownBy(() -> borrowService.renewBook(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有借阅中的图书才能续借");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
    }

    @Test
    @DisplayName("借书 - 无法获取读者信息")
    void testBorrowBook_CannotGetReaderInfo() {
        // Arrange
        BorrowRequest request = createBorrowRequest(1L, 1L);

        when(readerServiceClient.validateReaderStatus(1L))
                .thenReturn(Result.success(true));

        // Mock reader info retrieval failure
        when(readerServiceClient.getReaderById(1L))
                .thenReturn(Result.error("服务不可用"));

        // Act & Assert
        assertThatThrownBy(() -> borrowService.borrowBook(request))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法获取读者信息");

        verify(borrowMapper, never()).insert(any(Borrow.class));
    }

    @Test
    @DisplayName("还书 - 增加图书可借数量失败")
    void testReturnBook_IncreaseAvailableCopiesFails() {
        // Arrange
        Long borrowId = 1L;
        ReturnRequest request = createReturnRequest(borrowId, false);

        when(borrowMapper.selectById(borrowId)).thenReturn(testBorrow);

        // Mock increase failure
        when(bookServiceClient.increaseAvailableCopies(1L))
                .thenReturn(Result.error("系统异常"));

        // Act & Assert
        assertThatThrownBy(() -> borrowService.returnBook(request))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("增加图书可借数量失败");

        verify(borrowMapper, never()).updateById(any(Borrow.class));
    }
}
