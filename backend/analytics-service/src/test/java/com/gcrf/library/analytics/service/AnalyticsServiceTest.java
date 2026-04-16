package com.gcrf.library.analytics.service;

import com.gcrf.library.analytics.client.BookServiceClient;
import com.gcrf.library.analytics.client.CirculationServiceClient;
import com.gcrf.library.analytics.client.ReaderServiceClient;
import com.gcrf.library.analytics.dto.request.RankingQueryRequest;
import com.gcrf.library.analytics.dto.request.TrendQueryRequest;
import com.gcrf.library.analytics.dto.response.ActiveReaderVO;
import com.gcrf.library.analytics.dto.response.BorrowTrendVO;
import com.gcrf.library.analytics.dto.response.CategoryDistributionVO;
import com.gcrf.library.analytics.dto.response.HeatmapDataVO;
import com.gcrf.library.analytics.dto.response.OverviewVO;
import com.gcrf.library.analytics.dto.response.PopularBookVO;
import com.gcrf.library.analytics.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnalyticsService 单元测试
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private CirculationServiceClient circulationServiceClient;

    @Mock
    private ReaderServiceClient readerServiceClient;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    @DisplayName("getOverview_normalCase_shouldReturnValidOverview")
    void getOverview_normalCase_shouldReturnValidOverview() {
        // Act
        OverviewVO result = analyticsService.getOverview();

        // Assert
        assertNotNull(result);
        assertEquals(10000L, result.getTotalBooks());
        assertEquals(5000L, result.getTotalReaders());
        assertNotNull(result.getBorrowGrowth());
        assertTrue(result.getCurrentBorrowed() > 0);
    }

    @Test
    @DisplayName("getBorrowTrends_last7Days_shouldReturn7DataPoints")
    void getBorrowTrends_last7Days_shouldReturn7DataPoints() {
        // Arrange
        TrendQueryRequest request = new TrendQueryRequest();
        request.setTimeRange("LAST_7_DAYS");
        request.setGranularity("DAILY");

        // Act
        List<BorrowTrendVO> result = analyticsService.getBorrowTrends(request);

        // Assert
        assertNotNull(result);
        assertEquals(7, result.size());
        result.forEach(trend -> {
            assertNotNull(trend.getDate());
            assertNotNull(trend.getDateStr());
            assertTrue(trend.getBorrowed() >= 0);
            assertTrue(trend.getReturned() >= 0);
        });
    }

    @Test
    @DisplayName("getBorrowTrends_last30Days_shouldReturn30DataPoints")
    void getBorrowTrends_last30Days_shouldReturn30DataPoints() {
        // Arrange
        TrendQueryRequest request = new TrendQueryRequest();
        request.setTimeRange("LAST_30_DAYS");
        request.setGranularity("DAILY");

        // Act
        List<BorrowTrendVO> result = analyticsService.getBorrowTrends(request);

        // Assert
        assertNotNull(result);
        assertEquals(30, result.size());
    }

    @Test
    @DisplayName("getPopularBooks_withLimit10_shouldReturn10Books")
    void getPopularBooks_withLimit10_shouldReturn10Books() {
        // Arrange
        RankingQueryRequest request = new RankingQueryRequest();
        request.setRankBy("BORROW_COUNT");
        request.setLimit(10);
        request.setTimeRange("THIS_MONTH");

        // Act
        List<PopularBookVO> result = analyticsService.getPopularBooks(request);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size());
        // Ranks should be sequential starting at 1
        for (int i = 0; i < result.size(); i++) {
            assertEquals(i + 1, result.get(i).getRank());
        }
        result.forEach(book -> {
            assertNotNull(book.getTitle());
            assertNotNull(book.getAuthor());
            assertTrue(book.getBorrowCount() > 0);
        });
    }

    @Test
    @DisplayName("getActiveReaders_withLimit5_shouldReturn5Readers")
    void getActiveReaders_withLimit5_shouldReturn5Readers() {
        // Arrange
        RankingQueryRequest request = new RankingQueryRequest();
        request.setRankBy("BORROW_COUNT");
        request.setLimit(5);
        request.setTimeRange("THIS_MONTH");

        // Act
        List<ActiveReaderVO> result = analyticsService.getActiveReaders(request);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        result.forEach(reader -> {
            assertNotNull(reader.getRealName());
            assertNotNull(reader.getCardNo());
            assertTrue(reader.getBorrowCount() > 0);
        });
    }

    @Test
    @DisplayName("getCategoryDistribution_normalCase_shouldReturnNonEmptyDistribution")
    void getCategoryDistribution_normalCase_shouldReturnNonEmptyDistribution() {
        // Act
        List<CategoryDistributionVO> result = analyticsService.getCategoryDistribution();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        result.forEach(dist -> {
            assertNotNull(dist.getCode());
            assertNotNull(dist.getName());
            assertTrue(dist.getBookCount() > 0);
            assertNotNull(dist.getCirculationRate());
        });
    }

    @Test
    @DisplayName("getReaderActivityHeatmap_normalCase_shouldReturnValidHeatmap")
    void getReaderActivityHeatmap_normalCase_shouldReturnValidHeatmap() {
        // Act
        HeatmapDataVO result = analyticsService.getReaderActivityHeatmap();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getHours());
        assertNotNull(result.getDays());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
        // 7 days x 12 hours = 84 data points
        assertEquals(84, result.getData().size());
        assertTrue(result.getMinValue() <= result.getMaxValue());
    }
}
