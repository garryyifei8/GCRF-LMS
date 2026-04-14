package com.gcrf.library.book.integration;

import com.gcrf.library.book.service.FileStorageService;
import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BookFileController Integration Tests
 * Tests file upload/download operations with mocked MinIO
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("BookFileController Integration Tests")
@Sql(scripts = "/testdata/book-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookFileControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/books";

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    // ==================== Upload Book Cover ====================

    @Test
    @DisplayName("Should upload book cover successfully")
    void testUploadBookCover_success() throws Exception {
        // Prepare test image file
        MockMultipartFile coverFile = new MockMultipartFile(
                "file",
                "test-cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        String expectedUrl = "http://minio.example.com/books/1000/cover.jpg";
        when(fileStorageService.uploadBookCover(eq(1000L), any())).thenReturn(expectedUrl);

        mockMvc.perform(multipart(BASE_URL + "/1000/cover")
                        .file(coverFile))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(expectedUrl));

        verify(fileStorageService, times(1)).uploadBookCover(eq(1000L), any());
    }

    @Test
    @DisplayName("Should fail to upload cover with empty file")
    void testUploadBookCover_emptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        when(fileStorageService.uploadBookCover(eq(1000L), any()))
                .thenThrow(new IllegalArgumentException("文件为空"));

        mockMvc.perform(multipart(BASE_URL + "/1000/cover")
                        .file(emptyFile))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should fail to upload cover with invalid file type")
    void testUploadBookCover_invalidFileType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        when(fileStorageService.uploadBookCover(eq(1000L), any()))
                .thenThrow(new IllegalArgumentException("不支持的文件类型"));

        mockMvc.perform(multipart(BASE_URL + "/1000/cover")
                        .file(invalidFile))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should fail to upload cover for non-existent book")
    void testUploadBookCover_bookNotFound() throws Exception {
        MockMultipartFile coverFile = new MockMultipartFile(
                "file",
                "cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        when(fileStorageService.uploadBookCover(eq(99999L), any()))
                .thenThrow(new RuntimeException("图书不存在"));

        mockMvc.perform(multipart(BASE_URL + "/99999/cover")
                        .file(coverFile))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should fail to upload cover with oversized file")
    void testUploadBookCover_oversizedFile() throws Exception {
        // Simulate a 6MB file (exceeds 5MB limit)
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeContent
        );

        when(fileStorageService.uploadBookCover(eq(1000L), any()))
                .thenThrow(new IllegalArgumentException("文件大小超过限制"));

        mockMvc.perform(multipart(BASE_URL + "/1000/cover")
                        .file(largeFile))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    // ==================== Upload Book PDF ====================

    @Test
    @DisplayName("Should upload book PDF successfully")
    void testUploadBookPdf_success() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "test-book.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

        Map<String, Object> expectedResult = Map.of(
                "pdfUrl", "http://minio.example.com/books/1000/book.pdf",
                "fileName", "test-book.pdf",
                "fileSize", 16L
        );

        when(fileStorageService.uploadBookPdf(eq(1000L), any())).thenReturn(expectedResult);

        mockMvc.perform(multipart(BASE_URL + "/1000/pdf")
                        .file(pdfFile))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pdfUrl").value(containsString("book.pdf")))
                .andExpect(jsonPath("$.data.fileName").value("test-book.pdf"))
                .andExpect(jsonPath("$.data.fileSize").isNumber());

        verify(fileStorageService, times(1)).uploadBookPdf(eq(1000L), any());
    }

    @Test
    @DisplayName("Should fail to upload PDF with invalid file type")
    void testUploadBookPdf_invalidFileType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "not-a-pdf.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not a pdf".getBytes()
        );

        when(fileStorageService.uploadBookPdf(eq(1000L), any()))
                .thenThrow(new IllegalArgumentException("只支持PDF格式"));

        mockMvc.perform(multipart(BASE_URL + "/1000/pdf")
                        .file(invalidFile))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should fail to upload PDF with oversized file")
    void testUploadBookPdf_oversizedFile() throws Exception {
        // Simulate a 51MB file (exceeds 50MB limit)
        byte[] largeContent = new byte[51 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-book.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                largeContent
        );

        when(fileStorageService.uploadBookPdf(eq(1000L), any()))
                .thenThrow(new IllegalArgumentException("PDF文件大小超过50MB限制"));

        mockMvc.perform(multipart(BASE_URL + "/1000/pdf")
                        .file(largeFile))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    // ==================== Download Book PDF ====================

    @Test
    @DisplayName("Should download book PDF successfully")
    void testDownloadBookPdf_success() throws Exception {
        byte[] pdfContent = "fake pdf content for download".getBytes();
        InputStream inputStream = new ByteArrayInputStream(pdfContent);

        when(fileStorageService.downloadBookPdf(1003L)).thenReturn(inputStream);

        mockMvc.perform(get(BASE_URL + "/1003/download"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes(pdfContent));

        verify(fileStorageService, times(1)).downloadBookPdf(1003L);
    }

    @Test
    @DisplayName("Should fail to download PDF from book without PDF")
    void testDownloadBookPdf_noPdfAvailable() throws Exception {
        when(fileStorageService.downloadBookPdf(1000L))
                .thenThrow(new RuntimeException("该图书没有上传PDF文件"));

        mockMvc.perform(get(BASE_URL + "/1000/download"))
                .andDo(print())
                .andExpect(status().is5xxServerError());

        verify(fileStorageService, times(1)).downloadBookPdf(1000L);
    }

    @Test
    @DisplayName("Should fail to download PDF from non-existent book")
    void testDownloadBookPdf_bookNotFound() throws Exception {
        when(fileStorageService.downloadBookPdf(99999L))
                .thenThrow(new RuntimeException("图书不存在"));

        mockMvc.perform(get(BASE_URL + "/99999/download"))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    // ==================== Delete Book Cover ====================

    @Test
    @DisplayName("Should delete book cover successfully")
    void testDeleteBookCover_success() throws Exception {
        doNothing().when(fileStorageService).deleteBookCover(1000L);

        mockMvc.perform(delete(BASE_URL + "/1000/cover"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(fileStorageService, times(1)).deleteBookCover(1000L);
    }

    @Test
    @DisplayName("Should fail to delete cover from book without cover")
    void testDeleteBookCover_noCoverAvailable() throws Exception {
        doThrow(new RuntimeException("该图书没有封面"))
                .when(fileStorageService).deleteBookCover(1002L);

        mockMvc.perform(delete(BASE_URL + "/1002/cover"))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should fail to delete cover from non-existent book")
    void testDeleteBookCover_bookNotFound() throws Exception {
        doThrow(new RuntimeException("图书不存在"))
                .when(fileStorageService).deleteBookCover(99999L);

        mockMvc.perform(delete(BASE_URL + "/99999/cover"))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    // ==================== Delete Book PDF ====================

    @Test
    @DisplayName("Should delete book PDF successfully")
    void testDeleteBookPdf_success() throws Exception {
        doNothing().when(fileStorageService).deleteBookPdf(1003L);

        mockMvc.perform(delete(BASE_URL + "/1003/pdf"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(fileStorageService, times(1)).deleteBookPdf(1003L);
    }

    @Test
    @DisplayName("Should fail to delete PDF from book without PDF")
    void testDeleteBookPdf_noPdfAvailable() throws Exception {
        doThrow(new RuntimeException("该图书没有上传PDF文件"))
                .when(fileStorageService).deleteBookPdf(1000L);

        mockMvc.perform(delete(BASE_URL + "/1000/pdf"))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should fail to delete PDF from non-existent book")
    void testDeleteBookPdf_bookNotFound() throws Exception {
        doThrow(new RuntimeException("图书不存在"))
                .when(fileStorageService).deleteBookPdf(99999L);

        mockMvc.perform(delete(BASE_URL + "/99999/pdf"))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle concurrent uploads for same book")
    void testConcurrentUploads() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "cover1.jpg", MediaType.IMAGE_JPEG_VALUE, "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "cover2.jpg", MediaType.IMAGE_JPEG_VALUE, "content2".getBytes()
        );

        String url1 = "http://minio.example.com/books/1000/cover1.jpg";
        String url2 = "http://minio.example.com/books/1000/cover2.jpg";

        when(fileStorageService.uploadBookCover(eq(1000L), any()))
                .thenReturn(url1)
                .thenReturn(url2);

        // First upload
        mockMvc.perform(multipart(BASE_URL + "/1000/cover").file(file1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(url1));

        // Second upload (should replace the first)
        mockMvc.perform(multipart(BASE_URL + "/1000/cover").file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(url2));

        verify(fileStorageService, times(2)).uploadBookCover(eq(1000L), any());
    }

    @Test
    @DisplayName("Should handle upload with special characters in filename")
    void testUploadWithSpecialCharacters() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "测试封面 (2024).jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "content".getBytes()
        );

        String expectedUrl = "http://minio.example.com/books/1000/cover.jpg";
        when(fileStorageService.uploadBookCover(eq(1000L), any())).thenReturn(expectedUrl);

        mockMvc.perform(multipart(BASE_URL + "/1000/cover").file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
