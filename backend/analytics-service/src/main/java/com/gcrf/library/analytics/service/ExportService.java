package com.gcrf.library.analytics.service;

import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 导出服务接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
public interface ExportService {

    /**
     * 导出借阅统计Excel
     *
     * @param request 查询请求
     * @param response HTTP响应
     */
    void exportBorrowStatisticsExcel(TrendQueryRequest request, HttpServletResponse response);

    /**
     * 导出热门图书Excel
     *
     * @param request 查询请求
     * @param response HTTP响应
     */
    void exportPopularBooksExcel(RankingQueryRequest request, HttpServletResponse response);

    /**
     * 导出活跃读者Excel
     *
     * @param request 查询请求
     * @param response HTTP响应
     */
    void exportActiveReadersExcel(RankingQueryRequest request, HttpServletResponse response);

    /**
     * 导出分类统计Excel
     *
     * @param response HTTP响应
     */
    void exportCategoryStatsExcel(HttpServletResponse response);

    /**
     * 导出综合报告PDF（暂不实现，预留接口）
     *
     * @param response HTTP响应
     */
    void exportComprehensiveReportPdf(HttpServletResponse response);
}
