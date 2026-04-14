package com.gcrf.library.recommend.algorithm;

import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.mapper.BorrowHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合推荐算法
 *
 * 结合多种推荐策略：
 * - User-based CF: 发现新领域的图书
 * - Item-based CF: 推荐相似图书
 * - Popular: 补充热门图书，解决冷启动
 *
 * 加权策略：
 * - 有丰富借阅历史的用户：CF权重高
 * - 新用户：Popular权重高
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRecommender {

    private final UserBasedCF userBasedCF;
    private final ItemBasedCF itemBasedCF;
    private final PopularRecommender popularRecommender;
    private final BorrowHistoryMapper borrowHistoryMapper;

    // 默认权重配置
    private static final double DEFAULT_USER_CF_WEIGHT = 0.4;
    private static final double DEFAULT_ITEM_CF_WEIGHT = 0.4;
    private static final double DEFAULT_POPULAR_WEIGHT = 0.2;

    // 新用户权重配置（借阅少于5本）
    private static final double NEW_USER_USER_CF_WEIGHT = 0.2;
    private static final double NEW_USER_ITEM_CF_WEIGHT = 0.3;
    private static final double NEW_USER_POPULAR_WEIGHT = 0.5;

    // 借阅阈值
    private static final int NEW_USER_THRESHOLD = 5;

    /**
     * 混合推荐
     *
     * @param readerId 读者ID
     * @param limit 推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommend(Long readerId, int limit) {
        log.info("HybridRecommender: Generating hybrid recommendations for reader {}", readerId);

        // 判断用户类型并设置权重
        int borrowCount = borrowHistoryMapper.findBookIdsByReaderId(readerId).size();
        double userCfWeight, itemCfWeight, popularWeight;

        if (borrowCount < NEW_USER_THRESHOLD) {
            log.info("Reader {} is a new user with {} borrows, using new user weights", readerId, borrowCount);
            userCfWeight = NEW_USER_USER_CF_WEIGHT;
            itemCfWeight = NEW_USER_ITEM_CF_WEIGHT;
            popularWeight = NEW_USER_POPULAR_WEIGHT;
        } else {
            userCfWeight = DEFAULT_USER_CF_WEIGHT;
            itemCfWeight = DEFAULT_ITEM_CF_WEIGHT;
            popularWeight = DEFAULT_POPULAR_WEIGHT;
        }

        // 获取各算法的推荐结果
        int fetchLimit = limit * 2; // 多获取一些用于去重和融合

        List<RecommendationVO> userCfRecs = Collections.emptyList();
        List<RecommendationVO> itemCfRecs = Collections.emptyList();
        List<RecommendationVO> popularRecs = Collections.emptyList();

        try {
            userCfRecs = userBasedCF.recommend(readerId, fetchLimit);
        } catch (Exception e) {
            log.warn("User-based CF failed for reader {}: {}", readerId, e.getMessage());
        }

        try {
            itemCfRecs = itemBasedCF.recommend(readerId, fetchLimit);
        } catch (Exception e) {
            log.warn("Item-based CF failed for reader {}: {}", readerId, e.getMessage());
        }

        try {
            popularRecs = popularRecommender.recommendForUser(readerId, fetchLimit);
        } catch (Exception e) {
            log.warn("Popular recommender failed for reader {}: {}", readerId, e.getMessage());
        }

        // 融合推荐结果
        Map<Long, HybridScore> scoreMap = new HashMap<>();

        // 添加User-based CF结果
        for (RecommendationVO rec : userCfRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addUserCfScore(rec.getScore(), userCfWeight, rec.getReason());
        }

        // 添加Item-based CF结果
        for (RecommendationVO rec : itemCfRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addItemCfScore(rec.getScore(), itemCfWeight, rec.getReason());
        }

        // 添加Popular结果
        for (RecommendationVO rec : popularRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addPopularScore(rec.getScore(), popularWeight, rec.getReason());
        }

        // 计算最终分数并排序
        return scoreMap.entrySet().stream()
                .map(entry -> {
                    RecommendationVO rec = new RecommendationVO();
                    rec.setBookId(entry.getKey());
                    rec.setScore(entry.getValue().getFinalScore());
                    rec.setAlgorithm("HYBRID");
                    rec.setReason(entry.getValue().getBestReason());
                    return rec;
                })
                .sorted(Comparator.comparingDouble(RecommendationVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 带自定义权重的混合推荐
     */
    public List<RecommendationVO> recommend(
            Long readerId,
            int limit,
            double userCfWeight,
            double itemCfWeight,
            double popularWeight) {

        log.info("HybridRecommender: Custom weights - UserCF:{}, ItemCF:{}, Popular:{}",
                userCfWeight, itemCfWeight, popularWeight);

        // 归一化权重
        double total = userCfWeight + itemCfWeight + popularWeight;
        userCfWeight /= total;
        itemCfWeight /= total;
        popularWeight /= total;

        int fetchLimit = limit * 2;

        List<RecommendationVO> userCfRecs = userBasedCF.recommend(readerId, fetchLimit);
        List<RecommendationVO> itemCfRecs = itemBasedCF.recommend(readerId, fetchLimit);
        List<RecommendationVO> popularRecs = popularRecommender.recommendForUser(readerId, fetchLimit);

        Map<Long, HybridScore> scoreMap = new HashMap<>();

        for (RecommendationVO rec : userCfRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addUserCfScore(rec.getScore(), userCfWeight, rec.getReason());
        }

        for (RecommendationVO rec : itemCfRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addItemCfScore(rec.getScore(), itemCfWeight, rec.getReason());
        }

        for (RecommendationVO rec : popularRecs) {
            scoreMap.computeIfAbsent(rec.getBookId(), k -> new HybridScore())
                    .addPopularScore(rec.getScore(), popularWeight, rec.getReason());
        }

        return scoreMap.entrySet().stream()
                .map(entry -> {
                    RecommendationVO rec = new RecommendationVO();
                    rec.setBookId(entry.getKey());
                    rec.setScore(entry.getValue().getFinalScore());
                    rec.setAlgorithm("HYBRID");
                    rec.setReason(entry.getValue().getBestReason());
                    return rec;
                })
                .sorted(Comparator.comparingDouble(RecommendationVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 混合分数计算辅助类
     */
    private static class HybridScore {
        private double userCfScore = 0;
        private double itemCfScore = 0;
        private double popularScore = 0;
        private double userCfWeight = 0;
        private double itemCfWeight = 0;
        private double popularWeight = 0;
        private String userCfReason;
        private String itemCfReason;
        private String popularReason;

        void addUserCfScore(double score, double weight, String reason) {
            this.userCfScore = score;
            this.userCfWeight = weight;
            this.userCfReason = reason;
        }

        void addItemCfScore(double score, double weight, String reason) {
            this.itemCfScore = score;
            this.itemCfWeight = weight;
            this.itemCfReason = reason;
        }

        void addPopularScore(double score, double weight, String reason) {
            this.popularScore = score;
            this.popularWeight = weight;
            this.popularReason = reason;
        }

        double getFinalScore() {
            double totalWeight = userCfWeight + itemCfWeight + popularWeight;
            if (totalWeight == 0) return 0;

            return (userCfScore * userCfWeight +
                    itemCfScore * itemCfWeight +
                    popularScore * popularWeight) / totalWeight;
        }

        String getBestReason() {
            // 返回权重最高的推荐理由
            if (userCfWeight >= itemCfWeight && userCfWeight >= popularWeight && userCfReason != null) {
                return userCfReason;
            }
            if (itemCfWeight >= userCfWeight && itemCfWeight >= popularWeight && itemCfReason != null) {
                return itemCfReason;
            }
            if (popularReason != null) {
                return popularReason;
            }
            return "综合推荐";
        }
    }
}
