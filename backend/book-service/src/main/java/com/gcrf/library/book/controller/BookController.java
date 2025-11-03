package com.gcrf.library.book.controller;

import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.book.service.BookService;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 图书控制器
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "图书管理", description = "图书的增删改查、搜索等接口")
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询图书
     */
    @GetMapping
    @Operation(summary = "分页查询图书", description = "支持关键词搜索、分类筛选等")
    public Result<PageResult<BookVO>> queryBooks(@Valid BookQueryRequest request) {
        log.info("分页查询图书请求: {}", request);
        PageResult<BookVO> result = bookService.queryBooks(request);
        return Result.success(result);
    }

    /**
     * 根据ID查询图书详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询图书详情", description = "根据图书ID查询详细信息")
    public Result<BookDetailVO> getBook(@PathVariable Long id) {
        log.info("查询图书详情: id={}", id);
        BookDetailVO book = bookService.getBookById(id);
        return Result.success(book);
    }

    /**
     * 创建图书
     */
    @PostMapping
    @Operation(summary = "创建图书", description = "新增图书信息")
    public Result<BookDetailVO> createBook(@Valid @RequestBody BookCreateRequest request) {
        log.info("创建图书: {}", request.getTitle());
        BookDetailVO result = bookService.createBook(request);
        return Result.success(result);
    }

    /**
     * 更新图书
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新图书", description = "修改图书信息")
    public Result<BookDetailVO> updateBook(@PathVariable Long id, @Valid @RequestBody BookUpdateRequest request) {
        log.info("更新图书: id={}", id);
        // 确保路径参数和请求体的ID一致
        request.setId(id);
        BookDetailVO result = bookService.updateBook(request);
        return Result.success(result);
    }

    /**
     * 删除图书
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除图书", description = "删除图书（逻辑删除）")
    public Result<Void> deleteBook(@PathVariable Long id) {
        log.info("删除图书: id={}", id);
        bookService.deleteBook(id);
        return Result.success();
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查图书服务是否正常运行")
    public Result<String> health() {
        return Result.success("Book Service is running");
    }
}
