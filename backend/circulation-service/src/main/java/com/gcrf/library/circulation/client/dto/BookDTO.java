package com.gcrf.library.circulation.client.dto;

import lombok.Data;

/**
 * 图书信息DTO - Feign调用返回
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class BookDTO {

    /**
     * 图书ID
     */
    private Long id;

    /**
     * 图书标题
     */
    private String title;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 图书条码
     */
    private String barcode;

    /**
     * 总库存
     */
    private Integer totalCopies;

    /**
     * 可借数量
     */
    private Integer availableCopies;

    /**
     * 状态 (AVAILABLE-可借, UNAVAILABLE-不可借)
     */
    private String status;
}
