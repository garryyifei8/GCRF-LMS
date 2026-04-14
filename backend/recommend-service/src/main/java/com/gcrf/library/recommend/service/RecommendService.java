package com.gcrf.library.recommend.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.recommend.dto.request.BatchRecommendRequest;
import com.gcrf.library.recommend.dto.request.RecommendRequest;
import com.gcrf.library.recommend.dto.response.RecommendStatsVO;
import com.gcrf.library.recommend.dto.response.RecommendationVO;

import java.util.List;

/**
 * 推荐服务接口
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
public interface RecommendService {

    /**
     * 为指定读者生成个性化推荐
     *
     * @param readerId 读者ID
     * @param limit 推荐数量
     * @param algorithm 算法类型
     * @param scene 推荐场景
     * @return 推荐结果列表
     */
    List<RecommendationVO> recommendForReader(Long readerId, int limit, String algorithm, String scene);

    /**
     * 获取热门图书推荐
     *
     * @param limit 推荐数量
     * @return 热门图书列表
     */
    List<RecommendationVO> getPopularBooks(int limit);

    /**
     * 获取与指定图书相似的图书
     *
     * @param bookId 图书ID
     * @param limit 推荐数量
     * @return 相似图书列表
     */
    List<RecommendationVO> getSimilarBooks(Long bookId, int limit);

    /**
     * 批量生成推荐（管理后台使用）
     *
     * @param request 批量推荐请求
     * @return 分页推荐结果
     */
    PageResult<RecommendationVO> batchRecommend(BatchRecommendRequest request);

    /**
     * 获取推荐效果统计
     *
     * @param days 统计天数
     * @return 统计结果
     */
    RecommendStatsVO getRecommendStats(int days);

    /**
     * 记录推荐点击
     *
     * @param readerId 读者ID
     * @param bookId 图书ID
     */
    void recordClick(Long readerId, Long bookId);

    /**
     * 记录推荐借阅转化
     *
     * @param readerId 读者ID
     * @param bookId 图书ID
     */
    void recordBorrow(Long readerId, Long bookId);

    /**
     * 触发相似度矩阵重新计算
     */
    void recomputeSimilarityMatrix();
}
