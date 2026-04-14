package com.gcrf.library.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话记录Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
