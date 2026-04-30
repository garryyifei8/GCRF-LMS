package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.entity.SystemFeedback;
import com.gcrf.library.system.service.SystemFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户反馈控制器
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/feedback")
@RequiredArgsConstructor
@Tag(name = "用户反馈", description = "用户意见反馈提交与查询")
public class SystemFeedbackController {

    private final SystemFeedbackService feedbackService;

    /**
     * 提交反馈
     */
    @PostMapping
    @Operation(summary = "提交反馈", description = "用户提交意见或问题反馈")
    public Result<SystemFeedback> submit(@RequestBody SystemFeedback feedback) {
        log.info("用户提交反馈, userId: {}, title: {}", feedback.getUserId(), feedback.getTitle());
        return Result.success(feedbackService.submit(feedback));
    }

    /**
     * 分页查询反馈列表
     */
    @GetMapping
    @Operation(summary = "查询反馈列表", description = "支持按用户ID过滤，分页返回")
    public Result<PageResult<SystemFeedback>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("查询反馈列表, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        return Result.success(feedbackService.listByUser(userId, pageNum, pageSize));
    }
}
