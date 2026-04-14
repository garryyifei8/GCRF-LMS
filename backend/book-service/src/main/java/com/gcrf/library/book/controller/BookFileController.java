package com.gcrf.library.book.controller;

import com.gcrf.library.book.service.FileStorageService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * 图书文件管理控制器
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "图书文件管理", description = "图书封面和PDF文件的上传下载管理")
public class BookFileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传图书封面
     */
    @PostMapping("/{id}/cover")
    @Operation(summary = "上传图书封面", description = "上传JPG/PNG格式图片，最大5MB")
    public Result<String> uploadBookCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        log.info("上传图书封面: bookId={}, fileName={}, size={}", id, file.getOriginalFilename(), file.getSize());

        String coverUrl = fileStorageService.uploadBookCover(id, file);
        return Result.success(coverUrl);
    }

    /**
     * 上传图书PDF
     */
    @PostMapping("/{id}/pdf")
    @Operation(summary = "上传图书PDF", description = "上传PDF文件，最大50MB")
    public Result<Map<String, Object>> uploadBookPdf(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        log.info("上传图书PDF: bookId={}, fileName={}, size={}", id, file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = fileStorageService.uploadBookPdf(id, file);
        return Result.success(result);
    }

    /**
     * 下载图书PDF
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载图书PDF", description = "下载图书PDF文件")
    public ResponseEntity<InputStreamResource> downloadBookPdf(@PathVariable Long id) {

        log.info("下载图书PDF: bookId={}", id);

        InputStream inputStream = fileStorageService.downloadBookPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"book_" + id + ".pdf\"")
                .body(new InputStreamResource(inputStream));
    }

    /**
     * 删除图书封面
     */
    @DeleteMapping("/{id}/cover")
    @Operation(summary = "删除图书封面", description = "删除已上传的图书封面")
    public Result<Void> deleteBookCover(@PathVariable Long id) {

        log.info("删除图书封面: bookId={}", id);

        fileStorageService.deleteBookCover(id);
        return Result.success();
    }

    /**
     * 删除图书PDF
     */
    @DeleteMapping("/{id}/pdf")
    @Operation(summary = "删除图书PDF", description = "删除已上传的PDF文件")
    public Result<Void> deleteBookPdf(@PathVariable Long id) {

        log.info("删除图书PDF: bookId={}", id);

        fileStorageService.deleteBookPdf(id);
        return Result.success();
    }
}
