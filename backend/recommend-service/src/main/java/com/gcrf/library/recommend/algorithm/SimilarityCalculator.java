package com.gcrf.library.recommend.algorithm;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 相似度计算器
 *
 * 提供多种相似度计算方法：
 * - 余弦相似度 (Cosine Similarity)
 * - 皮尔逊相关系数 (Pearson Correlation)
 * - 杰卡德相似度 (Jaccard Similarity)
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Component
public class SimilarityCalculator {

    /**
     * 计算两个评分向量的余弦相似度
     *
     * cos(A,B) = (A . B) / (||A|| * ||B||)
     *
     * @param vectorA 用户/物品A的评分向量 (item_id -> rating)
     * @param vectorB 用户/物品B的评分向量 (item_id -> rating)
     * @return 相似度分数 [0, 1]
     */
    public double cosineSimilarity(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        // 找出共同的key
        Set<Long> commonKeys = new java.util.HashSet<>(vectorA.keySet());
        commonKeys.retainAll(vectorB.keySet());

        if (commonKeys.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Long key : commonKeys) {
            double a = vectorA.get(key);
            double b = vectorB.get(key);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 计算两个评分向量的皮尔逊相关系数
     *
     * 考虑用户的评分偏差，更适合处理不同评分习惯的用户
     *
     * @param vectorA 用户/物品A的评分向量
     * @param vectorB 用户/物品B的评分向量
     * @return 相关系数 [-1, 1]，返回时转换为 [0, 1]
     */
    public double pearsonCorrelation(Map<Long, Double> vectorA, Map<Long, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        Set<Long> commonKeys = new java.util.HashSet<>(vectorA.keySet());
        commonKeys.retainAll(vectorB.keySet());

        if (commonKeys.size() < 2) {
            return 0.0;
        }

        // 计算共同项的平均值
        double sumA = 0.0, sumB = 0.0;
        for (Long key : commonKeys) {
            sumA += vectorA.get(key);
            sumB += vectorB.get(key);
        }
        double meanA = sumA / commonKeys.size();
        double meanB = sumB / commonKeys.size();

        // 计算皮尔逊相关系数
        double numerator = 0.0;
        double denomA = 0.0;
        double denomB = 0.0;

        for (Long key : commonKeys) {
            double diffA = vectorA.get(key) - meanA;
            double diffB = vectorB.get(key) - meanB;
            numerator += diffA * diffB;
            denomA += diffA * diffA;
            denomB += diffB * diffB;
        }

        if (denomA == 0.0 || denomB == 0.0) {
            return 0.0;
        }

        double correlation = numerator / (Math.sqrt(denomA) * Math.sqrt(denomB));
        // 将 [-1, 1] 转换为 [0, 1]
        return (correlation + 1) / 2;
    }

    /**
     * 计算两个集合的杰卡德相似度
     *
     * J(A,B) = |A ∩ B| / |A ∪ B|
     *
     * 适用于二值数据（借阅/未借阅）
     *
     * @param setA 集合A
     * @param setB 集合B
     * @return 相似度 [0, 1]
     */
    public double jaccardSimilarity(Set<Long> setA, Set<Long> setB) {
        if (setA == null || setB == null || setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        Set<Long> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);

        Set<Long> union = new java.util.HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 计算调整后的余弦相似度
     *
     * 用于Item-based CF，减去用户的平均评分以消除评分偏差
     *
     * @param itemVectorA 物品A的评分向量 (user_id -> rating)
     * @param itemVectorB 物品B的评分向量 (user_id -> rating)
     * @param userMeans 用户平均评分 (user_id -> mean_rating)
     * @return 相似度 [0, 1]
     */
    public double adjustedCosineSimilarity(
            Map<Long, Double> itemVectorA,
            Map<Long, Double> itemVectorB,
            Map<Long, Double> userMeans) {

        if (itemVectorA == null || itemVectorB == null ||
            itemVectorA.isEmpty() || itemVectorB.isEmpty() ||
            userMeans == null || userMeans.isEmpty()) {
            return 0.0;
        }

        Set<Long> commonUsers = new java.util.HashSet<>(itemVectorA.keySet());
        commonUsers.retainAll(itemVectorB.keySet());

        if (commonUsers.isEmpty()) {
            return 0.0;
        }

        double numerator = 0.0;
        double denomA = 0.0;
        double denomB = 0.0;

        for (Long userId : commonUsers) {
            double mean = userMeans.getOrDefault(userId, 3.0);
            double diffA = itemVectorA.get(userId) - mean;
            double diffB = itemVectorB.get(userId) - mean;

            numerator += diffA * diffB;
            denomA += diffA * diffA;
            denomB += diffB * diffB;
        }

        if (denomA == 0.0 || denomB == 0.0) {
            return 0.0;
        }

        double similarity = numerator / (Math.sqrt(denomA) * Math.sqrt(denomB));
        // 将 [-1, 1] 转换为 [0, 1]
        return Math.max(0, (similarity + 1) / 2);
    }

    /**
     * 获取两个向量的共同元素数量
     */
    public int countCommonElements(Map<Long, ?> vectorA, Map<Long, ?> vectorB) {
        if (vectorA == null || vectorB == null) {
            return 0;
        }
        Set<Long> common = new java.util.HashSet<>(vectorA.keySet());
        common.retainAll(vectorB.keySet());
        return common.size();
    }
}
