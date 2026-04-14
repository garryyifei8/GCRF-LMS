package com.gcrf.library.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.recommend.entity.UserSimilarity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户相似度Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Mapper
public interface UserSimilarityMapper extends BaseMapper<UserSimilarity> {

    /**
     * 获取与指定用户最相似的N个用户
     */
    @Select("""
        SELECT * FROM user_similarity
        WHERE (user_id_a = #{userId} OR user_id_b = #{userId})
          AND similarity_score >= #{minSimilarity}
        ORDER BY similarity_score DESC
        LIMIT #{limit}
        """)
    List<UserSimilarity> findTopSimilarUsers(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("minSimilarity") double minSimilarity
    );

    /**
     * 获取两个用户的相似度
     */
    @Select("""
        SELECT * FROM user_similarity
        WHERE (user_id_a = #{userIdA} AND user_id_b = #{userIdB})
           OR (user_id_a = #{userIdB} AND user_id_b = #{userIdA})
        LIMIT 1
        """)
    UserSimilarity findSimilarity(
            @Param("userIdA") Long userIdA,
            @Param("userIdB") Long userIdB
    );

    /**
     * 删除指定用户的所有相似度记录（用于重新计算）
     */
    @Select("DELETE FROM user_similarity WHERE user_id_a = #{userId} OR user_id_b = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
