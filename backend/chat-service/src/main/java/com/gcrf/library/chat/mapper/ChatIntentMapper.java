package com.gcrf.library.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.chat.entity.ChatIntent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 意图定义Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Mapper
public interface ChatIntentMapper extends BaseMapper<ChatIntent> {
}
