package com.gcrf.library.book.service;

import com.gcrf.library.book.config.MinioConfig;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.book.service.impl.FileStorageServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FileStorageService单元测试
 *
 * @author GCRF Team
 * @date 2025-11-06
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件存储服务测试")
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private MinioConfig minioConfig;

    private FileStorageServiceImpl fileStorageService;

    private Book testBook;
    private static final String ENDPOINT = "http://localhost:9000";
    private static final String BUCKET_COVERS = "library-covers";
    private static final String BUCKET_PDFS = "library-pdfs";
    private static final Long MAX_COVER_SIZE = 5242880L; // 5MB
    private static final Long MAX_PDF_SIZE = 52428800L; // 50MB

    @BeforeEach
    void setUp() {
        // Mock MinioConfig methods
        lenient().when(minioConfig.getEndpoint()).thenReturn(ENDPOINT);
        lenient().when(minioConfig.getBucketCovers()).thenReturn(BUCKET_COVERS);
        lenient().when(minioConfig.getBucketPdfs()).thenReturn(BUCKET_PDFS);
        lenient().when(minioConfig.getMaxCoverSize()).thenReturn(MAX_COVER_SIZE);
        lenient().when(minioConfig.getMaxPdfSize()).thenReturn(MAX_PDF_SIZE);

        // Create service with mocked dependencies
        fileStorageService = new FileStorageServiceImpl(minioClient, bookMapper, minioConfig);

        // 准备测试图书
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("测试图书");
        testBook.setIsbn("978-7-111-42252-1");
    }

    @Test
    @DisplayName("上传图书封面 - 成功")
    void uploadBookCover_Success() throws Exception {
        // Arrange
        // Create valid JPEG content (JPEG magic bytes)
        byte[] jpegMagic = new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0};
        byte[] content = new byte[100];
        System.arraycopy(jpegMagic, 0, content, 0, jpegMagic.length);

        MultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                content
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        String result = fileStorageService.uploadBookCover(1L, file);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains(ENDPOINT);
        assertThat(result).contains(BUCKET_COVERS);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(bookMapper, times(1)).updateById(any(Book.class));
    }

    @Test
    @DisplayName("上传图书封面 - 图书不存在")
    void uploadBookCover_BookNotFound() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        when(bookMapper.selectById(1L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> fileStorageService.uploadBookCover(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图书不存在");
    }

    @Test
    @DisplayName("上传图书封面 - 无效的文件类型")
    void uploadBookCover_InvalidFileType() {
        // Arrange - Create a plain text file (not an image)
        byte[] content = "This is not an image file, it's just text content.".getBytes();

        MultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                content
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert - BusinessException due to invalid file type or detection failure
        assertThatThrownBy(() -> fileStorageService.uploadBookCover(1L, file))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("上传图书封面 - 文件大小超限")
    void uploadBookCover_FileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                largeContent
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        assertThatThrownBy(() -> fileStorageService.uploadBookCover(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片大小不能超过");
    }

    @Test
    @DisplayName("上传图书PDF - 成功")
    void uploadBookPdf_Success() throws Exception {
        // Arrange - PDF magic bytes
        byte[] pdfMagic = "%PDF-1.4".getBytes();
        byte[] content = new byte[100];
        System.arraycopy(pdfMagic, 0, content, 0, pdfMagic.length);

        MultipartFile file = new MockMultipartFile(
                "file",
                "book.pdf",
                "application/pdf",
                content
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        Map<String, Object> result = fileStorageService.uploadBookPdf(1L, file);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("url", "fileName", "fileSize", "uploadTime");
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("上传图书PDF - 无效的文件类型")
    void uploadBookPdf_InvalidFileType() {
        // Arrange - Create a valid JPEG file with proper structure
        // Use simple text content that won't be detected as PDF
        byte[] content = "This is not a PDF file, it's just text content.".getBytes();

        MultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",  // Wrong extension
                "text/plain",
                content
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        // This can either fail due to extension validation or file type detection
        assertThatThrownBy(() -> fileStorageService.uploadBookPdf(1L, file))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("下载图书PDF - 成功")
    void downloadBookPdf_Success() throws Exception {
        // Arrange
        testBook.setPdfUrl(ENDPOINT + "/" + BUCKET_PDFS + "/book_1_abc12345.pdf");
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        // Act
        InputStream result = fileStorageService.downloadBookPdf(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("下载图书PDF - 文件不存在")
    void downloadBookPdf_FileNotFound() {
        // Arrange
        testBook.setPdfUrl(null);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        assertThatThrownBy(() -> fileStorageService.downloadBookPdf(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PDF文件不存在");
    }

    @Test
    @DisplayName("删除图书封面 - 成功")
    void deleteBookCover_Success() throws Exception {
        // Arrange
        testBook.setCoverUrl(ENDPOINT + "/" + BUCKET_COVERS + "/book_1_cover.jpg");
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        fileStorageService.deleteBookCover(1L);

        // Assert
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        verify(bookMapper, times(1)).updateById(any(Book.class));
    }

    @Test
    @DisplayName("删除图书PDF - 成功")
    void deleteBookPdf_Success() throws Exception {
        // Arrange
        testBook.setPdfUrl(ENDPOINT + "/" + BUCKET_PDFS + "/book_1_abc12345.pdf");
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        // Act
        fileStorageService.deleteBookPdf(1L);

        // Assert
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        verify(bookMapper, times(1)).updateById(any(Book.class));
    }

    @Test
    @DisplayName("删除图书封面 - 封面不存在时静默返回")
    void deleteBookCover_NoCover() throws Exception {
        // Arrange
        testBook.setCoverUrl(null);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act - should not throw exception
        fileStorageService.deleteBookCover(1L);

        // Assert - MinIO should not be called
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("删除图书PDF - PDF不存在时静默返回")
    void deleteBookPdf_NoPdf() throws Exception {
        // Arrange
        testBook.setPdfUrl(null);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act - should not throw exception
        fileStorageService.deleteBookPdf(1L);

        // Assert - MinIO should not be called
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("上传图书封面 - 文件扩展名无效")
    void uploadBookCover_InvalidExtension() {
        // Arrange - JPEG magic bytes but wrong extension
        byte[] jpegMagic = new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0};
        byte[] content = new byte[100];
        System.arraycopy(jpegMagic, 0, content, 0, jpegMagic.length);

        MultipartFile file = new MockMultipartFile(
                "file",
                "cover.txt",  // Wrong extension
                "image/jpeg",
                content
        );

        when(bookMapper.selectById(1L)).thenReturn(testBook);

        // Act & Assert
        assertThatThrownBy(() -> fileStorageService.uploadBookCover(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("扩展名");
    }
}
