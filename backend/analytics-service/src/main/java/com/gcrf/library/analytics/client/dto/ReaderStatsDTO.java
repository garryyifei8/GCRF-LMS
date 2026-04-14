package com.gcrf.library.analytics.client.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 读者统计DTO（用于Feign调用）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class ReaderStatsDTO {

    private Long totalReaders;
    private Long activeReaders;
    private Long todayNewReaders;
    private Long thisMonthNewReaders;
    private BigDecimal activeRate;
}
