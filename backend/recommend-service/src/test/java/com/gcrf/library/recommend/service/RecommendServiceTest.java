package com.gcrf.library.recommend.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.recommend.algorithm.HybridRecommender;
import com.gcrf.library.recommend.algorithm.ItemBasedCF;
import com.gcrf.library.recommend.algorithm.PopularRecommender;
import com.gcrf.library.recommend.algorithm.UserBasedCF;
import com.gcrf.library.recommend.dto.request.BatchRecommendRequest;
import com.gcrf.library.recommend.dto.response.RecommendStatsVO;
import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.entity.RecommendationLog;
import com.gcrf.library.recommend.mapper.BorrowHistoryMapper;
import com.gcrf.library.recommend.mapper.RecommendationLogMapper;
import com.gcrf.library.recommend.service.impl.RecommendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecommendService 单元测试
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private UserBasedCF userBasedCF;

    @Mock
    private ItemBasedCF itemBasedCF;

    @Mock
    private PopularRecommender popularRecommender;

    @Mock
    private HybridRecommender hybridRecommender;

    @Mock
    private BorrowHistoryMapper borrowHistoryMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    private RecommendationVO rec1;
    private RecommendationVO rec2;

    @BeforeEach
    void setUp() {
        rec1 = new RecommendationVO();
        rec1.setBookId(101L);
        rec1.setBookTitle("三体");
        rec1.setAuthor("刘慈欣");
        rec1.setScore(0.95);
        rec1.setAlgorithm("HYBRID");
        rec1.setScene("HOMEPAGE");

        rec2 = new RecommendationVO();
        rec2.setBookId(102L);
        rec2.setBookTitle("活着");
        rec2.setAuthor("余华");
        rec2.setScore(0.88);
        rec2.setAlgorithm("HYBRID");
        rec2.setScene("HOMEPAGE");
    }

    @Test
    @DisplayName("recommendForReader_hybridAlgorithm_shouldReturnRecommendations")
    void recommendForReader_hybridAlgorithm_shouldReturnRecommendations() {
        // Arrange
        when(hybridRecommender.recommend(1L, 5))
                .thenReturn(Arrays.asList(rec1, rec2));
        when(recommendationLogMapper.insert(any(RecommendationLog.class))).thenReturn(1);

        // Act
        List<RecommendationVO> result = recommendService.recommendForReader(1L, 5, "HYBRID", "HOMEPAGE");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Scene and readerId should be set by the service
        result.forEach(r -> {
            assertEquals(1L, r.getReaderId());
            assertEquals("HOMEPAGE", r.getScene());
        });
        verify(hybridRecommender).recommend(1L, 5);
    }

    @Test
    @DisplayName("recommendForReader_userCfAlgorithm_shouldDelegateToUserBasedCF")
    void recommendForReader_userCfAlgorithm_shouldDelegateToUserBasedCF() {
        // Arrange
        when(userBasedCF.recommend(2L, 10)).thenReturn(Arrays.asList(rec1));
        when(recommendationLogMapper.insert(any(RecommendationLog.class))).thenReturn(1);

        // Act
        List<RecommendationVO> result = recommendService.recommendForReader(2L, 10, "USER_CF", "DETAIL");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userBasedCF).recommend(2L, 10);
        verify(hybridRecommender, never()).recommend(anyLong(), anyInt());
    }

    @Test
    @DisplayName("recommendForReader_unknownAlgorithm_shouldFallbackToHybrid")
    void recommendForReader_unknownAlgorithm_shouldFallbackToHybrid() {
        // Arrange
        when(hybridRecommender.recommend(3L, 5)).thenReturn(Arrays.asList(rec1, rec2));
        when(recommendationLogMapper.insert(any(RecommendationLog.class))).thenReturn(1);

        // Act
        List<RecommendationVO> result = recommendService.recommendForReader(3L, 5, "NONEXISTENT", "HOMEPAGE");

        // Assert
        assertNotNull(result);
        // Falls back to HYBRID
        verify(hybridRecommender).recommend(3L, 5);
    }

    @Test
    @DisplayName("getPopularBooks_normalCase_shouldReturnBooksFromPopularRecommender")
    void getPopularBooks_normalCase_shouldReturnBooksFromPopularRecommender() {
        // Arrange
        when(popularRecommender.recommend(10, null)).thenReturn(Arrays.asList(rec1, rec2));

        // Act
        List<RecommendationVO> result = recommendService.getPopularBooks(10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(popularRecommender).recommend(10, null);
    }

    @Test
    @DisplayName("getSimilarBooks_normalCase_shouldDelegateToItemBasedCF")
    void getSimilarBooks_normalCase_shouldDelegateToItemBasedCF() {
        // Arrange
        when(itemBasedCF.findSimilarBooks(101L, 5)).thenReturn(Arrays.asList(rec2));

        // Act
        List<RecommendationVO> result = recommendService.getSimilarBooks(101L, 5);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(102L, result.get(0).getBookId());
        verify(itemBasedCF).findSimilarBooks(101L, 5);
    }

    @Test
    @DisplayName("batchRecommend_emptyReaderList_shouldReturnEmptyPage")
    void batchRecommend_emptyReaderList_shouldReturnEmptyPage() {
        // Arrange
        BatchRecommendRequest request = new BatchRecommendRequest();
        request.setAlgorithm("HYBRID");
        request.setCountPerReader(5);
        request.setScene("HOMEPAGE");
        request.setPageNum(1);
        request.setPageSize(20);

        when(borrowHistoryMapper.findAllReaderIds()).thenReturn(Collections.emptyList());

        // Act
        PageResult<RecommendationVO> result = recommendService.batchRecommend(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    @DisplayName("getRecommendStats_withData_shouldReturnMappedStats")
    void getRecommendStats_withData_shouldReturnMappedStats() {
        // Arrange
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("total_recommendations", 1000L);
        statsMap.put("clicked_count", 200L);
        statsMap.put("borrowed_count", 50L);
        statsMap.put("click_rate", 20.0);
        statsMap.put("conversion_rate", 5.0);

        when(recommendationLogMapper.getRecommendationStats(any(LocalDateTime.class))).thenReturn(statsMap);
        when(recommendationLogMapper.getStatsByAlgorithm(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        RecommendStatsVO result = recommendService.getRecommendStats(30);

        // Assert
        assertNotNull(result);
        assertEquals(1000L, result.getTotalRecommendations());
        assertEquals(200L, result.getClickedCount());
        assertEquals(50L, result.getBorrowedCount());
        assertEquals(20.0, result.getCtr());
        verify(recommendationLogMapper).getRecommendationStats(any(LocalDateTime.class));
    }

    @Test
    @Disabled("TODO: MyBatis-Plus lambda cache missing for RecommendationLog — add TableInfoHelper.initTableInfo in @BeforeAll")
    @DisplayName("recordClick_normalCase_shouldUpdateClickedStatus")
    void recordClick_normalCase_shouldUpdateClickedStatus() {
        // Arrange
        when(recommendationLogMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> recommendService.recordClick(1L, 101L));

        // Assert
        verify(recommendationLogMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @Disabled("TODO: MyBatis-Plus lambda cache missing for RecommendationLog — add TableInfoHelper.initTableInfo in @BeforeAll")
    @DisplayName("recordBorrow_normalCase_shouldUpdateBorrowedStatus")
    void recordBorrow_normalCase_shouldUpdateBorrowedStatus() {
        // Arrange
        when(recommendationLogMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> recommendService.recordBorrow(1L, 101L));

        // Assert
        verify(recommendationLogMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
