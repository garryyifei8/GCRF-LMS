package com.gcrf.library.circulation.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类 - 流通服务消息发布配置
 *
 * 消息路由设计:
 * - Exchange: circulation.topic (Topic类型)
 * - circulation.overdue.detected -> notification.email.queue (逾期通知)
 * - circulation.return.completed -> system.log.queue (系统日志)
 * - circulation.reservation.ready -> notification.sms.queue (预约通知)
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Configuration
public class RabbitMQConfig {

    // ========== Exchange常量 ==========

    /** 流通服务交换机名称 */
    public static final String CIRCULATION_EXCHANGE = "circulation.topic";

    /** 通知服务交换机名称 */
    public static final String NOTIFICATION_EXCHANGE = "notification.topic";

    /** 系统日志交换机名称 */
    public static final String SYSTEM_LOG_EXCHANGE = "system.log.topic";

    // ========== Queue常量 ==========

    /** 邮件通知队列 */
    public static final String EMAIL_QUEUE = "notification.email.queue";

    /** 短信通知队列 */
    public static final String SMS_QUEUE = "notification.sms.queue";

    /** 系统日志队列 */
    public static final String SYSTEM_LOG_QUEUE = "system.log.queue";

    // ========== Routing Key常量 ==========

    /** 逾期检测路由键 */
    public static final String ROUTING_KEY_OVERDUE_DETECTED = "circulation.overdue.detected";

    /** 归还完成路由键 */
    public static final String ROUTING_KEY_RETURN_COMPLETED = "circulation.return.completed";

    /** 预约就绪路由键 */
    public static final String ROUTING_KEY_RESERVATION_READY = "circulation.reservation.ready";

    /** 罚金支付路由键 */
    public static final String ROUTING_KEY_FINE_PAID = "circulation.fine.paid";

    // ========== 消息转换器 ==========

    /**
     * JSON消息转换器
     * 用于自动序列化/反序列化消息对象
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     * 设置消息转换器和确认回调
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);

        // 发送确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("[Circulation] 消息发送成功: " + correlationData);
            } else {
                System.err.println("[Circulation] 消息发送失败: " + cause);
            }
        });

        // 消息返回回调(当消息无法路由到队列时触发)
        template.setReturnsCallback(returned -> {
            System.err.println("[Circulation] 消息未路由到队列: " + returned.getMessage());
        });

        return template;
    }

    // ========== 流通服务交换机 ==========

    /**
     * 流通服务主题交换机
     * 用于发布流通相关事件
     */
    @Bean
    public TopicExchange circulationExchange() {
        return ExchangeBuilder.topicExchange(CIRCULATION_EXCHANGE)
                .durable(true)
                .build();
    }

    // ========== 通知服务交换机和队列声明 ==========

    /**
     * 通知服务主题交换机
     * 用于路由通知消息
     */
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 系统日志主题交换机
     */
    @Bean
    public TopicExchange systemLogExchange() {
        return ExchangeBuilder.topicExchange(SYSTEM_LOG_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 邮件通知队列
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .ttl(600000)  // 消息TTL: 10分钟
                .build();
    }

    /**
     * 短信通知队列
     */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .ttl(600000)
                .build();
    }

    /**
     * 系统日志队列
     */
    @Bean
    public Queue systemLogQueue() {
        return QueueBuilder.durable(SYSTEM_LOG_QUEUE)
                .ttl(3600000)  // 消息TTL: 1小时
                .build();
    }

    // ========== 绑定关系 ==========

    /**
     * 逾期检测事件 -> 邮件队列
     * routing key: circulation.overdue.detected
     */
    @Bean
    public Binding overdueToEmailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(notificationExchange)
                .with("notification.email.#");
    }

    /**
     * 预约就绪事件 -> 短信队列
     * routing key: circulation.reservation.ready
     */
    @Bean
    public Binding reservationToSmsBinding(Queue smsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(smsQueue)
                .to(notificationExchange)
                .with("notification.sms.#");
    }

    /**
     * 归还完成事件 -> 系统日志队列
     */
    @Bean
    public Binding returnToLogBinding(Queue systemLogQueue, TopicExchange systemLogExchange) {
        return BindingBuilder.bind(systemLogQueue)
                .to(systemLogExchange)
                .with("system.log.#");
    }
}
