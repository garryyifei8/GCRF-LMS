package com.gcrf.library.analytics.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.dto.response.*;
import com.gcrf.library.analytics.service.AnalyticsService;
import com.gcrf.library.analytics.service.ExportService;
import com.gcrf.library.common.exception.SystemException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 导出服务实现类
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final AnalyticsService analyticsService;

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void exportBorrowStatisticsExcel(TrendQueryRequest request, HttpServletResponse response) {
        log.info("导出借阅统计Excel: timeRange={}, granularity={}", request.getTimeRange(), request.getGranularity());

        try {
            // 获取数据
            List<BorrowTrendVO> trends = analyticsService.getBorrowTrends(request);

            // 转换为Excel VO
            List<BorrowTrendExcelVO> excelData = trends.stream()
                    .map(this::convertToBorrowTrendExcelVO)
                    .collect(Collectors.toList());

            // 设置响应头
            String fileName = "借阅统计_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
            setExcelResponseHeader(response, fileName);

            // 写入Excel
            EasyExcel.write(response.getOutputStream(), BorrowTrendExcelVO.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("借阅统计")
                    .doWrite(excelData);

            log.info("借阅统计Excel导出成功: {} 条记录", excelData.size());
        } catch (IOException e) {
            log.error("导出借阅统计Excel失败", e);
            throw new SystemException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public void exportPopularBooksExcel(RankingQueryRequest request, HttpServletResponse response) {
        log.info("导出热门图书Excel: rankBy={}, limit={}", request.getRankBy(), request.getLimit());

        try {
            // 获取数据
            List<PopularBookVO> books = analyticsService.getPopularBooks(request);

            // 转换为Excel VO
            List<PopularBookExcelVO> excelData = books.stream()
                    .map(this::convertToPopularBookExcelVO)
                    .collect(Collectors.toList());

            // 设置响应头
            String fileName = "热门图书排行_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
            setExcelResponseHeader(response, fileName);

            // 写入Excel
            EasyExcel.write(response.getOutputStream(), PopularBookExcelVO.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("热门图书排行")
                    .doWrite(excelData);

            log.info("热门图书Excel导出成功: {} 条记录", excelData.size());
        } catch (IOException e) {
            log.error("导出热门图书Excel失败", e);
            throw new SystemException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public void exportActiveReadersExcel(RankingQueryRequest request, HttpServletResponse response) {
        log.info("导出活跃读者Excel: rankBy={}, limit={}", request.getRankBy(), request.getLimit());

        try {
            // 获取数据
            List<ActiveReaderVO> readers = analyticsService.getActiveReaders(request);

            // 转换为Excel VO
            List<ActiveReaderExcelVO> excelData = readers.stream()
                    .map(this::convertToActiveReaderExcelVO)
                    .collect(Collectors.toList());

            // 设置响应头
            String fileName = "活跃读者排行_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
            setExcelResponseHeader(response, fileName);

            // 写入Excel
            EasyExcel.write(response.getOutputStream(), ActiveReaderExcelVO.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("活跃读者排行")
                    .doWrite(excelData);

            log.info("活跃读者Excel导出成功: {} 条记录", excelData.size());
        } catch (IOException e) {
            log.error("导出活跃读者Excel失败", e);
            throw new SystemException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public void exportCategoryStatsExcel(HttpServletResponse response) {
        log.info("导出分类统计Excel");

        try {
            // 获取数据
            List<CategoryDistributionVO> categories = analyticsService.getCategoryDistribution();

            // 转换为Excel VO
            List<CategoryDistributionExcelVO> excelData = categories.stream()
                    .map(this::convertToCategoryDistributionExcelVO)
                    .collect(Collectors.toList());

            // 设置响应头
            String fileName = "分类统计_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
            setExcelResponseHeader(response, fileName);

            // 写入Excel
            EasyExcel.write(response.getOutputStream(), CategoryDistributionExcelVO.class)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("分类统计")
                    .doWrite(excelData);

            log.info("分类统计Excel导出成功: {} 条记录", excelData.size());
        } catch (IOException e) {
            log.error("导出分类统计Excel失败", e);
            throw new SystemException("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public void exportComprehensiveReportPdf(HttpServletResponse response) {
        log.info("导出综合报告Excel（多Sheet）");

        try {
            TrendQueryRequest trendReq = new TrendQueryRequest();
            RankingQueryRequest popularReq = new RankingQueryRequest();
            popularReq.setLimit(20);
            RankingQueryRequest readerReq = new RankingQueryRequest();
            readerReq.setLimit(20);

            List<BorrowTrendExcelVO> trendData = analyticsService.getBorrowTrends(trendReq).stream()
                    .map(this::convertToBorrowTrendExcelVO)
                    .collect(Collectors.toList());
            List<PopularBookExcelVO> bookData = analyticsService.getPopularBooks(popularReq).stream()
                    .map(this::convertToPopularBookExcelVO)
                    .collect(Collectors.toList());
            List<ActiveReaderExcelVO> readerData = analyticsService.getActiveReaders(readerReq).stream()
                    .map(this::convertToActiveReaderExcelVO)
                    .collect(Collectors.toList());
            List<CategoryDistributionExcelVO> categoryData = analyticsService.getCategoryDistribution().stream()
                    .map(this::convertToCategoryDistributionExcelVO)
                    .collect(Collectors.toList());

            String fileName = "综合分析报告_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
            setExcelResponseHeader(response, fileName);

            try (com.alibaba.excel.ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .build()) {
                writer.write(trendData,
                        EasyExcel.writerSheet(0, "借阅趋势").head(BorrowTrendExcelVO.class).build());
                writer.write(bookData,
                        EasyExcel.writerSheet(1, "热门图书").head(PopularBookExcelVO.class).build());
                writer.write(readerData,
                        EasyExcel.writerSheet(2, "活跃读者").head(ActiveReaderExcelVO.class).build());
                writer.write(categoryData,
                        EasyExcel.writerSheet(3, "分类统计").head(CategoryDistributionExcelVO.class).build());
            }

            log.info("综合报告Excel导出成功: 趋势{}/图书{}/读者{}/分类{}",
                    trendData.size(), bookData.size(), readerData.size(), categoryData.size());
        } catch (IOException e) {
            log.error("导出综合报告Excel失败", e);
            throw new SystemException("导出Excel失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 设置Excel响应头
     */
    private void setExcelResponseHeader(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
    }

    /**
     * 转换借阅趋势为Excel VO
     */
    private BorrowTrendExcelVO convertToBorrowTrendExcelVO(BorrowTrendVO vo) {
        return BorrowTrendExcelVO.builder()
                .dateStr(vo.getDateStr())
                .borrowed(vo.getBorrowed())
                .returned(vo.getReturned())
                .visits(vo.getVisits())
                .newReaders(vo.getNewReaders())
                .reserved(vo.getReserved())
                .renewed(vo.getRenewed())
                .build();
    }

    /**
     * 转换热门图书为Excel VO
     */
    private PopularBookExcelVO convertToPopularBookExcelVO(PopularBookVO vo) {
        return PopularBookExcelVO.builder()
                .rank(vo.getRank())
                .isbn(vo.getIsbn())
                .title(vo.getTitle())
                .author(vo.getAuthor())
                .categoryName(vo.getCategoryName())
                .borrowCount(vo.getBorrowCount())
                .rating(vo.getRating())
                .totalCopies(vo.getTotalCopies())
                .availableCopies(vo.getAvailableCopies())
                .reservationCount(vo.getReservationCount())
                .build();
    }

    /**
     * 转换活跃读者为Excel VO
     */
    private ActiveReaderExcelVO convertToActiveReaderExcelVO(ActiveReaderVO vo) {
        return ActiveReaderExcelVO.builder()
                .rank(vo.getRank())
                .cardNo(vo.getCardNo())
                .realName(vo.getRealName())
                .readerTypeName(vo.getReaderTypeName())
                .borrowCount(vo.getBorrowCount())
                .visitCount(vo.getVisitCount())
                .favoriteCategory(vo.getFavoriteCategory())
                .currentBorrowCount(vo.getCurrentBorrowCount())
                .overdueCount(vo.getOverdueCount())
                .build();
    }

    /**
     * 转换分类分布为Excel VO
     */
    private CategoryDistributionExcelVO convertToCategoryDistributionExcelVO(CategoryDistributionVO vo) {
        return CategoryDistributionExcelVO.builder()
                .code(vo.getCode())
                .name(vo.getName())
                .bookCount(vo.getBookCount())
                .borrowCount(vo.getBorrowCount())
                .circulationRate(vo.getCirculationRate())
                .readerCount(vo.getReaderCount())
                .percentage(vo.getPercentage())
                .zeroCirculationCount(vo.getZeroCirculationCount())
                .zeroCirculationRate(vo.getZeroCirculationRate())
                .build();
    }
}
