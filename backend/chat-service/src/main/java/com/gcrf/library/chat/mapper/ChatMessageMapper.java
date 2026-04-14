package com.gcrf.library.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
