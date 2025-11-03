package com.gcrf.library.book.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BookController集成测试
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookMapper bookMapper;

    private Book testBook;

    @BeforeEach
    void setUp() {
        // Clean up existing test data
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsbn, "9787111111111");
        bookMapper.delete(queryWrapper);

        // Create test book
        testBook = new Book();
        testBook.setIsbn("9787111111111");
        testBook.setTitle("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setPublisher("测试出版社");
        testBook.setPublishDate(LocalDate.of(2023, 1, 1));
        testBook.setPages(500);
        testBook.setPrice(new BigDecimal("88.00"));
        testBook.setLanguage("中文");
        testBook.setTotalQuantity(10);
        testBook.setAvailableQuantity(10);
        testBook.setStatus("ACTIVE");
        testBook.setCreatedAt(LocalDateTime.now());
        testBook.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(testBook);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsbn, "9787111111111")
                .or().eq(Book::getIsbn, "9787111222222")
                .or().eq(Book::getIsbn, "9787111333333")
                .or().eq(Book::getIsbn, "9787111444444");
        bookMapper.delete(queryWrapper);
    }

    @Test
    void testQueryBooks_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void testQueryBooks_WithKeyword() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books")
                        .param("keyword", "测试")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryBooks_WithAuthor() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books")
                        .param("author", "测试作者")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryBooks_WithPublisher() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books")
                        .param("publisher", "测试出版社")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testQueryBooks_WithStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books")
                        .param("status", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void testGetBookById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books/{id}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testBook.getId()))
                .andExpect(jsonPath("$.data.isbn").value("9787111111111"))
                .andExpect(jsonPath("$.data.title").value("测试图书"))
                .andExpect(jsonPath("$.data.author").value("测试作者"));
    }

    @Test
    void testGetBookById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testCreateBook_Success() throws Exception {
        // Arrange
        BookCreateRequest request = new BookCreateRequest();
        request.setIsbn("9787111222222");
        request.setTitle("新测试图书");
        request.setAuthor("新作者");
        request.setPublisher("新出版社");
        request.setPublishDate(LocalDate.of(2024, 1, 1));
        request.setPages(600);
        request.setPrice(new BigDecimal("99.00"));
        request.setLanguage("中文");
        request.setTotalQuantity(5);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isbn").value("9787111222222"))
                .andExpect(jsonPath("$.data.title").value("新测试图书"))
                .andExpect(jsonPath("$.data.availableQuantity").value(5));
    }

    @Test
    void testCreateBook_DuplicateISBN() throws Exception {
        // Arrange
        BookCreateRequest request = new BookCreateRequest();
        request.setIsbn("9787111111111"); // Same as testBook
        request.setTitle("重复图书");
        request.setAuthor("作者");
        request.setPublisher("出版社");
        request.setTotalQuantity(5);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testUpdateBook_Success() throws Exception {
        // Arrange
        BookUpdateRequest request = new BookUpdateRequest();
        request.setId(testBook.getId());
        request.setIsbn("9787111111111");
        request.setTitle("更新后的标题");
        request.setAuthor("更新后的作者");
        request.setPublisher("更新后的出版社");
        request.setPublishDate(LocalDate.of(2024, 6, 1));
        request.setPages(600);
        request.setPrice(new BigDecimal("108.00"));
        request.setLanguage("中文");
        request.setTotalQuantity(15);
        request.setAvailableQuantity(15);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(put("/api/v1/books/{id}", testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("更新后的标题"))
                .andExpect(jsonPath("$.data.author").value("更新后的作者"));
    }

    @Test
    void testUpdateBook_NotFound() throws Exception {
        // Arrange
        BookUpdateRequest request = new BookUpdateRequest();
        request.setId(999999L);
        request.setIsbn("9787111333333");
        request.setTitle("不存在的图书");
        request.setTotalQuantity(5);
        request.setAvailableQuantity(5);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(put("/api/v1/books/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testUpdateBook_WithNewISBN_Duplicate() throws Exception {
        // Arrange - Create another book first
        Book anotherBook = new Book();
        anotherBook.setIsbn("9787111333333");
        anotherBook.setTitle("另一本书");
        anotherBook.setAuthor("另一个作者");
        anotherBook.setPublisher("另一个出版社");
        anotherBook.setTotalQuantity(5);
        anotherBook.setAvailableQuantity(5);
        anotherBook.setStatus("ACTIVE");
        anotherBook.setCreatedAt(LocalDateTime.now());
        anotherBook.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(anotherBook);

        // Try to update testBook with ISBN of anotherBook
        BookUpdateRequest request = new BookUpdateRequest();
        request.setId(testBook.getId());
        request.setIsbn("9787111333333"); // Duplicate ISBN
        request.setTitle("尝试重复");
        request.setTotalQuantity(5);
        request.setAvailableQuantity(5);
        request.setStatus("ACTIVE");

        // Act & Assert
        mockMvc.perform(put("/api/v1/books/{id}", testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testDeleteBook_Success() throws Exception {
        // Arrange - Create a new book for deletion
        Book bookToDelete = new Book();
        bookToDelete.setIsbn("9787111444444");
        bookToDelete.setTitle("待删除图书");
        bookToDelete.setAuthor("作者");
        bookToDelete.setPublisher("出版社");
        bookToDelete.setTotalQuantity(5);
        bookToDelete.setAvailableQuantity(5);
        bookToDelete.setStatus("ACTIVE");
        bookToDelete.setCreatedAt(LocalDateTime.now());
        bookToDelete.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(bookToDelete);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/books/{id}", bookToDelete.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify deletion - should return 404
        mockMvc.perform(get("/api/v1/books/{id}", bookToDelete.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testDeleteBook_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/books/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testDeleteBook_HasActiveCirculation() throws Exception {
        // Arrange - Modify test book to have borrowed copies
        testBook.setAvailableQuantity(5); // 5 out of 10 are borrowed
        bookMapper.updateById(testBook);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/books/{id}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testHealth_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/books/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Book Service is running"));
    }
}
