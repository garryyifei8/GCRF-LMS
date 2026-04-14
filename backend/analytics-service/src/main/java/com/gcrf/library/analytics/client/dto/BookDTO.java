package com.gcrf.library.analytics.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书DTO（用于Feign调用）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class BookDTO {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String categoryCode;
    private String categoryName;
    private String coverUrl;
    private Integer totalCopies;
    private Integer availableCopies;
    private BigDecimal rating;
    private String barcode;
    private LocalDateTime createdAt;
}
