package com.gcrf.library.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.chat.entity.HotQuestionStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 热门问题统计Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Mapper
public interface HotQuestionStatsMapper extends BaseMapper<HotQuestionStats> {

    /**
     * 获取热门问题列表
     *
     * @param limit 限制数量
     * @return 热门问题列表
     */
    @Select("SELECT * FROM hot_question_stats ORDER BY ask_count DESC LIMIT #{limit}")
    List<HotQuestionStats> getTopQuestions(@Param("limit") int limit);
}
