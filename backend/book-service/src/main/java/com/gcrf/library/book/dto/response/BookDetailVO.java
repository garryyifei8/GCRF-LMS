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
 * 图书详情响应VO - 详情页展示所有字段
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailVO {

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
     * 副标题
     */
    private String subtitle;

    /**
     * 作者
     */
    private String author;

    /**
     * 译者
     */
    private String translator;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private LocalDate publishDate;

    /**
     * 版本
     */
    private String edition;

    /**
     * 页数
     */
    private Integer pages;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 装帧
     */
    private String binding;

    /**
     * 语言
     */
    private String language;

    /**
     * 分类代码
     */
    private String classificationCode;

    /**
     * 主题关键词
     */
    private String subjectKeywords;

    /**
     * 摘要/简介
     */
    private String description;

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
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static BookDetailVO from(Book book) {
        if (book == null) {
            return null;
        }
        return BookDetailVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .subtitle(book.getSubtitle())
                .author(book.getAuthor())
                .translator(book.getTranslator())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate())
                .edition(book.getEdition())
                .pages(book.getPages())
                .price(book.getPrice())
                .binding(book.getBinding())
                .language(book.getLanguage())
                .classificationCode(book.getClassificationCode())
                .subjectKeywords(book.getSubjectKeywords())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .totalQuantity(book.getTotalQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
