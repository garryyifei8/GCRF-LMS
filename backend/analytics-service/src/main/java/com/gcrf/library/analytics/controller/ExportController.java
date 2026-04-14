package com.gcrf.library.analytics.controller;

import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 报表导出控制器
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Tag(name = "报表导出", description = "Excel/PDF报表导出相关接口")
@RestController
@RequestMapping("/api/v1/analytics/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * 导出借阅统计Excel
     */
    @Operation(summary = "导出借阅统计", description = "导出借阅趋势统计数据到Excel")
    @GetMapping("/borrow-statistics")
    public void exportBorrowStatistics(
            @Valid TrendQueryRequest request,
            HttpServletResponse response
    ) {
        log.info("导出借阅统计Excel: timeRange={}, granularity={}",
                request.getTimeRange(), request.getGranularity());
        exportService.exportBorrowStatisticsExcel(request, response);
    }

    /**
     * 导出热门图书Excel
     */
    @Operation(summary = "导出热门图书", description = "导出热门图书排行数据到Excel")
    @GetMapping("/popular-books")
    public void exportPopularBooks(
            @Valid RankingQueryRequest request,
            HttpServletResponse response
    ) {
        log.info("导出热门图书Excel: rankBy={}, limit={}",
                request.getRankBy(), request.getLimit());
        exportService.exportPopularBooksExcel(request, response);
    }

    /**
     * 导出活跃读者Excel
     */
    @Operation(summary = "导出活跃读者", description = "导出活跃读者排行数据到Excel")
    @GetMapping("/active-readers")
    public void exportActiveReaders(
            @Valid RankingQueryRequest request,
            HttpServletResponse response
    ) {
        log.info("导出活跃读者Excel: rankBy={}, limit={}",
                request.getRankBy(), request.getLimit());
        exportService.exportActiveReadersExcel(request, response);
    }

    /**
     * 导出分类统计Excel
     */
    @Operation(summary = "导出分类统计", description = "导出图书分类统计数据到Excel")
    @GetMapping("/category-stats")
    public void exportCategoryStats(HttpServletResponse response) {
        log.info("导出分类统计Excel");
        exportService.exportCategoryStatsExcel(response);
    }

    /**
     * 导出综合报告PDF（暂未实现）
     */
    @Operation(summary = "导出综合报告", description = "导出图书馆综合分析报告到PDF")
    @GetMapping("/comprehensive-report")
    public void exportComprehensiveReport(HttpServletResponse response) {
        log.info("导出综合报告PDF");
        exportService.exportComprehensiveReportPdf(response);
    }
}
