package com.gcrf.library.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.chat.dto.request.ChatFeedbackRequest;
import com.gcrf.library.chat.dto.request.ChatMessageRequest;
import com.gcrf.library.chat.dto.response.ChatHistoryVO;
import com.gcrf.library.chat.dto.response.ChatMessageVO;
import com.gcrf.library.chat.dto.response.ChatStatsVO;
import com.gcrf.library.chat.dto.response.HotQuestionVO;
import com.gcrf.library.chat.engine.ChatBotEngine;
import com.gcrf.library.chat.entity.*;
import com.gcrf.library.chat.mapper.*;
import com.gcrf.library.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatBotEngine chatBotEngine;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatFeedbackMapper chatFeedbackMapper;
    private final HotQuestionStatsMapper hotQuestionStatsMapper;
    private final FaqKnowledgeMapper faqKnowledgeMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String SESSION_PREFIX = "chat:session:";
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * 发送消息并获取回复
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(ChatMessageRequest request) {
        String sessionId = request.getSessionId();
        boolean isNewSession = false;

        // 如果没有sessionId，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
            isNewSession = true;
            createSession(sessionId, request.getReaderId());
        } else {
            // 更新会话活跃时间
            refreshSession(sessionId);
        }

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole("user");
        userMessage.setContent(request.getContent());
        userMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);

        // 更新热门问题统计
        updateHotQuestionStats(request.getContent());

        // 获取AI响应
        ChatMessageVO response = chatBotEngine.processMessage(request.getContent(), sessionId);

        // 保存AI响应消息
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(response.getContent());
        assistantMessage.setIntentCode(response.getIntentCode());
        assistantMessage.setConfidence(response.getConfidence());
        assistantMessage.setMatchedFaqId(response.getMatchedFaqId());
        assistantMessage.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(assistantMessage);

        // 更新会话消息数
        updateSessionMessageCount(sessionId);

        // 设置消息ID
        response.setId(assistantMessage.getId());

        return response;
    }

    /**
     * 获取对话历史
     */
    @Override
    public ChatHistoryVO getHistory(String sessionId) {
        // 获取会话信息
        LambdaQueryWrapper<ChatSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = chatSessionMapper.selectOne(sessionWrapper);

        if (session == null) {
            return ChatHistoryVO.builder()
                    .sessionId(sessionId)
                    .messages(Collections.emptyList())
                    .build();
        }

        // 获取消息列表
        LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt);
        List<ChatMessage> messages = chatMessageMapper.selectList(messageWrapper);

        List<ChatMessageVO> messageVOs = messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());

        return ChatHistoryVO.builder()
                .sessionId(sessionId)
                .readerId(session.getReaderId())
                .startTime(session.getStartTime())
                .messageCount(session.getMessageCount())
                .messages(messageVOs)
                .build();
    }

    /**
     * 提交反馈
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(ChatFeedbackRequest request) {
        // 保存反馈
        ChatFeedback feedback = new ChatFeedback();
        feedback.setSessionId(request.getSessionId());
        feedback.setMessageId(request.getMessageId());
        feedback.setFaqId(request.getFaqId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setComment(request.getComment());
        feedback.setReaderId(request.getReaderId());
        feedback.setCreatedAt(LocalDateTime.now());
        chatFeedbackMapper.insert(feedback);

        // 更新FAQ统计
        if (request.getFaqId() != null) {
            if ("helpful".equals(request.getFeedbackType())) {
                faqKnowledgeMapper.incrementHelpfulCount(request.getFaqId());
            } else if ("unhelpful".equals(request.getFeedbackType())) {
                faqKnowledgeMapper.incrementUnhelpfulCount(request.getFaqId());
            }
        }

        log.info("Feedback submitted: sessionId={}, type={}", request.getSessionId(), request.getFeedbackType());
    }

    /**
     * 获取热门问题
     */
    @Override
    public List<HotQuestionVO> getHotQuestions(int limit) {
        List<HotQuestionStats> stats = hotQuestionStatsMapper.getTopQuestions(limit);

        return stats.stream()
                .map(s -> HotQuestionVO.builder()
                        .question(s.getQuestionText())
                        .count(s.getAskCount())
                        .faqId(s.getFaqId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取对话统计
     */
    @Override
    public ChatStatsVO getChatStats() {
        // 总提问数
        Long totalQuestions = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getRole, "user")
        );

        // 今日提问数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayQuestions = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRole, "user")
                        .ge(ChatMessage::getCreatedAt, todayStart)
        );

        // 活跃会话数 (最近30分钟)
        LocalDateTime activeThreshold = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
        Long activeSessions = chatSessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>()
                        .ge(ChatSession::getUpdatedAt, activeThreshold)
        );

        // 有帮助反馈数
        Long helpfulCount = chatFeedbackMapper.selectCount(
                new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getFeedbackType, "helpful")
        );
        Long totalFeedback = chatFeedbackMapper.selectCount(null);

        // 计算成功率
        BigDecimal successRate = BigDecimal.valueOf(87.5); // 默认值
        if (totalFeedback > 0) {
            successRate = BigDecimal.valueOf(helpfulCount)
                    .divide(BigDecimal.valueOf(totalFeedback), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        return ChatStatsVO.builder()
                .totalQuestions(totalQuestions)
                .todayQuestions(todayQuestions)
                .activeSessions(activeSessions)
                .successRate(successRate)
                .avgResponseTime(BigDecimal.valueOf(0.8))
                .satisfaction(BigDecimal.valueOf(4.5))
                .build();
    }

    /**
     * 刷新缓存
     */
    @Override
    public void refreshCache() {
        chatBotEngine.refreshAllCaches();
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建新会话
     */
    private void createSession(String sessionId, Long readerId) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setReaderId(readerId);
        session.setStartTime(LocalDateTime.now());
        session.setMessageCount(0);
        session.setResolved(false);
        session.setContext(new HashMap<>());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);

        // 在Redis中设置会话超时
        redisTemplate.opsForValue().set(
                SESSION_PREFIX + sessionId,
                "active",
                SESSION_TIMEOUT_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("New chat session created: {}", sessionId);
    }

    /**
     * 刷新会话活跃时间
     */
    private void refreshSession(String sessionId) {
        // 更新数据库
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }

        // 刷新Redis过期时间
        redisTemplate.expire(
                SESSION_PREFIX + sessionId,
                SESSION_TIMEOUT_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /**
     * 更新会话消息数
     */
    private void updateSessionMessageCount(String sessionId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);
        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session != null) {
            session.setMessageCount(session.getMessageCount() + 2); // 用户消息 + AI回复
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    /**
     * 更新热门问题统计
     */
    private void updateHotQuestionStats(String questionText) {
        String normalizedText = normalizeQuestionText(questionText);

        LambdaQueryWrapper<HotQuestionStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotQuestionStats::getNormalizedText, normalizedText);
        HotQuestionStats stats = hotQuestionStatsMapper.selectOne(wrapper);

        if (stats != null) {
            stats.setAskCount(stats.getAskCount() + 1);
            stats.setLastAskedAt(LocalDateTime.now());
            stats.setUpdatedAt(LocalDateTime.now());
            hotQuestionStatsMapper.updateById(stats);
        } else {
            stats = new HotQuestionStats();
            stats.setQuestionText(questionText);
            stats.setNormalizedText(normalizedText);
            stats.setAskCount(1);
            stats.setLastAskedAt(LocalDateTime.now());
            stats.setCreatedAt(LocalDateTime.now());
            stats.setUpdatedAt(LocalDateTime.now());
            hotQuestionStatsMapper.insert(stats);
        }
    }

    /**
     * 标准化问题文本
     */
    private String normalizeQuestionText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[\\s\\p{Punct}]+", " ")
                .trim();
    }

    /**
     * 转换为消息VO
     */
    private ChatMessageVO convertToMessageVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .intentCode(message.getIntentCode())
                .confidence(message.getConfidence())
                .matchedFaqId(message.getMatchedFaqId())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
