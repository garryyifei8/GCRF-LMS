package com.gcrf.library.analytics.client.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 流通统计DTO（用于Feign调用）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class CirculationStatsDTO {

    private Long currentBorrowed;
    private Long overdueCount;
    private Long reservationCount;
    private Long todayBorrowed;
    private Long todayReturned;
    private Long thisMonthBorrowed;
    private Long thisMonthReturned;
    private Long totalVisits;
    private Long todayVisits;
    private Long thisMonthVisits;
    private BigDecimal borrowGrowth;
    private BigDecimal returnGrowth;
}
