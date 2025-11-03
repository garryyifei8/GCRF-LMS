package com.gcrf.library.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 *
 * 消息路由设计:
 * - Exchange: notification.topic (Topic类型)
 * - Email Queue: notification.email.queue (routing key: notification.email.#)
 * - SMS Queue: notification.sms.queue (routing key: notification.sms.#)
 * - Dead Letter Exchange: notification.dlx (处理失败消息)
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Configuration
public class RabbitMQConfig {

    // ========== 常量定义 ==========

    /** 主交换机名称 */
    public static final String NOTIFICATION_EXCHANGE = "notification.topic";

    /** 邮件队列名称 */
    public static final String EMAIL_QUEUE = "notification.email.queue";

    /** 短信队列名称 */
    public static final String SMS_QUEUE = "notification.sms.queue";

    /** 邮件路由键 */
    public static final String EMAIL_ROUTING_KEY = "notification.email";

    /** 短信路由键 */
    public static final String SMS_ROUTING_KEY = "notification.sms";

    /** 死信交换机名称 */
    public static final String DLX_EXCHANGE = "notification.dlx";

    /** 死信队列名称 */
    public static final String DLX_QUEUE = "notification.dlx.queue";

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
                System.out.println("消息发送成功: " + correlationData);
            } else {
                System.err.println("消息发送失败: " + cause);
            }
        });

        // 消息返回回调(当消息无法路由到队列时触发)
        template.setReturnsCallback(returned -> {
            System.err.println("消息未路由到队列: " + returned.getMessage());
        });

        return template;
    }

    /**
     * 监听器容器工厂配置
     * 设置消息转换器和并发消费者数量
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(3);  // 并发消费者数量
        factory.setMaxConcurrentConsumers(10);  // 最大并发消费者数量
        factory.setDefaultRequeueRejected(false);  // 消费失败不重新入队
        return factory;
    }

    // ========== 主交换机和队列 ==========

    /**
     * 主题交换机
     * 用于根据routing key路由消息到不同队列
     */
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EXCHANGE)
                .durable(true)  // 持久化
                .build();
    }

    /**
     * 邮件队列
     * 绑定死信交换机,消息TTL 10分钟
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)  // 死信交换机
                .deadLetterRoutingKey("dlx.email")  // 死信路由键
                .ttl(600000)  // 消息TTL: 10分钟
                .build();
    }

    /**
     * 短信队列
     * 绑定死信交换机,消息TTL 10分钟
     */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey("dlx.sms")
                .ttl(600000)
                .build();
    }

    /**
     * 邮件队列绑定
     * routing key: notification.email.#
     */
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(notificationExchange)
                .with(EMAIL_ROUTING_KEY + ".#");
    }

    /**
     * 短信队列绑定
     * routing key: notification.sms.#
     */
    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(smsQueue)
                .to(notificationExchange)
                .with(SMS_ROUTING_KEY + ".#");
    }

    // ========== 死信交换机和队列 ==========

    /**
     * 死信交换机
     * 用于处理消费失败或过期的消息
     */
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信队列
     * 存储所有死信消息,供后续人工处理
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE)
                .build();
    }

    /**
     * 死信队列绑定
     * 捕获所有死信消息
     */
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue)
                .to(dlxExchange)
                .with("dlx.#");
    }
}
