package com.gcrf.library.analytics.controller;

import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.dto.response.*;
import com.gcrf.library.analytics.service.AnalyticsService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分析控制器
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Tag(name = "数据分析", description = "统计报表与数据可视化相关接口")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * 获取总览统计数据
     */
    @Operation(summary = "获取总览统计", description = "获取图书馆总览统计数据，包括馆藏、借阅、读者等核心指标")
    @GetMapping("/overview")
    public Result<OverviewVO> getOverview() {
        log.info("获取总览统计数据");
        OverviewVO overview = analyticsService.getOverview();
        return Result.success(overview);
    }

    /**
     * 获取借阅趋势数据
     */
    @Operation(summary = "获取借阅趋势", description = "获取指定时间范围内的借阅趋势数据，支持日/周/月粒度")
    @GetMapping("/borrow-trends")
    public Result<List<BorrowTrendVO>> getBorrowTrends(@Valid TrendQueryRequest request) {
        log.info("获取借阅趋势数据: timeRange={}, granularity={}",
                request.getTimeRange(), request.getGranularity());
        List<BorrowTrendVO> trends = analyticsService.getBorrowTrends(request);
        return Result.success(trends);
    }

    /**
     * 获取热门图书排行
     */
    @Operation(summary = "获取热门图书", description = "获取热门图书排行榜，支持按借阅量、评分等排序")
    @GetMapping("/popular-books")
    public Result<List<PopularBookVO>> getPopularBooks(@Valid RankingQueryRequest request) {
        log.info("获取热门图书排行: rankBy={}, limit={}", request.getRankBy(), request.getLimit());
        List<PopularBookVO> books = analyticsService.getPopularBooks(request);
        return Result.success(books);
    }

    /**
     * 获取活跃读者排行
     */
    @Operation(summary = "获取活跃读者", description = "获取活跃读者排行榜，支持按借阅量、到馆次数等排序")
    @GetMapping("/active-readers")
    public Result<List<ActiveReaderVO>> getActiveReaders(@Valid RankingQueryRequest request) {
        log.info("获取活跃读者排行: rankBy={}, limit={}", request.getRankBy(), request.getLimit());
        List<ActiveReaderVO> readers = analyticsService.getActiveReaders(request);
        return Result.success(readers);
    }

    /**
     * 获取分类分布数据
     */
    @Operation(summary = "获取分类分布", description = "获取图书分类分布数据，包括各分类的图书数量、借阅量等")
    @GetMapping("/category-distribution")
    public Result<List<CategoryDistributionVO>> getCategoryDistribution() {
        log.info("获取分类分布数据");
        List<CategoryDistributionVO> distribution = analyticsService.getCategoryDistribution();
        return Result.success(distribution);
    }

    /**
     * 获取读者活跃度热力图数据
     */
    @Operation(summary = "获取活跃度热力图", description = "获取读者按时段的活跃度热力图数据")
    @GetMapping("/reader-heatmap")
    public Result<HeatmapDataVO> getReaderHeatmap() {
        log.info("获取读者活跃度热力图数据");
        HeatmapDataVO heatmap = analyticsService.getReaderActivityHeatmap();
        return Result.success(heatmap);
    }

    /**
     * 获取分类统计数据（仪表盘专用）
     */
    @Operation(summary = "获取分类统计", description = "获取图书分类统计数据，供仪表盘图表使用")
    @GetMapping("/category-stats")
    public Result<List<CategoryDistributionVO>> getCategoryStats() {
        log.info("获取分类统计数据");
        List<CategoryDistributionVO> stats = analyticsService.getCategoryStats();
        return Result.success(stats);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查分析服务是否正常运行")
    public Result<String> health() {
        return Result.success("Analytics Service is running");
    }
}
