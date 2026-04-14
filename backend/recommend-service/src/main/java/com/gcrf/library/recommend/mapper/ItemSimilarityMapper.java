package com.gcrf.library.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.recommend.entity.ItemSimilarity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物品相似度Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Mapper
public interface ItemSimilarityMapper extends BaseMapper<ItemSimilarity> {

    /**
     * 获取与指定图书最相似的N本图书
     */
    @Select("""
        SELECT * FROM item_similarity
        WHERE (book_id_a = #{bookId} OR book_id_b = #{bookId})
          AND similarity_score >= #{minSimilarity}
        ORDER BY similarity_score DESC
        LIMIT #{limit}
        """)
    List<ItemSimilarity> findTopSimilarItems(
            @Param("bookId") Long bookId,
            @Param("limit") int limit,
            @Param("minSimilarity") double minSimilarity
    );

    /**
     * 获取两本书的相似度
     */
    @Select("""
        SELECT * FROM item_similarity
        WHERE (book_id_a = #{bookIdA} AND book_id_b = #{bookIdB})
           OR (book_id_a = #{bookIdB} AND book_id_b = #{bookIdA})
        LIMIT 1
        """)
    ItemSimilarity findSimilarity(
            @Param("bookIdA") Long bookIdA,
            @Param("bookIdB") Long bookIdB
    );

    /**
     * 删除指定图书的所有相似度记录（用于重新计算）
     */
    @Select("DELETE FROM item_similarity WHERE book_id_a = #{bookId} OR book_id_b = #{bookId}")
    void deleteByBookId(@Param("bookId") Long bookId);

    /**
     * 批量获取与指定图书列表相似的图书
     */
    @Select("""
        <script>
        SELECT * FROM item_similarity
        WHERE book_id_a IN
        <foreach item='id' collection='bookIds' open='(' separator=',' close=')'>
            #{id}
        </foreach>
        OR book_id_b IN
        <foreach item='id' collection='bookIds' open='(' separator=',' close=')'>
            #{id}
        </foreach>
        ORDER BY similarity_score DESC
        </script>
        """)
    List<ItemSimilarity> findSimilarItemsForBooks(@Param("bookIds") List<Long> bookIds);
}
