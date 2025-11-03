package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书响应VO - 列表页展示
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookVO {

    /**
     * 图书ID
     */
    private Long id;

    /**
     * ISBN号
     */
    private String isbn;

    /**
     * 图书标题
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private LocalDate publishDate;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 馆藏总数
     */
    private Integer totalQuantity;

    /**
     * 可借数量
     */
    private Integer availableQuantity;

    /**
     * 状态：ACTIVE-正常 INACTIVE-下架
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static BookVO from(Book book) {
        if (book == null) {
            return null;
        }
        return BookVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate())
                .price(book.getPrice())
                .coverUrl(book.getCoverUrl())
                .totalQuantity(book.getTotalQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .createdAt(book.getCreatedAt())
                .build();
    }
}
