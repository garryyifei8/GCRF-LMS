package com.gcrf.library.book.service.impl;

import com.gcrf.library.book.config.MinioConfig;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.book.service.FileStorageService;
import com.gcrf.library.common.exception.BusinessException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务实现
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final BookMapper bookMapper;
    private final MinioConfig minioConfig;
    private final Tika tika = new Tika();

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/jpg");
    private static final Set<String> ALLOWED_PDF_TYPES = Set.of("application/pdf");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadBookCover(Long bookId, MultipartFile file) {
        log.info("上传封面: bookId={}, fileName={}, size={}", bookId, file.getOriginalFilename(), file.getSize());

        // 验证图书存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // 验证文件
        validateImageFile(file);

        try {
            // 生成唯一文件名
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String objectName = String.format("book_%d_%s.%s", bookId, UUID.randomUUID().toString().substring(0, 8), extension);

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketCovers())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 生成URL
            String coverUrl = String.format("%s/%s/%s", minioConfig.getEndpoint(), minioConfig.getBucketCovers(), objectName);

            // 更新数据库
            book.setCoverUrl(coverUrl);
            bookMapper.updateById(book);

            log.info("封面上传成功: {}", coverUrl);
            return coverUrl;

        } catch (Exception e) {
            log.error("封面上传失败: bookId={}", bookId, e);
            throw new BusinessException("封面上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadBookPdf(Long bookId, MultipartFile file) {
        log.info("上传PDF: bookId={}, fileName={}, size={}", bookId, file.getOriginalFilename(), file.getSize());

        // 验证图书存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // 验证文件
        validatePdfFile(file);

        try {
            // 生成唯一文件名
            String objectName = String.format("book_%d_%s.pdf", bookId, UUID.randomUUID().toString().substring(0, 8));

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketPdfs())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/pdf")
                            .build()
            );

            // 生成URL
            String pdfUrl = String.format("%s/%s/%s", minioConfig.getEndpoint(), minioConfig.getBucketPdfs(), objectName);

            // 更新数据库
            book.setPdfUrl(pdfUrl);
            book.setPdfFileName(file.getOriginalFilename());
            book.setPdfFileSize(file.getSize());
            bookMapper.updateById(book);

            Map<String, Object> result = new HashMap<>();
            result.put("url", pdfUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("uploadTime", LocalDateTime.now());

            log.info("PDF上传成功: {}", pdfUrl);
            return result;

        } catch (Exception e) {
            log.error("PDF上传失败: bookId={}", bookId, e);
            throw new BusinessException("PDF上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadBookPdf(Long bookId) {
        log.info("下载PDF: bookId={}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getPdfUrl() == null) {
            throw new BusinessException("图书或PDF文件不存在");
        }

        try {
            String objectName = extractObjectName(book.getPdfUrl());
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketPdfs())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("PDF下载失败: bookId={}", bookId, e);
            throw new BusinessException("PDF下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookCover(Long bookId) {
        log.info("删除封面: bookId={}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getCoverUrl() == null) {
            return;
        }

        try {
            String objectName = extractObjectName(book.getCoverUrl());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketCovers())
                            .object(objectName)
                            .build()
            );

            book.setCoverUrl(null);
            bookMapper.updateById(book);

            log.info("封面删除成功: bookId={}", bookId);
        } catch (Exception e) {
            log.error("封面删除失败: bookId={}", bookId, e);
            throw new BusinessException("封面删除失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookPdf(Long bookId) {
        log.info("删除PDF: bookId={}", bookId);

        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getPdfUrl() == null) {
            return;
        }

        try {
            String objectName = extractObjectName(book.getPdfUrl());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketPdfs())
                            .object(objectName)
                            .build()
            );

            book.setPdfUrl(null);
            book.setPdfFileName(null);
            book.setPdfFileSize(null);
            bookMapper.updateById(book);

            log.info("PDF删除成功: bookId={}", bookId);
        } catch (Exception e) {
            log.error("PDF删除失败: bookId={}", bookId, e);
            throw new BusinessException("PDF删除失败: " + e.getMessage());
        }
    }

    // 私有辅助方法

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > minioConfig.getMaxCoverSize()) {
            throw new BusinessException(
                    String.format("图片大小不能超过%dMB", minioConfig.getMaxCoverSize() / 1024 / 1024));
        }

        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
                throw new BusinessException("只支持JPG/PNG格式的图片");
            }
        } catch (Exception e) {
            throw new BusinessException("文件类型检测失败");
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!Set.of("jpg", "jpeg", "png").contains(extension.toLowerCase())) {
            throw new BusinessException("文件扩展名无效");
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file.getSize() > minioConfig.getMaxPdfSize()) {
            throw new BusinessException(
                    String.format("PDF文件大小不能超过%dMB", minioConfig.getMaxPdfSize() / 1024 / 1024));
        }

        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_PDF_TYPES.contains(detectedType)) {
                throw new BusinessException("只支持PDF格式的文件");
            }
        } catch (Exception e) {
            throw new BusinessException("文件类型检测失败");
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!"pdf".equalsIgnoreCase(extension)) {
            throw new BusinessException("文件必须是PDF格式");
        }
    }

    private String extractObjectName(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }
}
