package com.gcrf.library.notification.messaging;

import com.gcrf.library.notification.config.RabbitMQConfig;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知消息生产者
 *
 * 负责将邮件和短信发送请求发送到RabbitMQ队列
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送邮件消息到队列
     *
     * @param request 邮件发送请求
     * @param logId 邮件日志ID(用于追踪)
     */
    public void sendEmailMessage(EmailSendRequest request, Long logId) {
        try {
            // 构建消息对象,包含请求和日志ID
            EmailMessage message = EmailMessage.builder()
                    .logId(logId)
                    .recipient(request.getRecipient())
                    .subject(request.getSubject())
                    .content(request.getContent())
                    .templateId(request.getTemplateId())
                    .build();

            // 发送到邮件队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.EMAIL_ROUTING_KEY,
                    message
            );

            log.info("邮件消息已发送到队列, logId: {}, recipient: {}", logId, request.getRecipient());

        } catch (Exception e) {
            log.error("发送邮件消息到队列失败, logId: {}, error: {}", logId, e.getMessage(), e);
            throw new RuntimeException("发送邮件消息失败", e);
        }
    }

    /**
     * 发送短信消息到队列
     *
     * @param request 短信发送请求
     * @param logId 短信日志ID(用于追踪)
     */
    public void sendSmsMessage(SmsSendRequest request, Long logId) {
        try {
            // 构建消息对象,包含请求和日志ID
            SmsMessage message = SmsMessage.builder()
                    .logId(logId)
                    .phoneNumber(request.getPhoneNumber())
                    .content(request.getContent())
                    .smsType(request.getSmsType())
                    .build();

            // 发送到短信队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.SMS_ROUTING_KEY,
                    message
            );

            log.info("短信消息已发送到队列, logId: {}, phoneNumber: {}", logId, request.getPhoneNumber());

        } catch (Exception e) {
            log.error("发送短信消息到队列失败, logId: {}, error: {}", logId, e.getMessage(), e);
            throw new RuntimeException("发送短信消息失败", e);
        }
    }

    /**
     * 邮件消息对象
     * 用于在RabbitMQ中传输
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmailMessage {
        /** 邮件日志ID */
        private Long logId;
        /** 收件人邮箱 */
        private String recipient;
        /** 邮件主题 */
        private String subject;
        /** 邮件内容 */
        private String content;
        /** 模板ID(可选) */
        private Long templateId;
    }

    /**
     * 短信消息对象
     * 用于在RabbitMQ中传输
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SmsMessage {
        /** 短信日志ID */
        private Long logId;
        /** 手机号 */
        private String phoneNumber;
        /** 短信内容 */
        private String content;
        /** 短信类型: VERIFICATION-验证码, NOTIFICATION-通知, MARKETING-营销 */
        private String smsType;
    }
}
