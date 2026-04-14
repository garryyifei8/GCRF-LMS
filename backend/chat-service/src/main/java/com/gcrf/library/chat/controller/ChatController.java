package com.gcrf.library.chat.controller;

import com.gcrf.library.chat.dto.request.ChatFeedbackRequest;
import com.gcrf.library.chat.dto.request.ChatMessageRequest;
import com.gcrf.library.chat.dto.response.ChatHistoryVO;
import com.gcrf.library.chat.dto.response.ChatMessageVO;
import com.gcrf.library.chat.dto.response.ChatStatsVO;
import com.gcrf.library.chat.dto.response.HotQuestionVO;
import com.gcrf.library.chat.service.ChatService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能问答控制器
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "智能问答", description = "AI智能问答相关接口")
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息获取回复
     */
    @PostMapping("/message")
    @Operation(summary = "发送消息", description = "发送消息给AI助手并获取回复")
    public Result<ChatMessageVO> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        log.info("Received chat message: sessionId={}, content={}",
                request.getSessionId(), request.getContent());

        ChatMessageVO response = chatService.sendMessage(request);

        return Result.success(response);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取对话历史", description = "获取指定会话的历史消息")
    public Result<ChatHistoryVO> getHistory(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {

        log.info("Getting chat history for session: {}", sessionId);

        ChatHistoryVO history = chatService.getHistory(sessionId);

        return Result.success(history);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    @Operation(summary = "提交反馈", description = "对AI回答提交反馈（有帮助/无帮助）")
    public Result<Void> submitFeedback(@Valid @RequestBody ChatFeedbackRequest request) {
        log.info("Received feedback: sessionId={}, type={}",
                request.getSessionId(), request.getFeedbackType());

        chatService.submitFeedback(request);

        return Result.success();
    }

    /**
     * 获取热门问题
     */
    @GetMapping("/hot-questions")
    @Operation(summary = "获取热门问题", description = "获取最近热门的问题列表")
    public Result<List<HotQuestionVO>> getHotQuestions(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Getting hot questions, limit={}", limit);

        List<HotQuestionVO> hotQuestions = chatService.getHotQuestions(limit);

        return Result.success(hotQuestions);
    }

    /**
     * 获取对话统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取对话统计", description = "获取聊天系统的统计数据")
    public Result<ChatStatsVO> getChatStats() {
        log.info("Getting chat stats");

        ChatStatsVO stats = chatService.getChatStats();

        return Result.success(stats);
    }

    /**
     * 刷新缓存
     */
    @PostMapping("/cache/refresh")
    @Operation(summary = "刷新缓存", description = "刷新FAQ和意图缓存")
    public Result<Void> refreshCache() {
        log.info("Refreshing chat cache");

        chatService.refreshCache();

        return Result.success();
    }
}
