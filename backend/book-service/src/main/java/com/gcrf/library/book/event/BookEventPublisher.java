package com.gcrf.library.book.event;

import com.gcrf.library.book.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 图书事件发布者
 *
 * @author GCRF Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布图书创建事件
     */
    @Async
    public void publishBookCreated(BookEvent event) {
        log.info("发布图书创建事件: bookId={}, title={}", event.getBookId(), event.getTitle());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOK_EXCHANGE,
                    RabbitMQConfig.BOOK_CREATED_KEY,
                    event
            );
            log.debug("图书创建事件发布成功");
        } catch (Exception e) {
            log.error("发布图书创建事件失败: bookId={}", event.getBookId(), e);
        }
    }

    /**
     * 发布图书更新事件
     */
    @Async
    public void publishBookUpdated(BookEvent event) {
        log.info("发布图书更新事件: bookId={}, title={}", event.getBookId(), event.getTitle());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOK_EXCHANGE,
                    RabbitMQConfig.BOOK_UPDATED_KEY,
                    event
            );
            log.debug("图书更新事件发布成功");
        } catch (Exception e) {
            log.error("发布图书更新事件失败: bookId={}", event.getBookId(), e);
        }
    }

    /**
     * 发布图书删除事件
     */
    @Async
    public void publishBookDeleted(BookEvent event) {
        log.info("发布图书删除事件: bookId={}, title={}", event.getBookId(), event.getTitle());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOK_EXCHANGE,
                    RabbitMQConfig.BOOK_DELETED_KEY,
                    event
            );
            log.debug("图书删除事件发布成功");
        } catch (Exception e) {
            log.error("发布图书删除事件失败: bookId={}", event.getBookId(), e);
        }
    }

    /**
     * 发布库存预警事件
     */
    @Async
    public void publishInventoryLow(InventoryEvent event) {
        log.info("发布库存预警事件: bookId={}, title={}, availableQuantity={}",
                event.getBookId(), event.getTitle(), event.getAvailableQuantity());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INVENTORY_EXCHANGE,
                    RabbitMQConfig.INVENTORY_LOW_KEY,
                    event
            );
            log.debug("库存预警事件发布成功");
        } catch (Exception e) {
            log.error("发布库存预警事件失败: bookId={}", event.getBookId(), e);
        }
    }
}
