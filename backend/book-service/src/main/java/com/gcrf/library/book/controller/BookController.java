package com.gcrf.library.book.controller;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BarcodeGenerateRequest;
import com.gcrf.library.book.dto.request.BatchDeleteRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BatchOperationResult;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.book.dto.response.BarcodeVO;
import com.gcrf.library.book.dto.response.IsbnLookupVO;

import java.util.List;
import com.gcrf.library.book.service.BookService;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;

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
     * 检查图书可借状态（供流通服务 Feign 调用）
     */
    @GetMapping("/{bookId}/availability")
    @Operation(summary = "检查图书可借状态", description = "检查指定图书是否有可借副本，供流通服务调用")
    public Result<Boolean> checkAvailability(@PathVariable Long bookId) {
        log.info("检查图书可借状态: bookId={}", bookId);
        boolean available = bookService.checkAvailability(bookId);
        return Result.success(available);
    }

    /**
     * 减少图书可借数量（借书时调用，供流通服务 Feign 调用）
     */
    @PostMapping("/{bookId}/decrease-copies")
    @Operation(summary = "减少可借数量", description = "借书时减少图书可借副本数量，供流通服务调用")
    public Result<Void> decreaseAvailableCopies(@PathVariable Long bookId) {
        log.info("减少图书可借数量: bookId={}", bookId);
        bookService.decreaseAvailableQuantity(bookId);
        return Result.success();
    }

    /**
     * 增加图书可借数量（还书时调用，供流通服务 Feign 调用）
     */
    @PostMapping("/{bookId}/increase-copies")
    @Operation(summary = "增加可借数量", description = "还书时增加图书可借副本数量，供流通服务调用")
    public Result<Void> increaseAvailableCopies(@PathVariable Long bookId) {
        log.info("增加图书可借数量: bookId={}", bookId);
        bookService.increaseAvailableQuantity(bookId);
        return Result.success();
    }

    /**
     * 获取库存信息
     */
    @GetMapping("/{id}/inventory")
    @Operation(summary = "获取库存信息", description = "查询指定图书的库存信息")
    public Result<com.gcrf.library.book.dto.response.InventoryVO> getInventory(@PathVariable Long id) {
        log.info("获取库存信息: bookId={}", id);
        com.gcrf.library.book.dto.response.InventoryVO inventory = bookService.getInventory(id);
        return Result.success(inventory);
    }

    /**
     * 更新库存
     */
    @PutMapping("/{id}/inventory")
    @Operation(summary = "更新库存", description = "调整图书库存数量")
    public Result<com.gcrf.library.book.dto.response.InventoryVO> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody com.gcrf.library.book.dto.request.InventoryUpdateRequest request) {
        log.info("更新库存: bookId={}", id);
        com.gcrf.library.book.dto.response.InventoryVO inventory = bookService.updateInventory(id, request);
        return Result.success(inventory);
    }

    /**
     * 全文搜索图书
     */
    @PostMapping("/search")
    @Operation(summary = "全文搜索图书", description = "使用关键词搜索图书")
    public Result<PageResult<BookVO>> searchBooks(@Valid @RequestBody com.gcrf.library.book.dto.request.BookSearchRequest request) {
        log.info("全文搜索图书: query={}", request.getQuery());
        PageResult<BookVO> result = bookService.searchBooks(request);
        return Result.success(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查图书服务是否正常运行")
    public Result<String> health() {
        return Result.success("Book Service is running");
    }

    /**
     * 批量删除图书
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除图书", description = "批量删除图书（逻辑删除）")
    public Result<BatchOperationResult> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        log.info("批量删除图书: count={}", request.getIds().size());
        BatchOperationResult result = bookService.batchDelete(request.getIds());
        return Result.success(result);
    }

    /**
     * 批量导入图书
     */
    @PostMapping(value = "/batch-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入图书", description = "通过Excel文件批量导入图书")
    public Result<BatchOperationResult> batchImport(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("批量导入图书: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return Result.error("请上传文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error("请上传Excel文件(.xlsx或.xls)");
        }

        BatchOperationResult result = bookService.batchImport(file.getInputStream());
        return Result.success(result);
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/import-template")
    @Operation(summary = "下载导入模板", description = "下载图书批量导入的Excel模板")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        log.info("下载导入模板");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("图书导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        bookService.downloadImportTemplate(response.getOutputStream());
    }

    /**
     * 通过ISBN查询图书信息
     */
    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "ISBN查询", description = "通过ISBN从第三方API查询图书信息")
    public Result<IsbnLookupVO> lookupByIsbn(@PathVariable String isbn) {
        log.info("通过ISBN查询图书信息: {}", isbn);
        IsbnLookupVO result = bookService.lookupByIsbn(isbn);
        return Result.success(result);
    }

    /**
     * 批量生成条码
     */
    @PostMapping("/barcode/generate")
    @Operation(summary = "批量生成条码", description = "为指定的图书批量生成条码")
    public Result<List<BarcodeVO>> generateBarcodes(@Valid @RequestBody BarcodeGenerateRequest request) {
        log.info("批量生成条码: count={}", request.getBookIds().size());
        List<BarcodeVO> result = bookService.generateBarcodes(request.getBookIds(), request.getPrefix());
        return Result.success(result);
    }

    /**
     * 根据条码查询图书
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "条码查询", description = "根据条码查询图书信息")
    public Result<BookDetailVO> findByBarcode(@PathVariable String barcode) {
        log.info("根据条码查询图书: {}", barcode);
        BookDetailVO result = bookService.findByBarcode(barcode);
        return Result.success(result);
    }
}
