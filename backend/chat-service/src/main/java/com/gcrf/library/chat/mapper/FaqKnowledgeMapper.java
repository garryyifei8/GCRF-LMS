package com.gcrf.library.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.chat.entity.FaqKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FAQ知识库Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Mapper
public interface FaqKnowledgeMapper extends BaseMapper<FaqKnowledge> {

    /**
     * 根据关键词搜索FAQ
     *
     * @param keyword 关键词
     * @return FAQ列表
     */
    @Select("SELECT * FROM faq_knowledge WHERE deleted_at IS NULL AND status = 1 " +
            "AND (question ILIKE '%' || #{keyword} || '%' " +
            "OR #{keyword} = ANY(keywords)) " +
            "ORDER BY priority DESC, view_count DESC LIMIT 10")
    List<FaqKnowledge> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据意图标签查询FAQ
     *
     * @param intentCode 意图编码
     * @return FAQ列表
     */
    @Select("SELECT * FROM faq_knowledge WHERE deleted_at IS NULL AND status = 1 " +
            "AND #{intentCode} = ANY(intent_tags) " +
            "ORDER BY priority DESC, view_count DESC LIMIT 5")
    List<FaqKnowledge> findByIntentTag(@Param("intentCode") String intentCode);

    /**
     * 增加查看次数
     *
     * @param id FAQ ID
     */
    @Select("UPDATE faq_knowledge SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 增加有帮助次数
     *
     * @param id FAQ ID
     */
    @Select("UPDATE faq_knowledge SET helpful_count = helpful_count + 1 WHERE id = #{id}")
    void incrementHelpfulCount(@Param("id") Long id);

    /**
     * 增加无帮助次数
     *
     * @param id FAQ ID
     */
    @Select("UPDATE faq_knowledge SET unhelpful_count = unhelpful_count + 1 WHERE id = #{id}")
    void incrementUnhelpfulCount(@Param("id") Long id);
}
