package com.gcrf.library.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.gcrf.library.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final UserBasedCF userBasedCF;
    private final ItemBasedCF itemBasedCF;
    private final PopularRecommender popularRecommender;
    private final HybridRecommender hybridRecommender;
    private final BorrowHistoryMapper borrowHistoryMapper;
    private final RecommendationLogMapper recommendationLogMapper;

    @Override
    @Cacheable(value = "recommendations", key = "#readerId + ':' + #algorithm + ':' + #limit",
               unless = "#result.isEmpty()")
    public List<RecommendationVO> recommendForReader(Long readerId, int limit, String algorithm, String scene) {
        log.info("Generating recommendations for reader {} with algorithm {}", readerId, algorithm);

        List<RecommendationVO> recommendations;

        switch (algorithm.toUpperCase()) {
            case "USER_CF" -> recommendations = userBasedCF.recommend(readerId, limit);
            case "ITEM_CF" -> recommendations = itemBasedCF.recommend(readerId, limit);
            case "POPULAR" -> recommendations = popularRecommender.recommendForUser(readerId, limit);
            case "HYBRID" -> recommendations = hybridRecommender.recommend(readerId, limit);
            default -> {
                log.warn("Unknown algorithm {}, falling back to HYBRID", algorithm);
                recommendations = hybridRecommender.recommend(readerId, limit);
            }
        }

        // 设置场景和读者信息
        for (RecommendationVO rec : recommendations) {
            rec.setReaderId(readerId);
            rec.setScene(scene);
        }

        // 异步记录推荐日志
        logRecommendationsAsync(recommendations);

        return recommendations;
    }

    @Override
    @Cacheable(value = "popular-books", key = "#limit", unless = "#result.isEmpty()")
    public List<RecommendationVO> getPopularBooks(int limit) {
        log.info("Getting popular books, limit: {}", limit);
        return popularRecommender.recommend(limit, null);
    }

    @Override
    @Cacheable(value = "similar-books", key = "#bookId + ':' + #limit", unless = "#result.isEmpty()")
    public List<RecommendationVO> getSimilarBooks(Long bookId, int limit) {
        log.info("Getting similar books for book {}, limit: {}", bookId, limit);
        return itemBasedCF.findSimilarBooks(bookId, limit);
    }

    @Override
    public PageResult<RecommendationVO> batchRecommend(BatchRecommendRequest request) {
        log.info("Batch recommend with request: {}", request);

        // 获取所有有借阅记录的读者
        List<Long> allReaders = borrowHistoryMapper.findAllReaderIds();

        // TODO: 根据readerType过滤读者（需要调用reader-service）
        // 这里简化处理，暂时不过滤

        // 分页处理
        int start = (request.getPageNum() - 1) * request.getPageSize();
        int end = Math.min(start + request.getPageSize(), allReaders.size());

        if (start >= allReaders.size()) {
            return PageResult.ofRecords((long) allReaders.size(), request.getPageNum(),
                    request.getPageSize(), Collections.emptyList());
        }

        List<Long> pagedReaders = allReaders.subList(start, end);

        // 为每位读者生成推荐
        List<RecommendationVO> allRecommendations = new ArrayList<>();
        for (Long readerId : pagedReaders) {
            List<RecommendationVO> recs = recommendForReader(
                    readerId,
                    request.getCountPerReader(),
                    request.getAlgorithm(),
                    request.getScene()
            );
            allRecommendations.addAll(recs);
        }

        // 按分数排序
        allRecommendations.sort(Comparator.comparingDouble(RecommendationVO::getScore).reversed());

        return PageResult.ofRecords(
                (long) allReaders.size() * request.getCountPerReader(),
                request.getPageNum(),
                request.getPageSize(),
                allRecommendations
        );
    }

    @Override
    public RecommendStatsVO getRecommendStats(int days) {
        log.info("Getting recommendation stats for last {} days", days);

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);

        Map<String, Object> stats = recommendationLogMapper.getRecommendationStats(startTime);
        List<Map<String, Object>> algorithmStats = recommendationLogMapper.getStatsByAlgorithm(startTime);

        RecommendStatsVO vo = new RecommendStatsVO();

        if (stats != null) {
            vo.setTotalRecommendations(getLongValue(stats, "total_recommendations"));
            vo.setClickedCount(getLongValue(stats, "clicked_count"));
            vo.setBorrowedCount(getLongValue(stats, "borrowed_count"));
            vo.setCtr(getDoubleValue(stats, "click_rate"));
            vo.setConversion(getDoubleValue(stats, "conversion_rate"));
            vo.setPrecision(vo.getConversion()); // 准确率用借阅转化率近似
        }

        if (algorithmStats != null && !algorithmStats.isEmpty()) {
            List<RecommendStatsVO.AlgorithmStats> algoStatsList = algorithmStats.stream()
                    .map(m -> {
                        RecommendStatsVO.AlgorithmStats as = new RecommendStatsVO.AlgorithmStats();
                        as.setAlgorithm((String) m.get("algorithm"));
                        as.setTotal(getLongValue(m, "total"));
                        as.setClicked(getLongValue(m, "clicked"));
                        as.setBorrowed(getLongValue(m, "borrowed"));
                        as.setPrecision(getDoubleValue(m, "precision"));
                        return as;
                    })
                    .collect(Collectors.toList());
            vo.setAlgorithmStats(algoStatsList);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordClick(Long readerId, Long bookId) {
        log.info("Recording click: reader={}, book={}", readerId, bookId);

        recommendationLogMapper.update(null,
                new LambdaUpdateWrapper<RecommendationLog>()
                        .eq(RecommendationLog::getReaderId, readerId)
                        .eq(RecommendationLog::getBookId, bookId)
                        .eq(RecommendationLog::getClicked, false)
                        .set(RecommendationLog::getClicked, true)
                        .set(RecommendationLog::getClickedAt, LocalDateTime.now())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBorrow(Long readerId, Long bookId) {
        log.info("Recording borrow: reader={}, book={}", readerId, bookId);

        recommendationLogMapper.update(null,
                new LambdaUpdateWrapper<RecommendationLog>()
                        .eq(RecommendationLog::getReaderId, readerId)
                        .eq(RecommendationLog::getBookId, bookId)
                        .eq(RecommendationLog::getBorrowed, false)
                        .set(RecommendationLog::getBorrowed, true)
                        .set(RecommendationLog::getBorrowedAt, LocalDateTime.now())
        );
    }

    @Override
    @CacheEvict(value = {"recommendations", "similar-books", "popular-books"}, allEntries = true)
    public void recomputeSimilarityMatrix() {
        log.info("Starting similarity matrix recomputation...");

        try {
            userBasedCF.computeUserSimilarityMatrix();
            log.info("User similarity matrix computed successfully");
        } catch (Exception e) {
            log.error("Failed to compute user similarity matrix", e);
        }

        try {
            itemBasedCF.computeItemSimilarityMatrix();
            log.info("Item similarity matrix computed successfully");
        } catch (Exception e) {
            log.error("Failed to compute item similarity matrix", e);
        }

        log.info("Similarity matrix recomputation completed");
    }

    /**
     * 异步记录推荐日志
     */
    @Async
    protected void logRecommendationsAsync(List<RecommendationVO> recommendations) {
        LocalDateTime now = LocalDateTime.now();

        for (RecommendationVO rec : recommendations) {
            try {
                RecommendationLog logEntity = new RecommendationLog();
                logEntity.setReaderId(rec.getReaderId());
                logEntity.setBookId(rec.getBookId());
                logEntity.setScore(rec.getScore());
                logEntity.setAlgorithm(rec.getAlgorithm());
                logEntity.setScene(rec.getScene());
                logEntity.setReason(rec.getReason());
                logEntity.setClicked(false);
                logEntity.setBorrowed(false);
                logEntity.setRecommendedAt(now);

                recommendationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.warn("Failed to log recommendation: {}", e.getMessage());
            }
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
