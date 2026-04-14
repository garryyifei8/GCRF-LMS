package com.gcrf.library.book.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存事件
 *
 * @author GCRF Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 当前库存数量
     */
    private Integer currentQuantity;

    /**
     * 可借数量
     */
    private Integer availableQuantity;

    /**
     * 预警阈值
     */
    private Integer alertThreshold;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        LOW_STOCK,      // 库存不足
        OUT_OF_STOCK,   // 缺货
        ADJUSTED        // 库存调整
    }

    /**
     * 创建库存预警事件
     */
    public static InventoryEvent lowStock(Long bookId, String isbn, String title,
                                          Integer currentQuantity, Integer availableQuantity, Integer alertThreshold) {
        return InventoryEvent.builder()
                .eventType(EventType.LOW_STOCK)
                .bookId(bookId)
                .isbn(isbn)
                .title(title)
                .currentQuantity(currentQuantity)
                .availableQuantity(availableQuantity)
                .alertThreshold(alertThreshold)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建缺货事件
     */
    public static InventoryEvent outOfStock(Long bookId, String isbn, String title) {
        return InventoryEvent.builder()
                .eventType(EventType.OUT_OF_STOCK)
                .bookId(bookId)
                .isbn(isbn)
                .title(title)
                .currentQuantity(0)
                .availableQuantity(0)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
