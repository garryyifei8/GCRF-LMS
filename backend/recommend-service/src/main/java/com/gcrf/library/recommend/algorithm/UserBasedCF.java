package com.gcrf.library.recommend.algorithm;

import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.entity.UserSimilarity;
import com.gcrf.library.recommend.mapper.BorrowHistoryMapper;
import com.gcrf.library.recommend.mapper.UserSimilarityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于用户的协同过滤算法 (User-based Collaborative Filtering)
 *
 * 算法原理：
 * 1. 找到与目标用户借阅行为相似的用户群体（邻居）
 * 2. 推荐邻居用户借阅过但目标用户未借阅的图书
 * 3. 按加权评分排序，权重为用户相似度
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBasedCF {

    private final BorrowHistoryMapper borrowHistoryMapper;
    private final UserSimilarityMapper userSimilarityMapper;
    private final SimilarityCalculator similarityCalculator;

    @Value("${recommend.algorithm.user-cf.min-common-items:3}")
    private int minCommonItems;

    @Value("${recommend.algorithm.user-cf.max-neighbors:50}")
    private int maxNeighbors;

    @Value("${recommend.algorithm.user-cf.similarity-threshold:0.1}")
    private double similarityThreshold;

    /**
     * 为指定用户生成推荐
     *
     * @param readerId 读者ID
     * @param limit 推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommend(Long readerId, int limit) {
        log.info("UserBasedCF: Generating recommendations for reader {}", readerId);

        // 1. 获取用户已借阅的图书
        Set<Long> borrowedBooks = new HashSet<>(borrowHistoryMapper.findBookIdsByReaderId(readerId));
        if (borrowedBooks.isEmpty()) {
            log.info("Reader {} has no borrow history, returning empty recommendations", readerId);
            return Collections.emptyList();
        }

        // 2. 获取相似用户
        List<UserSimilarity> similarUsers = userSimilarityMapper.findTopSimilarUsers(
                readerId, maxNeighbors, similarityThreshold);

        if (similarUsers.isEmpty()) {
            log.info("No similar users found for reader {}", readerId);
            return Collections.emptyList();
        }

        // 3. 收集邻居用户借阅的图书及其加权评分
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> similaritySum = new HashMap<>();
        Map<Long, List<Long>> bookNeighbors = new HashMap<>(); // 记录推荐来源

        for (UserSimilarity sim : similarUsers) {
            Long neighborId = sim.getUserIdA().equals(readerId) ? sim.getUserIdB() : sim.getUserIdA();
            double similarity = sim.getSimilarityScore();

            // 获取邻居借阅的图书
            List<Map<String, Object>> neighborRatings = borrowHistoryMapper.findRatingVectorByReaderId(neighborId);

            for (Map<String, Object> rating : neighborRatings) {
                Long bookId = ((Number) rating.get("book_id")).longValue();
                Double ratingValue = ((Number) rating.get("rating")).doubleValue();

                // 排除用户已借阅的图书
                if (borrowedBooks.contains(bookId)) {
                    continue;
                }

                // 加权评分累加
                candidateScores.merge(bookId, similarity * ratingValue, Double::sum);
                similaritySum.merge(bookId, similarity, Double::sum);
                bookNeighbors.computeIfAbsent(bookId, k -> new ArrayList<>()).add(neighborId);
            }
        }

        // 4. 计算预测评分并排序
        List<RecommendationVO> recommendations = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long bookId = entry.getKey();
            double weightedSum = entry.getValue();
            double simSum = similaritySum.get(bookId);

            double predictedScore = simSum > 0 ? weightedSum / simSum : 0;
            // 归一化到 [0, 1]
            double normalizedScore = Math.min(1.0, predictedScore / 5.0);

            RecommendationVO rec = new RecommendationVO();
            rec.setBookId(bookId);
            rec.setScore(normalizedScore);
            rec.setAlgorithm("USER_CF");
            rec.setReason(generateReason(bookNeighbors.get(bookId).size()));
            recommendations.add(rec);
        }

        // 按分数降序排序，取前N个
        return recommendations.stream()
                .sorted(Comparator.comparingDouble(RecommendationVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 计算并存储用户相似度矩阵
     *
     * 用于离线批量计算，可通过定时任务调用
     */
    public void computeUserSimilarityMatrix() {
        log.info("Computing user similarity matrix...");
        long startTime = System.currentTimeMillis();

        List<Long> allUsers = borrowHistoryMapper.findAllReaderIds();
        log.info("Total users to process: {}", allUsers.size());

        // 预加载所有用户的评分向量
        Map<Long, Map<Long, Double>> userVectors = new HashMap<>();
        for (Long userId : allUsers) {
            Map<Long, Double> vector = borrowHistoryMapper.findRatingVectorByReaderId(userId)
                    .stream()
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("book_id")).longValue(),
                            m -> ((Number) m.get("rating")).doubleValue(),
                            (a, b) -> a
                    ));
            userVectors.put(userId, vector);
        }

        // 计算用户对之间的相似度
        List<UserSimilarity> similarities = new ArrayList<>();
        for (int i = 0; i < allUsers.size(); i++) {
            Long userA = allUsers.get(i);
            Map<Long, Double> vectorA = userVectors.get(userA);

            for (int j = i + 1; j < allUsers.size(); j++) {
                Long userB = allUsers.get(j);
                Map<Long, Double> vectorB = userVectors.get(userB);

                int commonCount = similarityCalculator.countCommonElements(vectorA, vectorB);
                if (commonCount < minCommonItems) {
                    continue;
                }

                double similarity = similarityCalculator.cosineSimilarity(vectorA, vectorB);
                if (similarity < similarityThreshold) {
                    continue;
                }

                UserSimilarity sim = new UserSimilarity();
                sim.setUserIdA(userA);
                sim.setUserIdB(userB);
                sim.setSimilarityScore(similarity);
                sim.setCommonItemsCount(commonCount);
                sim.setCalculatedAt(LocalDateTime.now());
                similarities.add(sim);
            }
        }

        // 批量保存（这里简化处理，实际应使用批量插入）
        for (UserSimilarity sim : similarities) {
            userSimilarityMapper.insert(sim);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("User similarity matrix computed. Total pairs: {}, Duration: {}ms",
                similarities.size(), duration);
    }

    /**
     * 生成推荐理由
     */
    private String generateReason(int neighborCount) {
        if (neighborCount >= 5) {
            return "多位与你阅读口味相似的读者都借阅了此书";
        } else if (neighborCount >= 3) {
            return "与你阅读偏好相近的读者推荐";
        } else {
            return "基于相似读者的借阅历史推荐";
        }
    }
}
