package com.gcrf.library.analytics.client.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 图书统计DTO（用于Feign调用）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class BookStatsDTO {

    private Long totalBooks;
    private Long totalCopies;
    private Long availableCopies;
    private Long borrowedCopies;
    private Long thisMonthNewBooks;
    private Long zeroCirculationCount;
    private BigDecimal circulationRate;
}
