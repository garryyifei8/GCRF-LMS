package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 总览统计数据VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "总览统计数据")
public class OverviewVO {

    // ==================== 馆情数据 ====================

    @Schema(description = "图书总种数", example = "10000")
    private Long totalBooks;

    @Schema(description = "图书总册数", example = "25000")
    private Long totalCopies;

    @Schema(description = "读者总数", example = "5000")
    private Long totalReaders;

    @Schema(description = "总到馆人次", example = "125000")
    private Long totalVisits;

    @Schema(description = "人均藏书量", example = "5.0")
    private BigDecimal booksPerReader;

    @Schema(description = "人均到馆次数", example = "25.0")
    private BigDecimal visitsPerReader;

    // ==================== 当前借阅情况 ====================

    @Schema(description = "当前借出数量", example = "2500")
    private Long currentBorrowed;

    @Schema(description = "可借副本数", example = "22500")
    private Long availableCopies;

    @Schema(description = "逾期数量", example = "350")
    private Long overdueCount;

    @Schema(description = "预约数量", example = "180")
    private Long reservationCount;

    // ==================== 今日数据 ====================

    @Schema(description = "今日到馆人次", example = "320")
    private Long todayVisits;

    @Schema(description = "今日借阅量", example = "85")
    private Long todayBorrowed;

    @Schema(description = "今日归还量", example = "92")
    private Long todayReturned;

    @Schema(description = "今日新增读者", example = "5")
    private Long todayNewReaders;

    // ==================== 本月数据 ====================

    @Schema(description = "本月借阅量", example = "1800")
    private Long thisMonthBorrowed;

    @Schema(description = "本月归还量", example = "1650")
    private Long thisMonthReturned;

    @Schema(description = "本月到馆人次", example = "8500")
    private Long thisMonthVisits;

    @Schema(description = "本月新增图书", example = "120")
    private Long thisMonthNewBooks;

    // ==================== 流通率相关 ====================

    @Schema(description = "流通率", example = "0.65")
    private BigDecimal circulationRate;

    @Schema(description = "零借阅图书数量", example = "800")
    private Long zeroCirculationCount;

    @Schema(description = "零借阅率", example = "0.08")
    private BigDecimal zeroCirculationRate;

    // ==================== 同比增长 ====================

    @Schema(description = "借阅量同比增长", example = "0.15")
    private BigDecimal borrowGrowth;

    @Schema(description = "到馆人次同比增长", example = "0.12")
    private BigDecimal visitsGrowth;

    @Schema(description = "读者数量同比增长", example = "0.08")
    private BigDecimal readerGrowth;
}
