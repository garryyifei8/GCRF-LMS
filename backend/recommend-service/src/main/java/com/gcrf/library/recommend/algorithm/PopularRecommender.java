package com.gcrf.library.recommend.algorithm;

import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.mapper.BorrowHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 热门推荐算法
 *
 * 基于时间窗口内的借阅统计推荐热门图书
 * 适用于新用户或没有足够借阅历史的用户（冷启动问题）
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularRecommender {

    private final BorrowHistoryMapper borrowHistoryMapper;

    @Value("${recommend.algorithm.popular.time-window-days:30}")
    private int timeWindowDays;

    @Value("${recommend.algorithm.popular.max-results:50}")
    private int maxResults;

    /**
     * 获取热门图书推荐
     *
     * @param limit 推荐数量
     * @param excludeBookIds 需要排除的图书ID（如用户已借阅的）
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommend(int limit, Set<Long> excludeBookIds) {
        log.info("PopularRecommender: Generating popular book recommendations");

        LocalDateTime startTime = LocalDateTime.now().minusDays(timeWindowDays);
        List<Map<String, Object>> popularBooks = borrowHistoryMapper.findPopularBooks(
                startTime, maxResults);

        if (popularBooks.isEmpty()) {
            log.info("No popular books found in time window");
            return Collections.emptyList();
        }

        // 获取最高借阅次数用于归一化
        long maxBorrowCount = popularBooks.stream()
                .mapToLong(m -> ((Number) m.get("borrow_count")).longValue())
                .max()
                .orElse(1);

        return popularBooks.stream()
                .filter(m -> {
                    Long bookId = ((Number) m.get("book_id")).longValue();
                    return excludeBookIds == null || !excludeBookIds.contains(bookId);
                })
                .limit(limit)
                .map(m -> {
                    Long bookId = ((Number) m.get("book_id")).longValue();
                    long borrowCount = ((Number) m.get("borrow_count")).longValue();

                    // 归一化分数到 [0, 1]
                    double score = (double) borrowCount / maxBorrowCount;

                    RecommendationVO rec = new RecommendationVO();
                    rec.setBookId(bookId);
                    rec.setScore(score);
                    rec.setAlgorithm("POPULAR");
                    rec.setReason(String.format("近%d天内有%d位读者借阅", timeWindowDays, borrowCount));
                    return rec;
                })
                .collect(Collectors.toList());
    }

    /**
     * 为指定用户获取热门推荐（排除已借阅）
     *
     * @param readerId 读者ID
     * @param limit 推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommendForUser(Long readerId, int limit) {
        // 获取用户已借阅的图书
        Set<Long> borrowedBooks = new HashSet<>(
                borrowHistoryMapper.findBookIdsByReaderId(readerId));

        return recommend(limit, borrowedBooks);
    }

    /**
     * 按分类获取热门图书
     *
     * @param categoryCode 分类代码
     * @param limit 推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommendByCategory(String categoryCode, int limit) {
        log.info("PopularRecommender: Getting popular books for category {}", categoryCode);

        // 这里简化处理，实际应添加按分类查询的方法
        // 暂时返回全局热门
        return recommend(limit, null);
    }
}
