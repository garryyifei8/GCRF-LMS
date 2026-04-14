package com.gcrf.library.recommend.algorithm;

import com.gcrf.library.recommend.dto.response.RecommendationVO;
import com.gcrf.library.recommend.entity.ItemSimilarity;
import com.gcrf.library.recommend.mapper.BorrowHistoryMapper;
import com.gcrf.library.recommend.mapper.ItemSimilarityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于物品的协同过滤算法 (Item-based Collaborative Filtering)
 *
 * 算法原理：
 * 1. 计算图书之间的相似度（基于共同借阅用户）
 * 2. 根据用户历史借阅的图书，推荐与之相似的其他图书
 * 3. 按相似度加权排序
 *
 * 优点：
 * - 物品相似度相对稳定，可离线计算
 * - 推荐结果可解释性强（因为你借阅了X，推荐相似的Y）
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemBasedCF {

    private final BorrowHistoryMapper borrowHistoryMapper;
    private final ItemSimilarityMapper itemSimilarityMapper;
    private final SimilarityCalculator similarityCalculator;

    @Value("${recommend.algorithm.item-cf.min-common-users:5}")
    private int minCommonUsers;

    @Value("${recommend.algorithm.item-cf.max-similar-items:100}")
    private int maxSimilarItems;

    @Value("${recommend.algorithm.item-cf.similarity-threshold:0.1}")
    private double similarityThreshold;

    /**
     * 为指定用户生成推荐
     *
     * @param readerId 读者ID
     * @param limit 推荐数量
     * @return 推荐结果列表
     */
    public List<RecommendationVO> recommend(Long readerId, int limit) {
        log.info("ItemBasedCF: Generating recommendations for reader {}", readerId);

        // 1. 获取用户已借阅的图书及其评分
        List<Map<String, Object>> userRatings = borrowHistoryMapper.findRatingVectorByReaderId(readerId);
        if (userRatings.isEmpty()) {
            log.info("Reader {} has no borrow history, returning empty recommendations", readerId);
            return Collections.emptyList();
        }

        Set<Long> borrowedBooks = userRatings.stream()
                .map(m -> ((Number) m.get("book_id")).longValue())
                .collect(Collectors.toSet());

        Map<Long, Double> userRatingMap = userRatings.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("book_id")).longValue(),
                        m -> ((Number) m.get("rating")).doubleValue(),
                        (a, b) -> a
                ));

        // 2. 为每本已借阅的图书找相似图书
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> similaritySum = new HashMap<>();
        Map<Long, String> bookSourceMap = new HashMap<>(); // 记录推荐来源

        for (Long borrowedBookId : borrowedBooks) {
            double userRating = userRatingMap.get(borrowedBookId);

            // 获取相似图书
            List<ItemSimilarity> similarItems = itemSimilarityMapper.findTopSimilarItems(
                    borrowedBookId, maxSimilarItems, similarityThreshold);

            for (ItemSimilarity sim : similarItems) {
                Long candidateId = sim.getBookIdA().equals(borrowedBookId) ?
                        sim.getBookIdB() : sim.getBookIdA();

                // 排除已借阅的图书
                if (borrowedBooks.contains(candidateId)) {
                    continue;
                }

                double similarity = sim.getSimilarityScore();

                // 加权评分：用户对源图书的评分 * 相似度
                candidateScores.merge(candidateId, similarity * userRating, Double::sum);
                similaritySum.merge(candidateId, similarity, Double::sum);

                // 记录推荐来源（记录相似度最高的那个）
                if (!bookSourceMap.containsKey(candidateId)) {
                    bookSourceMap.put(candidateId, "book_" + borrowedBookId);
                }
            }
        }

        // 3. 计算预测评分并排序
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
            rec.setAlgorithm("ITEM_CF");
            rec.setReason("与你借阅过的图书相似");
            recommendations.add(rec);
        }

        // 按分数降序排序，取前N个
        return recommendations.stream()
                .sorted(Comparator.comparingDouble(RecommendationVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取与指定图书相似的图书列表
     *
     * @param bookId 图书ID
     * @param limit 返回数量
     * @return 相似图书列表
     */
    public List<RecommendationVO> findSimilarBooks(Long bookId, int limit) {
        log.info("ItemBasedCF: Finding similar books for book {}", bookId);

        List<ItemSimilarity> similarItems = itemSimilarityMapper.findTopSimilarItems(
                bookId, limit, similarityThreshold);

        return similarItems.stream()
                .map(sim -> {
                    RecommendationVO rec = new RecommendationVO();
                    Long similarBookId = sim.getBookIdA().equals(bookId) ?
                            sim.getBookIdB() : sim.getBookIdA();
                    rec.setBookId(similarBookId);
                    rec.setScore(sim.getSimilarityScore());
                    rec.setAlgorithm("ITEM_CF");
                    rec.setReason("借阅过此书的读者还借阅了这些书");
                    return rec;
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算并存储物品相似度矩阵
     *
     * 用于离线批量计算，可通过定时任务调用
     */
    public void computeItemSimilarityMatrix() {
        log.info("Computing item similarity matrix...");
        long startTime = System.currentTimeMillis();

        List<Long> allBooks = borrowHistoryMapper.findAllBookIds();
        log.info("Total books to process: {}", allBooks.size());

        // 预加载所有图书的评分向量（user_id -> rating）
        Map<Long, Map<Long, Double>> itemVectors = new HashMap<>();
        for (Long bookId : allBooks) {
            Map<Long, Double> vector = borrowHistoryMapper.findRatingVectorByBookId(bookId)
                    .stream()
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("reader_id")).longValue(),
                            m -> ((Number) m.get("rating")).doubleValue(),
                            (a, b) -> a
                    ));
            itemVectors.put(bookId, vector);
        }

        // 计算用户平均评分（用于调整余弦相似度）
        Map<Long, Double> userMeans = calculateUserMeans(itemVectors);

        // 计算物品对之间的相似度
        List<ItemSimilarity> similarities = new ArrayList<>();
        for (int i = 0; i < allBooks.size(); i++) {
            Long bookA = allBooks.get(i);
            Map<Long, Double> vectorA = itemVectors.get(bookA);

            for (int j = i + 1; j < allBooks.size(); j++) {
                Long bookB = allBooks.get(j);
                Map<Long, Double> vectorB = itemVectors.get(bookB);

                int commonCount = similarityCalculator.countCommonElements(vectorA, vectorB);
                if (commonCount < minCommonUsers) {
                    continue;
                }

                // 使用调整余弦相似度
                double similarity = similarityCalculator.adjustedCosineSimilarity(
                        vectorA, vectorB, userMeans);
                if (similarity < similarityThreshold) {
                    continue;
                }

                ItemSimilarity sim = new ItemSimilarity();
                sim.setBookIdA(bookA);
                sim.setBookIdB(bookB);
                sim.setSimilarityScore(similarity);
                sim.setCommonUsersCount(commonCount);
                sim.setCalculatedAt(LocalDateTime.now());
                similarities.add(sim);
            }
        }

        // 批量保存
        for (ItemSimilarity sim : similarities) {
            itemSimilarityMapper.insert(sim);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Item similarity matrix computed. Total pairs: {}, Duration: {}ms",
                similarities.size(), duration);
    }

    /**
     * 计算所有用户的平均评分
     */
    private Map<Long, Double> calculateUserMeans(Map<Long, Map<Long, Double>> itemVectors) {
        Map<Long, List<Double>> userRatings = new HashMap<>();

        for (Map<Long, Double> itemVector : itemVectors.values()) {
            for (Map.Entry<Long, Double> entry : itemVector.entrySet()) {
                userRatings.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        Map<Long, Double> userMeans = new HashMap<>();
        for (Map.Entry<Long, List<Double>> entry : userRatings.entrySet()) {
            double mean = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(3.0);
            userMeans.put(entry.getKey(), mean);
        }

        return userMeans;
    }
}
