package com.gcrf.library.notification.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import com.gcrf.library.notification.dto.response.SmsLogVO;

import java.util.Map;

/**
 * 短信发送服务接口
 */
public interface SmsService {

    /**
     * 发送短信(同步)
     */
    SmsLogVO sendSms(SmsSendRequest request);

    /**
     * 发送短信(异步,通过RabbitMQ)
     */
    void sendSmsAsync(SmsSendRequest request);

    /**
     * 发送验证码短信
     */
    SmsLogVO sendVerificationCode(String phoneNumber, String code, Integer expiresMinutes);

    /**
     * 使用模板发送短信
     */
    SmsLogVO sendSmsWithTemplate(String phoneNumber, Long templateId, Map<String, Object> variables);

    /**
     * 分页查询短信日志
     */
    PageResult<SmsLogVO> querySmsLogs(LogQueryRequest request);

    /**
     * 根据ID获取短信日志详情
     */
    SmsLogVO getSmsLogById(Long logId);

    /**
     * 重试失败的短信
     */
    SmsLogVO retryFailedSms(Long logId);

    /**
     * 验证验证码(检查5分钟内发送的验证码)
     */
    boolean verifyCode(String phoneNumber, String code);
}
