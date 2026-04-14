package com.gcrf.library.book.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 *
 * @author GCRF Team
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String BOOK_EXCHANGE = "book.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";

    // Queue names
    public static final String BOOK_CREATED_QUEUE = "book.created.queue";
    public static final String BOOK_UPDATED_QUEUE = "book.updated.queue";
    public static final String BOOK_DELETED_QUEUE = "book.deleted.queue";
    public static final String INVENTORY_LOW_QUEUE = "inventory.low.queue";

    // Routing keys
    public static final String BOOK_CREATED_KEY = "book.created";
    public static final String BOOK_UPDATED_KEY = "book.updated";
    public static final String BOOK_DELETED_KEY = "book.deleted";
    public static final String INVENTORY_LOW_KEY = "inventory.low";

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // ============ Book Exchange and Queues ============

    @Bean
    public TopicExchange bookExchange() {
        return new TopicExchange(BOOK_EXCHANGE);
    }

    @Bean
    public Queue bookCreatedQueue() {
        return QueueBuilder.durable(BOOK_CREATED_QUEUE).build();
    }

    @Bean
    public Queue bookUpdatedQueue() {
        return QueueBuilder.durable(BOOK_UPDATED_QUEUE).build();
    }

    @Bean
    public Queue bookDeletedQueue() {
        return QueueBuilder.durable(BOOK_DELETED_QUEUE).build();
    }

    @Bean
    public Binding bookCreatedBinding() {
        return BindingBuilder.bind(bookCreatedQueue()).to(bookExchange()).with(BOOK_CREATED_KEY);
    }

    @Bean
    public Binding bookUpdatedBinding() {
        return BindingBuilder.bind(bookUpdatedQueue()).to(bookExchange()).with(BOOK_UPDATED_KEY);
    }

    @Bean
    public Binding bookDeletedBinding() {
        return BindingBuilder.bind(bookDeletedQueue()).to(bookExchange()).with(BOOK_DELETED_KEY);
    }

    // ============ Inventory Exchange and Queues ============

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE);
    }

    @Bean
    public Queue inventoryLowQueue() {
        return QueueBuilder.durable(INVENTORY_LOW_QUEUE).build();
    }

    @Bean
    public Binding inventoryLowBinding() {
        return BindingBuilder.bind(inventoryLowQueue()).to(inventoryExchange()).with(INVENTORY_LOW_KEY);
    }
}
