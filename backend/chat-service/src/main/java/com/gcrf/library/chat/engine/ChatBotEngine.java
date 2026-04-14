package com.gcrf.library.chat.engine;

import com.gcrf.library.chat.dto.response.ChatMessageVO;
import com.gcrf.library.chat.entity.FaqKnowledge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天机器人引擎
 *
 * 整合意图识别和FAQ匹配，生成最终响应
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBotEngine {

    private final IntentRecognizer intentRecognizer;
    private final FaqMatcher faqMatcher;

    /**
     * 默认欢迎消息
     */
    private static final String WELCOME_MESSAGE =
            "您好！我是图书馆AI助手，很高兴为您服务。<br/>" +
            "您可以向我咨询以下问题：<br/>" +
            "<ul>" +
            "<li>图书借阅规则和流程</li>" +
            "<li>开馆时间查询</li>" +
            "<li>罚款和赔偿规则</li>" +
            "<li>读者证办理</li>" +
            "<li>图书预约和续借</li>" +
            "</ul>" +
            "请问有什么可以帮您的？";

    /**
     * 默认回退消息
     */
    private static final String FALLBACK_MESSAGE =
            "抱歉，我还不太理解您的问题。<br/><br/>" +
            "您可以尝试：<br/>" +
            "<ul>" +
            "<li>换一种方式描述问题</li>" +
            "<li>使用更具体的关键词</li>" +
            "<li>选择下方的快捷问题</li>" +
            "</ul>" +
            "如果问题仍未解决，建议您联系图书馆工作人员：<br/>" +
            "<strong>服务热线：0571-12345678</strong>";

    /**
     * 处理用户消息
     *
     * @param userInput 用户输入
     * @param sessionId 会话ID
     * @return 响应消息VO
     */
    public ChatMessageVO processMessage(String userInput, String sessionId) {
        log.info("Processing message: sessionId={}, input={}", sessionId, userInput);

        // 1. 意图识别
        IntentResult intentResult = intentRecognizer.recognize(userInput);
        log.debug("Intent recognized: {}", intentResult);

        // 2. 根据意图类型处理
        return switch (intentResult.getActionType()) {
            case "NONE" -> handleDirectResponse(intentResult, sessionId);
            case "FAQ_LOOKUP" -> handleFaqLookup(intentResult, userInput, sessionId);
            case "API_CALL" -> handleApiCall(intentResult, sessionId);
            case "TRANSFER" -> handleTransfer(intentResult, sessionId);
            default -> handleFallback(sessionId);
        };
    }

    /**
     * 处理直接响应（如问候、感谢）
     */
    private ChatMessageVO handleDirectResponse(IntentResult intentResult, String sessionId) {
        String response = intentResult.getResponseTemplate();
        if (response == null || response.isEmpty()) {
            response = FALLBACK_MESSAGE;
        }

        return ChatMessageVO.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(response)
                .intentCode(intentResult.getIntentCode())
                .confidence(intentResult.getConfidence())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 处理FAQ查询
     */
    private ChatMessageVO handleFaqLookup(IntentResult intentResult, String userInput, String sessionId) {
        // 匹配FAQ
        List<FaqKnowledge> matchedFaqs = faqMatcher.matchByIntent(intentResult, userInput);

        if (matchedFaqs.isEmpty()) {
            return handleFallback(sessionId);
        }

        FaqKnowledge bestMatch = matchedFaqs.get(0);

        // 增加查看次数
        faqMatcher.incrementViewCount(bestMatch.getId());

        // 获取相关问题
        List<FaqKnowledge> relatedFaqs = faqMatcher.getRelatedFaqs(bestMatch.getId(), 3);
        List<ChatMessageVO.RelatedQuestionVO> relatedQuestions = relatedFaqs.stream()
                .map(faq -> ChatMessageVO.RelatedQuestionVO.builder()
                        .faqId(faq.getId())
                        .question(faq.getQuestion())
                        .build())
                .collect(Collectors.toList());

        return ChatMessageVO.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(bestMatch.getAnswer())
                .intentCode(intentResult.getIntentCode())
                .confidence(intentResult.getConfidence())
                .matchedFaqId(bestMatch.getId())
                .relatedQuestions(relatedQuestions)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 处理API调用（预留扩展）
     */
    private ChatMessageVO handleApiCall(IntentResult intentResult, String sessionId) {
        // TODO: 实现外部API调用逻辑（如查询馆藏、查询借阅记录等）
        return handleFallback(sessionId);
    }

    /**
     * 处理转人工
     */
    private ChatMessageVO handleTransfer(IntentResult intentResult, String sessionId) {
        String response =
                "好的，正在为您转接人工客服...<br/><br/>" +
                "您也可以直接联系：<br/>" +
                "<ul>" +
                "<li>服务热线：0571-12345678</li>" +
                "<li>服务时间：周一至周五 8:00-17:00</li>" +
                "</ul>";

        return ChatMessageVO.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(response)
                .intentCode("TRANSFER")
                .confidence(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 处理回退
     */
    private ChatMessageVO handleFallback(String sessionId) {
        return ChatMessageVO.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(FALLBACK_MESSAGE)
                .intentCode("UNKNOWN")
                .confidence(BigDecimal.ZERO)
                .relatedQuestions(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 生成欢迎消息
     *
     * @param sessionId 会话ID
     * @return 欢迎消息VO
     */
    public ChatMessageVO generateWelcomeMessage(String sessionId) {
        return ChatMessageVO.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(WELCOME_MESSAGE)
                .intentCode("WELCOME")
                .confidence(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 刷新所有缓存
     */
    public void refreshAllCaches() {
        intentRecognizer.refreshCache();
        faqMatcher.refreshCache();
        log.info("All chat bot caches refreshed");
    }
}
