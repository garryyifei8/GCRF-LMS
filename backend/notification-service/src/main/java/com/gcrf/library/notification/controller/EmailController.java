package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.response.EmailLogVO;
import com.gcrf.library.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 邮件发送控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * 发送邮件(同步)
     *
     * POST /api/v1/emails/send
     */
    @PostMapping("/send")
    public Result<EmailLogVO> sendEmail(@Valid @RequestBody EmailSendRequest request) {
        log.info("发送邮件请求: recipient={}, subject={}", request.getRecipient(), request.getSubject());
        EmailLogVO emailLog = emailService.sendEmail(request);
        return Result.success(emailLog);
    }

    /**
     * 发送邮件(异步)
     *
     * POST /api/v1/emails/send-async
     */
    @PostMapping("/send-async")
    public Result<Void> sendEmailAsync(@Valid @RequestBody EmailSendRequest request) {
        log.info("异步发送邮件请求: recipient={}, subject={}", request.getRecipient(), request.getSubject());
        emailService.sendEmailAsync(request);
        return Result.success();
    }

    /**
     * 使用模板发送邮件
     *
     * POST /api/v1/emails/send-with-template
     */
    @PostMapping("/send-with-template")
    public Result<EmailLogVO> sendEmailWithTemplate(
            @RequestParam String recipient,
            @RequestParam Long templateId,
            @RequestBody Map<String, Object> variables
    ) {
        log.info("使用模板发送邮件: recipient={}, templateId={}", recipient, templateId);
        EmailLogVO emailLog = emailService.sendEmailWithTemplate(recipient, templateId, variables);
        return Result.success(emailLog);
    }

    /**
     * 查询邮件发送日志(分页)
     *
     * GET /api/v1/emails/logs?pageNum=1&pageSize=20&status=SENT
     */
    @GetMapping("/logs")
    public Result<PageResult<EmailLogVO>> queryEmailLogs(@Valid LogQueryRequest request) {
        log.info("查询邮件日志: pageNum={}, pageSize={}, status={}",
                request.getPageNum(), request.getPageSize(), request.getStatus());
        PageResult<EmailLogVO> result = emailService.queryEmailLogs(request);
        return Result.success(result);
    }

    /**
     * 获取邮件日志详情
     *
     * GET /api/v1/emails/logs/{logId}
     */
    @GetMapping("/logs/{logId}")
    public Result<EmailLogVO> getEmailLogById(@PathVariable Long logId) {
        log.info("获取邮件日志详情: logId={}", logId);
        EmailLogVO emailLog = emailService.getEmailLogById(logId);
        return Result.success(emailLog);
    }

    /**
     * 重试失败的邮件
     *
     * POST /api/v1/emails/logs/{logId}/retry
     */
    @PostMapping("/logs/{logId}/retry")
    public Result<EmailLogVO> retryFailedEmail(@PathVariable Long logId) {
        log.info("重试失败的邮件: logId={}", logId);
        EmailLogVO emailLog = emailService.retryFailedEmail(logId);
        return Result.success(emailLog);
    }
}
