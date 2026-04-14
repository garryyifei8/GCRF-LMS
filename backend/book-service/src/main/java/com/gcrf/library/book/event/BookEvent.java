package com.gcrf.library.book.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图书事件
 *
 * @author GCRF Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEvent implements Serializable {

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
     * 作者
     */
    private String author;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 附加信息
     */
    private String extra;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }

    /**
     * 创建图书创建事件
     */
    public static BookEvent created(Long bookId, String isbn, String title, String author, Long categoryId, String categoryName) {
        return BookEvent.builder()
                .eventType(EventType.CREATED)
                .bookId(bookId)
                .isbn(isbn)
                .title(title)
                .author(author)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建图书更新事件
     */
    public static BookEvent updated(Long bookId, String isbn, String title, String author) {
        return BookEvent.builder()
                .eventType(EventType.UPDATED)
                .bookId(bookId)
                .isbn(isbn)
                .title(title)
                .author(author)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建图书删除事件
     */
    public static BookEvent deleted(Long bookId, String isbn, String title) {
        return BookEvent.builder()
                .eventType(EventType.DELETED)
                .bookId(bookId)
                .isbn(isbn)
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
