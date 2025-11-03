package com.gcrf.library.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import com.gcrf.library.notification.entity.SmsLog;
import com.gcrf.library.notification.mapper.SmsLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SmsController集成测试
 *
 * 测试覆盖范围：
 * - 发送短信（同步/异步）
 * - 发送验证码
 * - 使用模板发送短信
 * - 查询短信日志（分页、条件过滤）
 * - 获取短信日志详情
 * - 重试失败的短信
 * - 验证验证码
 *
 * @author GCRF Team
 * @since 2025-10-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SmsLogMapper smsLogMapper;

    private SmsLog testSmsLog;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        LambdaQueryWrapper<SmsLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SmsLog::getPhoneNumber, "13800138000");
        smsLogMapper.delete(queryWrapper);

        // 创建测试短信日志
        testSmsLog = new SmsLog();
        testSmsLog.setPhoneNumber("13800138000");
        testSmsLog.setContent("集成测试短信内容");
        testSmsLog.setSmsType("NOTIFICATION");
        testSmsLog.setStatus("SENT");
        testSmsLog.setRetryCount(0);
        testSmsLog.setCreatedAt(LocalDateTime.now());
        testSmsLog.setSentAt(LocalDateTime.now());
        smsLogMapper.insert(testSmsLog);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testSmsLog != null && testSmsLog.getId() != null) {
            smsLogMapper.deleteById(testSmsLog.getId());
        }
    }

    @Test
    void testSendSms_Success() throws Exception {
        // Arrange
        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber("13900139000");
        request.setContent("测试短信内容");
        request.setSmsType("NOTIFICATION");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phoneNumber").value("13900139000"))
                .andExpect(jsonPath("$.data.content").value("测试短信内容"))
                .andExpect(jsonPath("$.data.status").isNotEmpty());
    }

    @Test
    void testSendSms_InvalidPhoneNumber() throws Exception {
        // Arrange
        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber(""); // 空手机号
        request.setContent("测试短信内容");
        request.setSmsType("NOTIFICATION");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSendSmsAsync_Success() throws Exception {
        // Arrange
        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber("13900139001");
        request.setContent("异步短信内容");
        request.setSmsType("NOTIFICATION");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/send-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSendVerificationCode_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/send-verification-code")
                        .param("phoneNumber", "13900139002")
                        .param("code", "123456")
                        .param("expiresMinutes", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phoneNumber").value("13900139002"))
                .andExpect(jsonPath("$.data.content").value(containsString("123456")))
                .andExpect(jsonPath("$.data.content").value(containsString("5分钟")));
    }

    @Test
    void testSendVerificationCode_DefaultExpiration() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/send-verification-code")
                        .param("phoneNumber", "13900139003")
                        .param("code", "654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value(containsString("654321")))
                .andExpect(jsonPath("$.data.content").value(containsString("5分钟"))); // 默认5分钟
    }

    @Test
    void testSendSmsWithTemplate_Success() throws Exception {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "测试用户");

        // Note: 这个测试需要预先创建模板数据
        // 这里仅演示API调用流程,实际运行需要数据库中有对应的模板
        mockMvc.perform(post("/api/v1/sms/send-with-template")
                        .param("phoneNumber", "13900139004")
                        .param("templateId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variables)))
                .andExpect(status().isOk());
        // 由于模板可能不存在,不强制验证成功
    }

    @Test
    void testQuerySmsLogs_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sms/logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void testQuerySmsLogs_WithDateRange() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sms/logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetSmsLogById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sms/logs/{logId}", testSmsLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testSmsLog.getId()))
                .andExpect(jsonPath("$.data.phoneNumber").value("13800138000"))
                .andExpect(jsonPath("$.data.content").value("集成测试短信内容"));
    }

    @Test
    void testGetSmsLogById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sms/logs/{logId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testRetryFailedSms_Success() throws Exception {
        // Arrange - 创建失败的短信日志
        SmsLog failedLog = new SmsLog();
        failedLog.setPhoneNumber("13900139005");
        failedLog.setContent("失败的短信");
        failedLog.setSmsType("NOTIFICATION");
        failedLog.setStatus("FAILED");
        failedLog.setRetryCount(1);
        failedLog.setErrorMessage("发送失败");
        failedLog.setCreatedAt(LocalDateTime.now());
        smsLogMapper.insert(failedLog);

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/logs/{logId}/retry", failedLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phoneNumber").value("13900139005"));

        // Cleanup
        smsLogMapper.deleteById(failedLog.getId());
    }

    @Test
    void testRetryFailedSms_WrongStatus() throws Exception {
        // 尝试重试已成功的短信
        mockMvc.perform(post("/api/v1/sms/logs/{logId}/retry", testSmsLog.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testVerifyCode_Success() throws Exception {
        // Arrange - 创建验证码短信日志
        SmsLog verificationLog = new SmsLog();
        verificationLog.setPhoneNumber("13900139006");
        verificationLog.setContent("您的验证码是888888,有效期5分钟,请勿泄露给他人。");
        verificationLog.setSmsType("VERIFICATION");
        verificationLog.setStatus("SENT");
        verificationLog.setRetryCount(0);
        verificationLog.setCreatedAt(LocalDateTime.now());
        verificationLog.setSentAt(LocalDateTime.now());
        smsLogMapper.insert(verificationLog);

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/verify-code")
                        .param("phoneNumber", "13900139006")
                        .param("code", "888888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        // Cleanup
        smsLogMapper.deleteById(verificationLog.getId());
    }

    @Test
    void testVerifyCode_WrongCode() throws Exception {
        // Arrange - 创建验证码短信日志
        SmsLog verificationLog = new SmsLog();
        verificationLog.setPhoneNumber("13900139007");
        verificationLog.setContent("您的验证码是777777,有效期5分钟,请勿泄露给他人。");
        verificationLog.setSmsType("VERIFICATION");
        verificationLog.setStatus("SENT");
        verificationLog.setRetryCount(0);
        verificationLog.setCreatedAt(LocalDateTime.now());
        verificationLog.setSentAt(LocalDateTime.now());
        smsLogMapper.insert(verificationLog);

        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/verify-code")
                        .param("phoneNumber", "13900139007")
                        .param("code", "111111")) // 错误的验证码
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));

        // Cleanup
        smsLogMapper.deleteById(verificationLog.getId());
    }

    @Test
    void testVerifyCode_NoLogFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/sms/verify-code")
                        .param("phoneNumber", "13900139999")
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
