package com.gcrf.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import com.gcrf.library.notification.dto.response.SmsLogVO;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.entity.SmsLog;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.mapper.SmsLogMapper;
import com.gcrf.library.notification.messaging.NotificationMessageProducer;
import com.gcrf.library.notification.service.impl.SmsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SmsServiceImpl单元测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock
    private SmsLogMapper smsLogMapper;

    @Mock
    private NotificationTemplateMapper templateMapper;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private NotificationMessageProducer messageProducer;

    @InjectMocks
    private SmsServiceImpl smsService;

    private SmsSendRequest smsRequest;
    private SmsLog smsLog;

    @BeforeEach
    void setUp() {
        smsRequest = new SmsSendRequest();
        smsRequest.setPhoneNumber("13800138000");
        smsRequest.setContent("测试短信内容");
        smsRequest.setSmsType("NOTIFICATION");

        smsLog = new SmsLog();
        smsLog.setId(1L);
        smsLog.setPhoneNumber("13800138000");
        smsLog.setContent("测试短信内容");
        smsLog.setSmsType("NOTIFICATION");
        smsLog.setStatus("PENDING");
        smsLog.setRetryCount(0);
        smsLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSendSms_Success() {
        // Arrange
        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        when(smsLogMapper.updateById(any(SmsLog.class))).thenReturn(1);

        // Act
        SmsLogVO result = smsService.sendSms(smsRequest);

        // Assert
        assertNotNull(result);
        verify(smsLogMapper).insert(any(SmsLog.class));
        verify(smsLogMapper, times(2)).updateById(any(SmsLog.class));
    }

    @Test
    void testSendSmsAsync_Success() {
        // Arrange
        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        doNothing().when(messageProducer).sendSmsMessage(any(), anyLong());

        // Act
        smsService.sendSmsAsync(smsRequest);

        // Assert
        verify(smsLogMapper).insert(any(SmsLog.class));
        verify(messageProducer).sendSmsMessage(eq(smsRequest), anyLong());
    }

    @Test
    void testSendVerificationCode_Success() {
        // Arrange
        String phoneNumber = "13800138000";
        String code = "123456";
        Integer expiresMinutes = 5;

        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        when(smsLogMapper.updateById(any(SmsLog.class))).thenReturn(1);

        // Act
        SmsLogVO result = smsService.sendVerificationCode(phoneNumber, code, expiresMinutes);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().contains(code));
        assertTrue(result.getContent().contains(expiresMinutes.toString()));
        verify(smsLogMapper).insert(any(SmsLog.class));
    }

    @Test
    void testSendVerificationCode_DefaultExpiration() {
        // Arrange
        String phoneNumber = "13800138000";
        String code = "123456";

        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        when(smsLogMapper.updateById(any(SmsLog.class))).thenReturn(1);

        // Act
        SmsLogVO result = smsService.sendVerificationCode(phoneNumber, code, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().contains("5分钟")); // 默认5分钟
        verify(smsLogMapper).insert(any(SmsLog.class));
    }

    @Test
    void testSendSmsWithTemplate_Success() {
        // Arrange
        Long templateId = 1L;
        String phoneNumber = "13800138000";
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "测试用户");

        NotificationTemplate template = new NotificationTemplate();
        template.setId(templateId);
        template.setTemplateType("SMS");
        template.setContent("欢迎 {{username}}");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(templateService.renderTemplate(eq(templateId), eq(variables)))
                .thenReturn("欢迎 测试用户");
        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        when(smsLogMapper.updateById(any(SmsLog.class))).thenReturn(1);

        // Act
        SmsLogVO result = smsService.sendSmsWithTemplate(phoneNumber, templateId, variables);

        // Assert
        assertNotNull(result);
        verify(templateMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(templateService).renderTemplate(eq(templateId), eq(variables));
    }

    @Test
    void testSendSmsWithTemplate_TemplateNotFound() {
        // Arrange
        Long templateId = 999L;
        Map<String, Object> variables = new HashMap<>();

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> smsService.sendSmsWithTemplate("13800138000", templateId, variables));

        verify(templateMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testQuerySmsLogs_Success() {
        // Arrange
        LogQueryRequest request = new LogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(20);
        request.setStatus("SENT");

        List<SmsLog> logs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            SmsLog log = new SmsLog();
            log.setId((long) i);
            log.setPhoneNumber("1380013800" + i);
            log.setContent("测试短信 " + i);
            log.setStatus("SENT");
            logs.add(log);
        }

        Page<SmsLog> page = new Page<>(1, 20);
        page.setRecords(logs);
        page.setTotal(10);

        when(smsLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        // Act
        PageResult<SmsLogVO> result = smsService.querySmsLogs(request);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getTotal());
        assertEquals(10, result.getRecords().size());
    }

    @Test
    void testGetSmsLogById_Success() {
        // Arrange
        Long logId = 1L;
        when(smsLogMapper.selectById(logId)).thenReturn(smsLog);

        // Act
        SmsLogVO result = smsService.getSmsLogById(logId);

        // Assert
        assertNotNull(result);
        assertEquals(smsLog.getPhoneNumber(), result.getPhoneNumber());
    }

    @Test
    void testGetSmsLogById_NotFound() {
        // Arrange
        Long logId = 999L;
        when(smsLogMapper.selectById(logId)).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> smsService.getSmsLogById(logId));
    }

    @Test
    void testRetryFailedSms_Success() {
        // Arrange
        Long logId = 1L;
        smsLog.setStatus("FAILED");

        when(smsLogMapper.selectById(logId)).thenReturn(smsLog);
        when(smsLogMapper.deleteById(logId)).thenReturn(1);
        when(smsLogMapper.insert(any(SmsLog.class))).thenReturn(1);
        when(smsLogMapper.updateById(any(SmsLog.class))).thenReturn(1);

        // Act
        SmsLogVO result = smsService.retryFailedSms(logId);

        // Assert
        assertNotNull(result);
        verify(smsLogMapper).selectById(logId);
        verify(smsLogMapper).deleteById(logId);
    }

    @Test
    void testRetryFailedSms_NotFailed() {
        // Arrange
        Long logId = 1L;
        smsLog.setStatus("SENT");

        when(smsLogMapper.selectById(logId)).thenReturn(smsLog);

        // Act & Assert
        assertThrows(BusinessException.class, () -> smsService.retryFailedSms(logId));

        verify(smsLogMapper).selectById(logId);
        verify(smsLogMapper, never()).deleteById(anyLong());
    }

    @Test
    void testVerifyCode_Success() {
        // Arrange
        String phoneNumber = "13800138000";
        String code = "123456";

        SmsLog log = new SmsLog();
        log.setPhoneNumber(phoneNumber);
        log.setContent("您的验证码是" + code + ",有效期5分钟,请勿泄露给他人。");
        log.setSmsType("VERIFICATION");
        log.setStatus("SENT");
        log.setSentAt(LocalDateTime.now().minusMinutes(2));

        when(smsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(log);

        // Act
        boolean result = smsService.verifyCode(phoneNumber, code);

        // Assert
        assertTrue(result);
        verify(smsLogMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testVerifyCode_NoLogFound() {
        // Arrange
        String phoneNumber = "13800138000";
        String code = "123456";

        when(smsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act
        boolean result = smsService.verifyCode(phoneNumber, code);

        // Assert
        assertFalse(result);
    }

    @Test
    void testVerifyCode_WrongCode() {
        // Arrange
        String phoneNumber = "13800138000";
        String code = "123456";

        SmsLog log = new SmsLog();
        log.setPhoneNumber(phoneNumber);
        log.setContent("您的验证码是654321,有效期5分钟,请勿泄露给他人。");
        log.setSmsType("VERIFICATION");
        log.setStatus("SENT");
        log.setSentAt(LocalDateTime.now().minusMinutes(2));

        when(smsLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(log);

        // Act
        boolean result = smsService.verifyCode(phoneNumber, code);

        // Assert
        assertFalse(result);
    }
}
