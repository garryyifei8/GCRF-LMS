package com.gcrf.library.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书DTO - 用于服务间调用
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 分类代码
     */
    private String classificationCode;

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
     * 已借出数量
     */
    private Integer borrowedQuantity;

    /**
     * 预约数量
     */
    private Integer reservedQuantity;

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
}
