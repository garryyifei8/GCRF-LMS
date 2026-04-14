package com.gcrf.library.chat.service;

import com.gcrf.library.chat.dto.request.ChatFeedbackRequest;
import com.gcrf.library.chat.dto.request.ChatMessageRequest;
import com.gcrf.library.chat.dto.response.ChatHistoryVO;
import com.gcrf.library.chat.dto.response.ChatMessageVO;
import com.gcrf.library.chat.dto.response.ChatStatsVO;
import com.gcrf.library.chat.dto.response.HotQuestionVO;

import java.util.List;

/**
 * 聊天服务接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
public interface ChatService {

    /**
     * 发送消息并获取回复
     *
     * @param request 消息请求
     * @return AI响应消息
     */
    ChatMessageVO sendMessage(ChatMessageRequest request);

    /**
     * 获取对话历史
     *
     * @param sessionId 会话ID
     * @return 对话历史
     */
    ChatHistoryVO getHistory(String sessionId);

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     */
    void submitFeedback(ChatFeedbackRequest request);

    /**
     * 获取热门问题
     *
     * @param limit 数量限制
     * @return 热门问题列表
     */
    List<HotQuestionVO> getHotQuestions(int limit);

    /**
     * 获取对话统计
     *
     * @return 统计数据
     */
    ChatStatsVO getChatStats();

    /**
     * 刷新缓存
     */
    void refreshCache();
}
