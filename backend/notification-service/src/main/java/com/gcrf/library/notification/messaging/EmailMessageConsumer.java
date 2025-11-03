package com.gcrf.library.notification.messaging;

import com.gcrf.library.notification.config.RabbitMQConfig;
import com.gcrf.library.notification.entity.EmailLog;
import com.gcrf.library.notification.mapper.EmailLogMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 邮件消息消费者
 *
 * 从RabbitMQ队列消费邮件发送请求并执行实际发送
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessageConsumer {

    private final JavaMailSender mailSender;
    private final EmailLogMapper emailLogMapper;

    /**
     * 消费邮件消息并发送
     *
     * @param message 邮件消息对象
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmailMessage(NotificationMessageProducer.EmailMessage message) {
        log.info("收到邮件消息, logId: {}, recipient: {}", message.getLogId(), message.getRecipient());

        // 查询邮件日志
        EmailLog emailLog = emailLogMapper.selectById(message.getLogId());
        if (emailLog == null) {
            log.error("邮件日志不存在, logId: {}", message.getLogId());
            return;
        }

        // 检查状态
        if (!"PENDING".equals(emailLog.getStatus())) {
            log.warn("邮件状态不是PENDING, 跳过发送, logId: {}, status: {}",
                    message.getLogId(), emailLog.getStatus());
            return;
        }

        // 更新状态为SENDING
        emailLog.setStatus("SENDING");
        emailLogMapper.updateById(emailLog);

        try {
            // 发送邮件
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(message.getRecipient());
            helper.setSubject(message.getSubject());
            helper.setText(message.getContent(), true); // HTML格式

            mailSender.send(mimeMessage);

            // 更新状态为SENT
            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
            emailLogMapper.updateById(emailLog);

            log.info("邮件发送成功, logId: {}, recipient: {}", message.getLogId(), message.getRecipient());

        } catch (MessagingException e) {
            // 发送失败,更新状态为FAILED
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogMapper.updateById(emailLog);

            log.error("邮件发送失败, logId: {}, recipient: {}, error: {}",
                    message.getLogId(), message.getRecipient(), e.getMessage(), e);

            // 如果重试次数小于3次,重新抛出异常触发消息重新入队
            if (emailLog.getRetryCount() < 3) {
                throw new RuntimeException("邮件发送失败,将重试", e);
            } else {
                log.error("邮件发送失败次数超过3次,不再重试, logId: {}", message.getLogId());
            }

        } catch (Exception e) {
            // 其他异常
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogMapper.updateById(emailLog);

            log.error("邮件发送异常, logId: {}, error: {}", message.getLogId(), e.getMessage(), e);

            if (emailLog.getRetryCount() < 3) {
                throw new RuntimeException("邮件发送异常,将重试", e);
            }
        }
    }
}
