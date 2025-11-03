package com.gcrf.library.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.book.service.impl.BookServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookService单元测试
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    private BookService bookService;

    private Book testBook;
    private BookCreateRequest createRequest;
    private BookUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Manually create BookServiceImpl instance with mocked BookMapper
        bookService = new BookServiceImpl(bookMapper);

        // 创建测试图书实体
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111123456");
        testBook.setTitle("Java核心技术");
        testBook.setAuthor("Cay S. Horstmann");
        testBook.setPublisher("机械工业出版社");
        testBook.setPublishDate(LocalDate.of(2023, 1, 1));
        testBook.setPages(800);
        testBook.setPrice(new BigDecimal("99.00"));
        testBook.setLanguage("中文");
        testBook.setTotalQuantity(10);
        testBook.setAvailableQuantity(10);
        testBook.setStatus("ACTIVE");
        testBook.setCreatedAt(LocalDateTime.now());
        testBook.setUpdatedAt(LocalDateTime.now());

        // 创建图书请求DTO
        createRequest = new BookCreateRequest();
        createRequest.setIsbn("9787111999999");
        createRequest.setTitle("新书");
        createRequest.setAuthor("作者");
        createRequest.setPublisher("出版社");
        createRequest.setTotalQuantity(5);
        createRequest.setStatus("ACTIVE");

        // 更新图书请求DTO
        updateRequest = new BookUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setIsbn("9787111123456");
        updateRequest.setTitle("Java核心技术（第11版）");
        updateRequest.setAuthor("Cay S. Horstmann");
        updateRequest.setPublisher("机械工业出版社");
        updateRequest.setTotalQuantity(15);
        updateRequest.setAvailableQuantity(15);
        updateRequest.setStatus("ACTIVE");
    }

    @Test
    void testQueryBooks_Success() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testBook));
        mockPage.setTotal(1L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(testBook.getTitle(), result.getRecords().get(0).getTitle());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryBooks_WithKeyword() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setKeyword("Java");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testBook));
        mockPage.setTotal(1L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryBooks_WithAuthor() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setAuthor("Horstmann");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testBook));
        mockPage.setTotal(1L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryBooks_WithPublisher() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setPublisher("机械工业出版社");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testBook));
        mockPage.setTotal(1L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryBooks_WithStatus() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setStatus(1); // ACTIVE
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(testBook));
        mockPage.setTotal(1L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryBooks_EmptyResult() {
        // Arrange
        BookQueryRequest request = new BookQueryRequest();
        request.setKeyword("nonexistent");
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0L);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // Act
        PageResult<BookVO> result = bookService.queryBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getRecords().size());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetBookById_Success() {
        // Arrange
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act
        BookDetailVO result = bookService.getBookById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testBook.getId(), result.getId());
        assertEquals(testBook.getTitle(), result.getTitle());
        assertEquals(testBook.getAuthor(), result.getAuthor());
        verify(bookMapper).selectById(1L);
    }

    @Test
    void testGetBookById_NotFound() {
        // Arrange
        when(bookMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.getBookById(999L);
        });

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("图书不存在", exception.getMessage());
        verify(bookMapper).selectById(999L);
    }

    @Test
    void testCreateBook_Success() {
        // Arrange
        when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(bookMapper.insert(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(1L);
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());
            return 1;
        });

        // Act
        BookDetailVO result = bookService.createBook(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(createRequest.getTitle(), result.getTitle());
        assertEquals(createRequest.getTotalQuantity(), result.getAvailableQuantity()); // Should be initialized
        verify(bookMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(bookMapper).insert(any(Book.class));
    }

    @Test
    void testCreateBook_DuplicateISBN() {
        // Arrange
        when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.createBook(createRequest);
        });

        assertEquals(ResultCode.DUPLICATE_DATA.getCode(), exception.getCode());
        assertEquals("ISBN已存在", exception.getMessage());
        verify(bookMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    void testUpdateBook_Success() {
        // Arrange
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateById(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            testBook.setTitle(book.getTitle());
            testBook.setTotalQuantity(book.getTotalQuantity());
            testBook.setAvailableQuantity(book.getAvailableQuantity());
            return 1;
        });

        // Act
        BookDetailVO result = bookService.updateBook(updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(updateRequest.getTitle(), result.getTitle());
        verify(bookMapper, atLeastOnce()).selectById(1L);
        verify(bookMapper).updateById(any(Book.class));
    }

    @Test
    void testUpdateBook_NotFound() {
        // Arrange
        when(bookMapper.selectById(999L)).thenReturn(null);
        updateRequest.setId(999L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.updateBook(updateRequest);
        });

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        verify(bookMapper).selectById(999L);
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void testUpdateBook_WithNewISBN_NoDuplicate() {
        // Arrange
        updateRequest.setIsbn("9787111777777"); // New ISBN
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(bookMapper.updateById(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            testBook.setIsbn(book.getIsbn());
            testBook.setTitle(book.getTitle());
            return 1;
        });

        // Act
        BookDetailVO result = bookService.updateBook(updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(updateRequest.getIsbn(), result.getIsbn());
        verify(bookMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(bookMapper).updateById(any(Book.class));
    }

    @Test
    void testUpdateBook_WithNewISBN_Duplicate() {
        // Arrange
        updateRequest.setIsbn("9787111999999"); // Different ISBN
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.updateBook(updateRequest);
        });

        assertEquals(ResultCode.DUPLICATE_DATA.getCode(), exception.getCode());
        assertEquals("ISBN已存在", exception.getMessage());
        verify(bookMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void testDeleteBook_Success() {
        // Arrange
        testBook.setAvailableQuantity(10);
        testBook.setTotalQuantity(10);

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.deleteById(1L)).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> bookService.deleteBook(1L));

        // Assert
        verify(bookMapper).selectById(1L);
        verify(bookMapper).deleteById(1L);
    }

    @Test
    void testDeleteBook_NotFound() {
        // Arrange
        when(bookMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.deleteBook(999L);
        });

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        verify(bookMapper).selectById(999L);
        verify(bookMapper, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteBook_HasActiveCirculation() {
        // Arrange
        testBook.setAvailableQuantity(8);
        testBook.setTotalQuantity(10); // 2 books are borrowed

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.deleteBook(1L);
        });

        assertEquals(ResultCode.OPERATION_FAILED.getCode(), exception.getCode());
        assertEquals("该图书有在借记录，无法删除", exception.getMessage());
        verify(bookMapper).selectById(1L);
        verify(bookMapper, never()).deleteById(anyLong());
    }

    @Test
    void testDecreaseAvailableQuantity_Success() {
        // Arrange
        testBook.setAvailableQuantity(10);

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> bookService.decreaseAvailableQuantity(1L));

        // Assert
        assertEquals(9, testBook.getAvailableQuantity());
        verify(bookMapper).selectById(1L);
        verify(bookMapper).updateById(testBook);
    }

    @Test
    void testDecreaseAvailableQuantity_InsufficientStock() {
        // Arrange
        testBook.setAvailableQuantity(0);

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.decreaseAvailableQuantity(1L);
        });

        assertEquals(ResultCode.OPERATION_FAILED.getCode(), exception.getCode());
        assertEquals("图书已全部借出", exception.getMessage());
        verify(bookMapper).selectById(1L);
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void testDecreaseAvailableQuantity_BookNotFound() {
        // Arrange
        when(bookMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.decreaseAvailableQuantity(999L);
        });

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        verify(bookMapper).selectById(999L);
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void testIncreaseAvailableQuantity_Success() {
        // Arrange
        testBook.setAvailableQuantity(8);
        testBook.setTotalQuantity(10);

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> bookService.increaseAvailableQuantity(1L));

        // Assert
        assertEquals(9, testBook.getAvailableQuantity());
        verify(bookMapper).selectById(1L);
        verify(bookMapper).updateById(testBook);
    }

    @Test
    void testIncreaseAvailableQuantity_ExceedsTotal() {
        // Arrange
        testBook.setAvailableQuantity(10);
        testBook.setTotalQuantity(10);

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.increaseAvailableQuantity(1L);
        });

        assertEquals(ResultCode.OPERATION_FAILED.getCode(), exception.getCode());
        assertEquals("可借数量异常", exception.getMessage());
        verify(bookMapper).selectById(1L);
        verify(bookMapper, never()).updateById(any(Book.class));
    }

    @Test
    void testIncreaseAvailableQuantity_BookNotFound() {
        // Arrange
        when(bookMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bookService.increaseAvailableQuantity(999L);
        });

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        verify(bookMapper).selectById(999L);
        verify(bookMapper, never()).updateById(any(Book.class));
    }
}
