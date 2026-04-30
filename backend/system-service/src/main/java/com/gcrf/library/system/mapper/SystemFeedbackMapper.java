package com.gcrf.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.system.entity.SystemFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈Mapper接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Mapper
public interface SystemFeedbackMapper extends BaseMapper<SystemFeedback> {
}
