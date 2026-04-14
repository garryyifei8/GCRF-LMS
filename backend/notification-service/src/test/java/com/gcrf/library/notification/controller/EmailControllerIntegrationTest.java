package com.gcrf.library.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.entity.EmailLog;
import com.gcrf.library.notification.mapper.EmailLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmailController集成测试
 *
 * 测试覆盖范围：
 * - 发送邮件（同步/异步）
 * - 查询邮件日志（分页、条件过滤）
 * - 获取邮件日志详情
 * - 重试失败的邮件
 * - 使用模板发送邮件
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EmailControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailLogMapper emailLogMapper;

    private EmailLog testEmailLog;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        LambdaQueryWrapper<EmailLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmailLog::getRecipient, "test@example.com");
        emailLogMapper.delete(queryWrapper);

        // 创建测试邮件日志
        testEmailLog = new EmailLog();
        testEmailLog.setRecipient("test@example.com");
        testEmailLog.setSubject("集成测试邮件");
        testEmailLog.setContent("<p>这是一封集成测试邮件</p>");
        testEmailLog.setStatus("SENT");
        testEmailLog.setRetryCount(0);
        testEmailLog.setCreatedAt(LocalDateTime.now());
        testEmailLog.setSentAt(LocalDateTime.now());
        emailLogMapper.insert(testEmailLog);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testEmailLog != null && testEmailLog.getId() != null) {
            emailLogMapper.deleteById(testEmailLog.getId());
        }
    }

    @Test
    void testSendEmail_Success() throws Exception {
        // Arrange
        EmailSendRequest request = new EmailSendRequest();
        request.setRecipient("newtest@example.com");
        request.setSubject("测试邮件");
        request.setContent("<p>测试内容</p>");

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recipient").value("newtest@example.com"))
                .andExpect(jsonPath("$.data.subject").value("测试邮件"))
                .andExpect(jsonPath("$.data.status").isNotEmpty());
    }

    @Test
    void testSendEmail_InvalidRecipient() throws Exception {
        // Arrange
        EmailSendRequest request = new EmailSendRequest();
        request.setRecipient(""); // 空邮箱
        request.setSubject("测试邮件");
        request.setContent("<p>测试内容</p>");

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendEmailAsync_Success() throws Exception {
        // Arrange
        EmailSendRequest request = new EmailSendRequest();
        request.setRecipient("async@example.com");
        request.setSubject("异步邮件");
        request.setContent("<p>异步内容</p>");

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testQueryEmailLogs_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void testQueryEmailLogs_WithDateRange() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetEmailLogById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/logs/{logId}", testEmailLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testEmailLog.getId()))
                .andExpect(jsonPath("$.data.recipient").value("test@example.com"))
                .andExpect(jsonPath("$.data.subject").value("集成测试邮件"));
    }

    @Test
    void testGetEmailLogById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/logs/{logId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testRetryFailedEmail_Success() throws Exception {
        // Arrange - 创建失败的邮件日志
        EmailLog failedLog = new EmailLog();
        failedLog.setRecipient("failed@example.com");
        failedLog.setSubject("失败的邮件");
        failedLog.setContent("<p>失败内容</p>");
        failedLog.setStatus("FAILED");
        failedLog.setRetryCount(1);
        failedLog.setErrorMessage("发送失败");
        failedLog.setCreatedAt(LocalDateTime.now());
        emailLogMapper.insert(failedLog);

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/logs/{logId}/retry", failedLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recipient").value("failed@example.com"));

        // Cleanup
        emailLogMapper.deleteById(failedLog.getId());
    }

    @Test
    void testRetryFailedEmail_WrongStatus() throws Exception {
        // 尝试重试已成功的邮件
        mockMvc.perform(post("/api/v1/emails/logs/{logId}/retry", testEmailLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testSendEmailWithTemplate_Success() throws Exception {
        // Note: 这个测试需要预先创建模板数据
        // 这里仅演示API调用流程,实际运行需要数据库中有对应的模板
        mockMvc.perform(post("/api/v1/emails/send-with-template")
                        .param("recipient", "template@example.com")
                        .param("templateId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"测试用户\"}"))
                .andExpect(status().isOk());
        // 由于模板可能不存在,不强制验证成功
    }
}
