package com.gcrf.library.analytics.service;

import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.dto.response.*;

import java.util.List;

/**
 * 数据分析服务接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
public interface AnalyticsService {

    /**
     * 获取总览统计数据
     *
     * @return 总览统计VO
     */
    OverviewVO getOverview();

    /**
     * 获取借阅趋势数据
     *
     * @param request 查询请求
     * @return 借阅趋势列表
     */
    List<BorrowTrendVO> getBorrowTrends(TrendQueryRequest request);

    /**
     * 获取热门图书排行
     *
     * @param request 查询请求
     * @return 热门图书列表
     */
    List<PopularBookVO> getPopularBooks(RankingQueryRequest request);

    /**
     * 获取活跃读者排行
     *
     * @param request 查询请求
     * @return 活跃读者列表
     */
    List<ActiveReaderVO> getActiveReaders(RankingQueryRequest request);

    /**
     * 获取分类分布数据
     *
     * @return 分类分布列表
     */
    List<CategoryDistributionVO> getCategoryDistribution();

    /**
     * 获取读者活跃度热力图数据
     *
     * @return 热力图数据
     */
    HeatmapDataVO getReaderActivityHeatmap();

    /**
     * 获取分类统计数据（仪表盘用，与分类分布数据相同结构）
     *
     * @return 分类统计列表
     */
    List<CategoryDistributionVO> getCategoryStats();

    /**
     * 获取馆藏分析数据
     *
     * @return 馆藏分析VO
     */
    CollectionAnalysisVO getCollectionAnalysis();

    /**
     * 获取近期活动记录
     *
     * @param limit 返回条数，将被夹紧至 [1, 100]，传入 ≤0 时默认使用 20
     * @return 近期活动列表
     */
    List<RecentActivityVO> getRecentActivities(int limit);
}
