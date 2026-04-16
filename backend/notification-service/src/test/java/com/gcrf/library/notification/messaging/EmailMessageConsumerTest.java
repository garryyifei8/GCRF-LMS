package com.gcrf.library.notification.messaging;

import com.gcrf.library.notification.entity.EmailLog;
import com.gcrf.library.notification.mapper.EmailLogMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmailMessageConsumer单元测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class EmailMessageConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogMapper emailLogMapper;

    @InjectMocks
    private EmailMessageConsumer emailConsumer;

    private NotificationMessageProducer.EmailMessage message;
    private EmailLog emailLog;

    @BeforeEach
    void setUp() {
        message = NotificationMessageProducer.EmailMessage.builder()
                .logId(1L)
                .recipient("test@example.com")
                .subject("测试邮件")
                .content("<p>测试内容</p>")
                .build();

        emailLog = new EmailLog();
        emailLog.setId(1L);
        emailLog.setRecipient("test@example.com");
        emailLog.setSubject("测试邮件");
        emailLog.setContent("<p>测试内容</p>");
        emailLog.setStatus("PENDING");
        emailLog.setRetryCount(0);
        emailLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testConsumeEmailMessage_Success() throws MessagingException {
        // Arrange
        when(emailLogMapper.selectById(1L)).thenReturn(emailLog);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailConsumer.consumeEmailMessage(message);

        // Assert
        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogMapper, times(2)).updateById(captor.capture());

        EmailLog sentLog = captor.getAllValues().get(1);
        assertEquals("SENT", sentLog.getStatus());
        assertNotNull(sentLog.getSentAt());
    }

    @Test
    void testConsumeEmailMessage_LogNotFound() {
        // Arrange
        when(emailLogMapper.selectById(1L)).thenReturn(null);

        // Act
        emailConsumer.consumeEmailMessage(message);

        // Assert
        verify(emailLogMapper).selectById(1L);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testConsumeEmailMessage_WrongStatus() {
        // Arrange
        emailLog.setStatus("SENT");
        when(emailLogMapper.selectById(1L)).thenReturn(emailLog);

        // Act
        emailConsumer.consumeEmailMessage(message);

        // Assert
        verify(emailLogMapper).selectById(1L);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testConsumeEmailMessage_MessagingException_WithRetry() throws MessagingException {
        // Arrange
        emailLog.setRetryCount(0);
        when(emailLogMapper.selectById(1L)).thenReturn(emailLog);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("发送失败")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> emailConsumer.consumeEmailMessage(message));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogMapper, times(2)).updateById(captor.capture());

        EmailLog failedLog = captor.getAllValues().get(1);
        assertEquals("FAILED", failedLog.getStatus());
        assertEquals(1, failedLog.getRetryCount());
    }

    @Test
    void testConsumeEmailMessage_MessagingException_MaxRetriesReached() throws MessagingException {
        // Arrange
        emailLog.setRetryCount(2); // 已经重试2次
        when(emailLogMapper.selectById(1L)).thenReturn(emailLog);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("发送失败")).when(mailSender).send(any(MimeMessage.class));

        // Act - 不应该抛出异常,因为已达到最大重试次数
        emailConsumer.consumeEmailMessage(message);

        // Assert
        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogMapper, times(2)).updateById(captor.capture());

        EmailLog failedLog = captor.getAllValues().get(1);
        assertEquals("FAILED", failedLog.getStatus());
        assertEquals(3, failedLog.getRetryCount());
    }

    @Test
    void testConsumeEmailMessage_OtherException_WithRetry() throws MessagingException {
        // Arrange
        emailLog.setRetryCount(0);
        when(emailLogMapper.selectById(1L)).thenReturn(emailLog);
        when(emailLogMapper.updateById(any(EmailLog.class))).thenReturn(1);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("意外错误")).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> emailConsumer.consumeEmailMessage(message));

        verify(emailLogMapper, times(2)).updateById(any(EmailLog.class));
    }
}
