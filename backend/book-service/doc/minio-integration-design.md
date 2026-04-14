# MinIO Integration Design for Book Service

**Service**: Book Service - File Storage Integration
**Date**: 2025-11-03
**Version**: 1.0.0
**Priority**: CRITICAL ⭐

---

## Executive Summary

This document outlines the complete design for integrating MinIO object storage into the Book Service for managing book cover images and PDF files. The integration will provide scalable, secure, and performant file storage capabilities.

---

## 1. Architecture Overview

### 1.1 High-Level Design

```
┌─────────────────────────────────────────────────────────┐
│                     Client Layer                         │
│                  (Web Admin / Mobile App)                │
└─────────────────┬───────────────────────────────────────┘
                  │ HTTP/HTTPS
                  ▼
┌─────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                    │
│                 (Authentication, Routing)                │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│                   Book Service (8082)                    │
│  ┌─────────────────────────────────────────────────┐   │
│  │              FileController                      │   │
│  │  • POST /books/{id}/cover                       │   │
│  │  • POST /books/{id}/pdf                         │   │
│  │  • GET /books/{id}/download                     │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      ▼                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │           FileStorageService                     │   │
│  │  • Upload validation                             │   │
│  │  • Virus scanning (future)                      │   │
│  │  • Metadata management                          │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      ▼                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │              MinioClient                         │   │
│  │  • S3 API operations                            │   │
│  │  • Pre-signed URLs                              │   │
│  └───────────────────┬─────────────────────────────┘   │
└──────────────────────┼───────────────────────────────────┘
                       │ S3 Protocol
                       ▼
┌─────────────────────────────────────────────────────────┐
│                 MinIO Server (9000/9001)                 │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Buckets:                                         │   │
│  │  • library-covers (public read)                 │   │
│  │  • library-pdfs (authenticated)                 │   │
│  │  • library-temp (pre-upload staging)            │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Component Responsibilities

| Component | Responsibility | Key Features |
|-----------|---------------|--------------|
| FileController | HTTP endpoint handling | Multipart upload, validation, response |
| FileStorageService | Business logic | File validation, naming, metadata |
| MinioService | MinIO operations | Upload, download, delete, URL generation |
| MinioClient | S3 API client | Low-level S3 operations |
| FileValidator | Validation | Type, size, content validation |
| FileMetadataService | Metadata | Track uploads, versions, audit |

---

## 2. Detailed Component Design

### 2.1 MinIO Configuration

```java
package com.gcrf.library.book.config;

import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.secure:false}")
    private boolean secure;

    @Value("${minio.bucket.covers:library-covers}")
    private String coversBucket;

    @Value("${minio.bucket.pdfs:library-pdfs}")
    private String pdfsBucket;

    @Value("${minio.bucket.temp:library-temp}")
    private String tempBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint, 9000, secure)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void initializeBuckets() {
        try {
            // Create buckets if not exist
            createBucketIfNotExists(coversBucket, true);  // Public read
            createBucketIfNotExists(pdfsBucket, false);    // Private
            createBucketIfNotExists(tempBucket, false);     // Private

            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO buckets", e);
        }
    }

    private void createBucketIfNotExists(String bucketName, boolean publicRead) {
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                );

                if (publicRead) {
                    String policy = generatePublicReadPolicy(bucketName);
                    client.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
                    );
                }

                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket: {}", bucketName, e);
        }
    }

    private String generatePublicReadPolicy(String bucketName) {
        return """
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }
                ]
            }
            """.formatted(bucketName);
    }
}
```

### 2.2 File Storage Service Interface

```java
package com.gcrf.library.book.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;

public interface FileStorageService {

    /**
     * Upload book cover image
     * @param bookId Book ID
     * @param file Image file (JPG/PNG, max 5MB)
     * @return Cover URL
     */
    String uploadBookCover(Long bookId, MultipartFile file);

    /**
     * Upload book PDF file
     * @param bookId Book ID
     * @param file PDF file (max 50MB)
     * @return PDF metadata including URL
     */
    Map<String, Object> uploadBookPdf(Long bookId, MultipartFile file);

    /**
     * Get book PDF download stream
     * @param bookId Book ID
     * @return InputStream for download
     */
    InputStream downloadBookPdf(Long bookId);

    /**
     * Delete book cover image
     * @param bookId Book ID
     */
    void deleteBookCover(Long bookId);

    /**
     * Delete book PDF file
     * @param bookId Book ID
     */
    void deleteBookPdf(Long bookId);

    /**
     * Get pre-signed URL for temporary access
     * @param bucketName Bucket name
     * @param objectName Object name
     * @param expiryHours Expiry in hours
     * @return Pre-signed URL
     */
    String getPresignedUrl(String bucketName, String objectName, int expiryHours);

    /**
     * Check if file exists
     * @param bucketName Bucket name
     * @param objectName Object name
     * @return true if exists
     */
    boolean fileExists(String bucketName, String objectName);
}
```

### 2.3 File Storage Service Implementation

```java
package com.gcrf.library.book.service.impl;

import com.gcrf.library.book.service.FileStorageService;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.common.exception.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final BookMapper bookMapper;
    private final Tika tika = new Tika();

    @Value("${minio.bucket.covers:library-covers}")
    private String coversBucket;

    @Value("${minio.bucket.pdfs:library-pdfs}")
    private String pdfsBucket;

    @Value("${minio.max-size.cover:5242880}") // 5MB
    private long maxCoverSize;

    @Value("${minio.max-size.pdf:52428800}") // 50MB
    private long maxPdfSize;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/jpg"
    );

    private static final Set<String> ALLOWED_PDF_TYPES = Set.of(
        "application/pdf"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadBookCover(Long bookId, MultipartFile file) {
        log.info("Uploading cover for book: {}", bookId);

        // Validate book exists
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // Validate file
        validateImageFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String objectName = String.format("book_%d_%s.%s",
                bookId,
                UUID.randomUUID().toString().substring(0, 8),
                extension
            );

            // Delete old cover if exists
            if (book.getCoverUrl() != null) {
                deleteOldFile(coversBucket, extractObjectName(book.getCoverUrl()));
            }

            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(coversBucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .userMetadata(Map.of(
                        "book-id", String.valueOf(bookId),
                        "upload-time", LocalDateTime.now().toString(),
                        "original-name", originalFilename
                    ))
                    .build()
            );

            // Generate public URL
            String coverUrl = String.format("%s/%s/%s",
                minioEndpoint, coversBucket, objectName);

            // Update database
            book.setCoverUrl(coverUrl);
            bookMapper.updateById(book);

            log.info("Cover uploaded successfully: {}", coverUrl);
            return coverUrl;

        } catch (Exception e) {
            log.error("Failed to upload cover for book: {}", bookId, e);
            throw new BusinessException("封面上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadBookPdf(Long bookId, MultipartFile file) {
        log.info("Uploading PDF for book: {}", bookId);

        // Validate book exists
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // Validate file
        validatePdfFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String objectName = String.format("book_%d_%s.pdf",
                bookId,
                UUID.randomUUID().toString().substring(0, 8)
            );

            // Delete old PDF if exists
            if (book.getPdfUrl() != null) {
                deleteOldFile(pdfsBucket, extractObjectName(book.getPdfUrl()));
            }

            // Upload to MinIO with progress tracking
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(pdfsBucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType("application/pdf")
                    .userMetadata(Map.of(
                        "book-id", String.valueOf(bookId),
                        "upload-time", LocalDateTime.now().toString(),
                        "original-name", originalFilename,
                        "file-size", String.valueOf(file.getSize())
                    ))
                    .build()
            );

            // Generate pre-signed URL (7 days expiry)
            String pdfUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(pdfsBucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );

            // Update database
            book.setPdfUrl(pdfUrl);
            book.setPdfFileName(originalFilename);
            book.setPdfFileSize(file.getSize());
            bookMapper.updateById(book);

            Map<String, Object> result = new HashMap<>();
            result.put("url", pdfUrl);
            result.put("fileName", originalFilename);
            result.put("fileSize", file.getSize());
            result.put("uploadTime", LocalDateTime.now());

            log.info("PDF uploaded successfully for book: {}", bookId);
            return result;

        } catch (Exception e) {
            log.error("Failed to upload PDF for book: {}", bookId, e);
            throw new BusinessException("PDF上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadBookPdf(Long bookId) {
        log.info("Downloading PDF for book: {}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getPdfUrl() == null) {
            throw new BusinessException("图书或PDF文件不存在");
        }

        try {
            String objectName = extractObjectName(book.getPdfUrl());

            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(pdfsBucket)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to download PDF for book: {}", bookId, e);
            throw new BusinessException("PDF下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookCover(Long bookId) {
        log.info("Deleting cover for book: {}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getCoverUrl() == null) {
            return;
        }

        try {
            String objectName = extractObjectName(book.getCoverUrl());

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(coversBucket)
                    .object(objectName)
                    .build()
            );

            // Update database
            book.setCoverUrl(null);
            bookMapper.updateById(book);

            log.info("Cover deleted for book: {}", bookId);
        } catch (Exception e) {
            log.error("Failed to delete cover for book: {}", bookId, e);
            throw new BusinessException("封面删除失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookPdf(Long bookId) {
        log.info("Deleting PDF for book: {}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getPdfUrl() == null) {
            return;
        }

        try {
            String objectName = extractObjectName(book.getPdfUrl());

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(pdfsBucket)
                    .object(objectName)
                    .build()
            );

            // Update database
            book.setPdfUrl(null);
            book.setPdfFileName(null);
            book.setPdfFileSize(null);
            bookMapper.updateById(book);

            log.info("PDF deleted for book: {}", bookId);
        } catch (Exception e) {
            log.error("Failed to delete PDF for book: {}", bookId, e);
            throw new BusinessException("PDF删除失败: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expiryHours) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiryHours, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL", e);
            throw new BusinessException("生成预签名URL失败");
        }
    }

    @Override
    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Private helper methods

    private void validateImageFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > maxCoverSize) {
            throw new BusinessException(
                String.format("图片大小不能超过%dMB", maxCoverSize / 1024 / 1024)
            );
        }

        // Check file type using Apache Tika
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
                throw new BusinessException("只支持JPG/PNG格式的图片");
            }
        } catch (Exception e) {
            throw new BusinessException("文件类型检测失败");
        }

        // Check file extension
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg", "png").contains(extension.toLowerCase())) {
            throw new BusinessException("文件扩展名无效");
        }
    }

    private void validatePdfFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > maxPdfSize) {
            throw new BusinessException(
                String.format("PDF文件大小不能超过%dMB", maxPdfSize / 1024 / 1024)
            );
        }

        // Check file type using Apache Tika
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_PDF_TYPES.contains(detectedType)) {
                throw new BusinessException("只支持PDF格式的文件");
            }
        } catch (Exception e) {
            throw new BusinessException("文件类型检测失败");
        }

        // Check file extension
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!"pdf".equalsIgnoreCase(extension)) {
            throw new BusinessException("文件必须是PDF格式");
        }
    }

    private void deleteOldFile(String bucket, String objectName) {
        try {
            if (fileExists(bucket, objectName)) {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to delete old file: {}/{}", bucket, objectName);
        }
    }

    private String extractObjectName(String url) {
        // Extract object name from URL
        // Format: http://localhost:9000/bucket-name/object-name
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }
}
```

### 2.4 File Controller

```java
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

        log.info("上传图书封面: bookId={}, fileName={}, size={}",
            id, file.getOriginalFilename(), file.getSize());

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

        log.info("上传图书PDF: bookId={}, fileName={}, size={}",
            id, file.getOriginalFilename(), file.getSize());

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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"book_" + id + ".pdf\"")
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
```

---

## 3. Database Schema Extensions

### 3.1 Book Entity Updates

```java
// Add these fields to Book.java entity

/**
 * PDF文件URL
 */
@TableField("pdf_url")
private String pdfUrl;

/**
 * PDF文件原始名称
 */
@TableField("pdf_file_name")
private String pdfFileName;

/**
 * PDF文件大小（字节）
 */
@TableField("pdf_file_size")
private Long pdfFileSize;

/**
 * 文件上传时间
 */
@TableField("file_upload_time")
private LocalDateTime fileUploadTime;

/**
 * 文件版本号
 */
@TableField("file_version")
private Integer fileVersion;
```

---

## 4. Configuration Properties

### 4.1 application.yml Updates

```yaml
# MinIO Configuration
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: gcrf_minio_2024
  secure: false

  # Bucket configuration
  bucket:
    covers: library-covers
    pdfs: library-pdfs
    temp: library-temp

  # File size limits
  max-size:
    cover: 5242880    # 5MB in bytes
    pdf: 52428800     # 50MB in bytes

  # URL expiry settings
  url-expiry:
    download: 24      # Hours for download links
    preview: 1        # Hours for preview links

  # Connection pool settings
  connection:
    timeout: 10000    # Connection timeout in ms
    read-timeout: 30000  # Read timeout in ms
    write-timeout: 30000 # Write timeout in ms

# File upload settings
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB
      file-size-threshold: 1MB
      location: ${java.io.tmpdir}/library-uploads

# Async configuration for file operations
async:
  file-upload:
    core-pool-size: 2
    max-pool-size: 10
    queue-capacity: 100
    thread-name-prefix: file-upload-
```

---

## 5. Security Considerations

### 5.1 File Upload Security

```java
package com.gcrf.library.book.security;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Component
public class FileSecurityValidator {

    // Magic numbers for file type detection
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    private static final byte[] PNG_MAGIC = {(byte)0x89, 0x50, 0x4E, 0x47};

    public boolean validateFileContent(MultipartFile file, String expectedType) {
        try {
            byte[] fileHeader = new byte[10];
            file.getInputStream().read(fileHeader, 0, 10);

            return switch (expectedType) {
                case "pdf" -> Arrays.equals(Arrays.copyOf(fileHeader, 4), PDF_MAGIC);
                case "jpeg" -> Arrays.equals(Arrays.copyOf(fileHeader, 3), JPEG_MAGIC);
                case "png" -> Arrays.equals(Arrays.copyOf(fileHeader, 4), PNG_MAGIC);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    public String sanitizeFileName(String fileName) {
        // Remove path traversal attempts
        fileName = fileName.replaceAll("\\.\\.[\\\\/]", "");
        fileName = fileName.replaceAll("[\\\\/:]", "_");

        // Remove special characters
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");

        // Limit length
        if (fileName.length() > 255) {
            String extension = fileName.substring(fileName.lastIndexOf("."));
            fileName = fileName.substring(0, 250) + extension;
        }

        return fileName;
    }
}
```

### 5.2 Access Control

```java
package com.gcrf.library.book.security;

import com.gcrf.library.book.entity.Book;
import com.gcrf.library.common.security.SecurityUtils;
import org.springframework.stereotype.Component;

@Component
public class FileAccessControl {

    public boolean canUploadFile(Long bookId) {
        // Check if user has ADMIN or LIBRARIAN role
        return SecurityUtils.hasAnyRole("ADMIN", "LIBRARIAN");
    }

    public boolean canDownloadPdf(Book book) {
        // Check if user is authenticated
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }

        // Check if book is active
        if (!"ACTIVE".equals(book.getStatus())) {
            return false;
        }

        // Additional business rules
        return true;
    }

    public boolean canDeleteFile(Long bookId) {
        // Only ADMIN can delete files
        return SecurityUtils.hasRole("ADMIN");
    }
}
```

---

## 6. Performance Optimization

### 6.1 Async Upload Handler

```java
package com.gcrf.library.book.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "fileUploadExecutor")
    public Executor fileUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("file-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 6.2 Multipart Upload for Large Files

```java
package com.gcrf.library.book.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncFileUploadService {

    @Async("fileUploadExecutor")
    public CompletableFuture<String> uploadLargeFileAsync(
            MinioClient client,
            String bucket,
            String objectName,
            InputStream stream,
            long size,
            String contentType) {

        try {
            // Use multipart upload for files > 5MB
            if (size > 5 * 1024 * 1024) {
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(stream, size, 5 * 1024 * 1024) // 5MB part size
                        .contentType(contentType)
                        .build()
                );
            } else {
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
                );
            }

            return CompletableFuture.completedFuture(objectName);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

---

## 7. Error Handling

### 7.1 Custom Exceptions

```java
package com.gcrf.library.book.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class FileSizeLimitExceededException extends FileStorageException {
    public FileSizeLimitExceededException(String message) {
        super(message);
    }
}

public class InvalidFileFormatException extends FileStorageException {
    public InvalidFileFormatException(String message) {
        super(message);
    }
}

public class FileNotFoundException extends FileStorageException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
```

### 7.2 Global Exception Handler

```java
package com.gcrf.library.book.handler;

import com.gcrf.library.book.exception.*;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class FileExceptionHandler {

    @ExceptionHandler(FileSizeLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleFileSizeLimit(FileSizeLimitExceededException e) {
        log.error("File size limit exceeded", e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleInvalidFormat(InvalidFileFormatException e) {
        log.error("Invalid file format", e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleFileNotFound(FileNotFoundException e) {
        log.error("File not found", e);
        return Result.error(404, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.error("Max upload size exceeded", e);
        return Result.error(400, "文件大小超过限制");
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleFileStorage(FileStorageException e) {
        log.error("File storage error", e);
        return Result.error(500, "文件存储服务异常");
    }
}
```

---

## 8. Monitoring and Metrics

### 8.1 MinIO Health Check

```java
package com.gcrf.library.book.health;

import io.minio.MinioClient;
import io.minio.ListBucketsArgs;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;

    public MinioHealthIndicator(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public Health health() {
        try {
            // Try to list buckets to check connectivity
            minioClient.listBuckets(ListBucketsArgs.builder().build());

            return Health.up()
                    .withDetail("service", "MinIO")
                    .withDetail("status", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "MinIO")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

### 8.2 Upload Metrics

```java
package com.gcrf.library.book.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class FileUploadMetrics {

    private final Counter uploadCounter;
    private final Counter uploadErrorCounter;
    private final Timer uploadTimer;

    public FileUploadMetrics(MeterRegistry registry) {
        this.uploadCounter = Counter.builder("file.upload.count")
                .description("Number of file uploads")
                .tag("service", "book-service")
                .register(registry);

        this.uploadErrorCounter = Counter.builder("file.upload.error")
                .description("Number of file upload errors")
                .tag("service", "book-service")
                .register(registry);

        this.uploadTimer = Timer.builder("file.upload.duration")
                .description("File upload duration")
                .tag("service", "book-service")
                .register(registry);
    }

    public void recordUpload(Runnable task) {
        uploadTimer.record(task);
        uploadCounter.increment();
    }

    public void recordError() {
        uploadErrorCounter.increment();
    }
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

```java
package com.gcrf.library.book.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @Test
    void uploadBookCover_Success() {
        // Given
        Long bookId = 1L;
        MockMultipartFile file = new MockMultipartFile(
            "file", "cover.jpg", "image/jpeg",
            new byte[1024]
        );

        Book book = new Book();
        book.setId(bookId);
        when(bookMapper.selectById(bookId)).thenReturn(book);

        // When
        String url = fileStorageService.uploadBookCover(bookId, file);

        // Then
        assertThat(url).isNotNull();
        assertThat(url).contains("library-covers");
        verify(bookMapper).updateById(any(Book.class));
    }

    @Test
    void uploadBookCover_FileTooLarge_ThrowsException() {
        // Given
        Long bookId = 1L;
        MockMultipartFile file = new MockMultipartFile(
            "file", "cover.jpg", "image/jpeg",
            new byte[6 * 1024 * 1024] // 6MB
        );

        // When/Then
        assertThatThrownBy(() ->
            fileStorageService.uploadBookCover(bookId, file)
        ).isInstanceOf(BusinessException.class)
          .hasMessageContaining("图片大小不能超过");
    }
}
```

### 9.2 Integration Tests

```java
package com.gcrf.library.book.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookFileControllerIntegrationTest {

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", minioContainer::getS3URL);
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadBookCover_Integration() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "cover.jpg", "image/jpeg",
            getClass().getResourceAsStream("/test-cover.jpg")
        );

        mockMvc.perform(multipart("/api/v1/books/1/cover")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }
}
```

---

## 10. Deployment Considerations

### 10.1 Docker Compose Configuration

```yaml
version: '3.8'
services:
  minio:
    image: minio/minio:latest
    container_name: gcrf-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: gcrf_minio_2024
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    networks:
      - gcrf-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

volumes:
  minio_data:
    driver: local

networks:
  gcrf-network:
    external: true
```

### 10.2 Production Configuration

```yaml
# application-prod.yml
minio:
  endpoint: https://minio.gcrf-library.com
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  secure: true

  # Production buckets
  bucket:
    covers: prod-library-covers
    pdfs: prod-library-pdfs
    temp: prod-library-temp

  # Production limits
  max-size:
    cover: 10485760   # 10MB
    pdf: 104857600    # 100MB

  # CDN configuration
  cdn:
    enabled: true
    url: https://cdn.gcrf-library.com

# Enable compression
server:
  compression:
    enabled: true
    mime-types: application/pdf,image/jpeg,image/png
```

---

## 11. Migration Plan

### Phase 1: Infrastructure Setup (Day 1)
1. Deploy MinIO server
2. Create buckets with policies
3. Configure network and security

### Phase 2: Code Implementation (Days 2-3)
1. Add MinIO dependencies
2. Implement configuration classes
3. Implement file storage service
4. Create file controller endpoints
5. Update Book entity

### Phase 3: Testing (Day 4)
1. Unit tests for all components
2. Integration tests with TestContainers
3. Performance testing with large files
4. Security testing

### Phase 4: Deployment (Day 5)
1. Deploy to development environment
2. Smoke testing
3. Documentation update
4. Deploy to production

---

## 12. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Upload Success Rate | > 99% | Successful uploads / Total attempts |
| Average Upload Time | < 2s for images, < 10s for PDFs | Timer metrics |
| Storage Utilization | < 80% | MinIO dashboard |
| Error Rate | < 1% | Error counter / Total requests |
| Availability | > 99.9% | Health check uptime |

---

## Conclusion

This MinIO integration design provides a robust, scalable, and secure file storage solution for the Book Service. The implementation follows Spring Boot best practices, includes comprehensive error handling, and is ready for production deployment.

Key benefits:
- **Scalable**: Object storage scales independently
- **Secure**: File validation, access control, pre-signed URLs
- **Performant**: Async uploads, CDN support, caching
- **Maintainable**: Clean architecture, comprehensive testing
- **Observable**: Health checks, metrics, logging

The design is production-ready and can handle millions of books with their associated files.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Author**: Backend Architecture Specialist