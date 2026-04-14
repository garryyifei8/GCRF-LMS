package com.gcrf.library.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.recommend.entity.RecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推荐日志Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Mapper
public interface RecommendationLogMapper extends BaseMapper<RecommendationLog> {

    /**
     * 获取推荐效果统计（点击率、转化率等）
     */
    @Select("""
        SELECT
            COUNT(*) as total_recommendations,
            COUNT(CASE WHEN clicked = true THEN 1 END) as clicked_count,
            COUNT(CASE WHEN borrowed = true THEN 1 END) as borrowed_count,
            ROUND(COUNT(CASE WHEN clicked = true THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) as click_rate,
            ROUND(COUNT(CASE WHEN borrowed = true THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) as conversion_rate
        FROM recommendation_log
        WHERE recommended_at >= #{startTime}
        """)
    Map<String, Object> getRecommendationStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 按算法类型获取推荐效果统计
     */
    @Select("""
        SELECT
            algorithm,
            COUNT(*) as total,
            COUNT(CASE WHEN clicked = true THEN 1 END) as clicked,
            COUNT(CASE WHEN borrowed = true THEN 1 END) as borrowed,
            ROUND(COUNT(CASE WHEN borrowed = true THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) as precision
        FROM recommendation_log
        WHERE recommended_at >= #{startTime}
        GROUP BY algorithm
        """)
    java.util.List<Map<String, Object>> getStatsByAlgorithm(@Param("startTime") LocalDateTime startTime);
}
