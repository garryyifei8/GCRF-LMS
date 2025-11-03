package com.gcrf.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.response.EmailLogVO;
import com.gcrf.library.notification.entity.EmailLog;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.mapper.EmailLogMapper;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.messaging.NotificationMessageProducer;
import com.gcrf.library.notification.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmailServiceImpl单元测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private EmailLogMapper emailLogMapper;

    @Mock
    private NotificationTemplateMapper templateMapper;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationMessageProducer messageProducer;

    @InjectMocks
    private EmailServiceImpl emailService;

    private EmailSendRequest emailRequest;
    private EmailLog emailLog;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        emailRequest = new EmailSendRequest();
        emailRequest.setRecipient("test@example.com");
        emailRequest.setSubject("测试邮件");
        emailRequest.setContent("<p>这是一封测试邮件</p>");

        emailLog = new EmailLog();
        emailLog.setId(1L);
        emailLog.setRecipient("test@example.com");
        emailLog.setSubject("测试邮件");
        emailLog.setContent("<p>这是一封测试邮件</p>");
        emailLog.setStatus("PENDING");
        emailLog.setRetryCount(0);
        emailLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSendEmail_Success() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogMapper.insert(any(EmailLog.class))).thenReturn(1);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        // Act
        EmailLogVO result = emailService.sendEmail(emailRequest);

        // Assert
        assertNotNull(result);
        verify(emailLogMapper).insert(any(EmailLog.class));
        verify(mailSender).send(any(MimeMessage.class));
        verify(emailLogMapper, times(2)).updateById(any(EmailLog.class));
    }

    @Test
    void testSendEmail_MessagingException() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogMapper.insert(any(EmailLog.class))).thenReturn(1);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        doThrow(new MessagingException("发送失败")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(BusinessException.class, () -> emailService.sendEmail(emailRequest));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogMapper, times(2)).updateById(captor.capture());

        EmailLog updatedLog = captor.getAllValues().get(1);
        assertEquals("FAILED", updatedLog.getStatus());
        assertNotNull(updatedLog.getErrorMessage());
    }

    @Test
    void testSendEmailAsync_Success() {
        // Arrange
        when(emailLogMapper.insert(any(EmailLog.class))).thenReturn(1);
        doNothing().when(messageProducer).sendEmailMessage(any(), anyLong());

        // Act
        emailService.sendEmailAsync(emailRequest);

        // Assert
        verify(emailLogMapper).insert(any(EmailLog.class));
        verify(messageProducer).sendEmailMessage(eq(emailRequest), anyLong());
    }

    @Test
    void testSendEmailWithTemplate_Success() {
        // Arrange
        Long templateId = 1L;
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "测试用户");

        NotificationTemplate template = new NotificationTemplate();
        template.setId(templateId);
        template.setTemplateType("EMAIL");
        template.setSubject("欢迎邮件");
        template.setContent("欢迎 {{username}}");

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);
        when(templateService.renderTemplate(eq(templateId), eq(variables)))
                .thenReturn("欢迎 测试用户");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogMapper.insert(any(EmailLog.class))).thenReturn(1);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        // Act
        EmailLogVO result = emailService.sendEmailWithTemplate(
                "test@example.com", templateId, variables);

        // Assert
        assertNotNull(result);
        verify(templateMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(templateService).renderTemplate(eq(templateId), eq(variables));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailWithTemplate_TemplateNotFound() {
        // Arrange
        Long templateId = 999L;
        Map<String, Object> variables = new HashMap<>();

        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> emailService.sendEmailWithTemplate("test@example.com", templateId, variables));

        verify(templateMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testQueryEmailLogs_Success() {
        // Arrange
        LogQueryRequest request = new LogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(20);
        request.setStatus("SENT");

        List<EmailLog> logs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            EmailLog log = new EmailLog();
            log.setId((long) i);
            log.setRecipient("test" + i + "@example.com");
            log.setSubject("测试邮件 " + i);
            log.setStatus("SENT");
            logs.add(log);
        }

        Page<EmailLog> page = new Page<>(1, 20);
        page.setRecords(logs);
        page.setTotal(10);

        when(emailLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        // Act
        PageResult<EmailLogVO> result = emailService.queryEmailLogs(request);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getTotal());
        assertEquals(10, result.getRecords().size());
        verify(emailLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetEmailLogById_Success() {
        // Arrange
        Long logId = 1L;
        when(emailLogMapper.selectById(logId)).thenReturn(emailLog);

        // Act
        EmailLogVO result = emailService.getEmailLogById(logId);

        // Assert
        assertNotNull(result);
        assertEquals(emailLog.getRecipient(), result.getRecipient());
        verify(emailLogMapper).selectById(logId);
    }

    @Test
    void testGetEmailLogById_NotFound() {
        // Arrange
        Long logId = 999L;
        when(emailLogMapper.selectById(logId)).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> emailService.getEmailLogById(logId));
        verify(emailLogMapper).selectById(logId);
    }

    @Test
    void testRetryFailedEmail_Success() throws MessagingException {
        // Arrange
        Long logId = 1L;
        emailLog.setStatus("FAILED");

        when(emailLogMapper.selectById(logId)).thenReturn(emailLog);
        when(emailLogMapper.deleteById(logId)).thenReturn(1);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogMapper.insert(any(EmailLog.class))).thenReturn(1);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        // Act
        EmailLogVO result = emailService.retryFailedEmail(logId);

        // Assert
        assertNotNull(result);
        verify(emailLogMapper).selectById(logId);
        verify(emailLogMapper).deleteById(logId);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testRetryFailedEmail_NotFailed() {
        // Arrange
        Long logId = 1L;
        emailLog.setStatus("SENT");

        when(emailLogMapper.selectById(logId)).thenReturn(emailLog);

        // Act & Assert
        assertThrows(BusinessException.class, () -> emailService.retryFailedEmail(logId));

        verify(emailLogMapper).selectById(logId);
        verify(emailLogMapper, never()).deleteById(anyLong());
    }

    @Test
    void testRetryFailedEmail_LogNotFound() {
        // Arrange
        Long logId = 999L;
        when(emailLogMapper.selectById(logId)).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> emailService.retryFailedEmail(logId));
        verify(emailLogMapper).selectById(logId);
    }
}
