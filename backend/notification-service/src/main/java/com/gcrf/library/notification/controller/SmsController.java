package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import com.gcrf.library.notification.dto.response.SmsLogVO;
import com.gcrf.library.notification.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短信发送控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送短信(同步)
     *
     * POST /api/v1/sms/send
     */
    @PostMapping("/send")
    public Result<SmsLogVO> sendSms(@Valid @RequestBody SmsSendRequest request) {
        log.info("发送短信请求: phoneNumber={}, smsType={}", request.getPhoneNumber(), request.getSmsType());
        SmsLogVO smsLog = smsService.sendSms(request);
        return Result.success(smsLog);
    }

    /**
     * 发送短信(异步)
     *
     * POST /api/v1/sms/send-async
     */
    @PostMapping("/send-async")
    public Result<Void> sendSmsAsync(@Valid @RequestBody SmsSendRequest request) {
        log.info("异步发送短信请求: phoneNumber={}, smsType={}", request.getPhoneNumber(), request.getSmsType());
        smsService.sendSmsAsync(request);
        return Result.success();
    }

    /**
     * 发送验证码短信
     *
     * POST /api/v1/sms/send-verification-code
     */
    @PostMapping("/send-verification-code")
    public Result<SmsLogVO> sendVerificationCode(
            @RequestParam String phoneNumber,
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "5") Integer expiresMinutes
    ) {
        log.info("发送验证码短信: phoneNumber={}, code={}, expiresMinutes={}",
                phoneNumber, code, expiresMinutes);
        SmsLogVO smsLog = smsService.sendVerificationCode(phoneNumber, code, expiresMinutes);
        return Result.success(smsLog);
    }

    /**
     * 使用模板发送短信
     *
     * POST /api/v1/sms/send-with-template
     */
    @PostMapping("/send-with-template")
    public Result<SmsLogVO> sendSmsWithTemplate(
            @RequestParam String phoneNumber,
            @RequestParam Long templateId,
            @RequestBody Map<String, Object> variables
    ) {
        log.info("使用模板发送短信: phoneNumber={}, templateId={}", phoneNumber, templateId);
        SmsLogVO smsLog = smsService.sendSmsWithTemplate(phoneNumber, templateId, variables);
        return Result.success(smsLog);
    }

    /**
     * 查询短信发送日志(分页)
     *
     * GET /api/v1/sms/logs?pageNum=1&pageSize=20&status=SENT
     */
    @GetMapping("/logs")
    public Result<PageResult<SmsLogVO>> querySmsLogs(@Valid LogQueryRequest request) {
        log.info("查询短信日志: pageNum={}, pageSize={}, status={}",
                request.getPageNum(), request.getPageSize(), request.getStatus());
        PageResult<SmsLogVO> result = smsService.querySmsLogs(request);
        return Result.success(result);
    }

    /**
     * 获取短信日志详情
     *
     * GET /api/v1/sms/logs/{logId}
     */
    @GetMapping("/logs/{logId}")
    public Result<SmsLogVO> getSmsLogById(@PathVariable Long logId) {
        log.info("获取短信日志详情: logId={}", logId);
        SmsLogVO smsLog = smsService.getSmsLogById(logId);
        return Result.success(smsLog);
    }

    /**
     * 重试失败的短信
     *
     * POST /api/v1/sms/logs/{logId}/retry
     */
    @PostMapping("/logs/{logId}/retry")
    public Result<SmsLogVO> retryFailedSms(@PathVariable Long logId) {
        log.info("重试失败的短信: logId={}", logId);
        SmsLogVO smsLog = smsService.retryFailedSms(logId);
        return Result.success(smsLog);
    }

    /**
     * 验证验证码
     *
     * POST /api/v1/sms/verify-code
     */
    @PostMapping("/verify-code")
    public Result<Boolean> verifyCode(
            @RequestParam String phoneNumber,
            @RequestParam String code
    ) {
        log.info("验证验证码: phoneNumber={}", phoneNumber);
        boolean valid = smsService.verifyCode(phoneNumber, code);
        return Result.success(valid);
    }
}
