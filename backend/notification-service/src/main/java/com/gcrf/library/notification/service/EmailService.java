package com.gcrf.library.notification.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.response.EmailLogVO;

import java.util.Map;

/**
 * 邮件发送服务接口
 */
public interface EmailService {

    /**
     * 发送邮件(同步,直接发送)
     */
    EmailLogVO sendEmail(EmailSendRequest request);

    /**
     * 发送邮件(异步,通过RabbitMQ)
     */
    void sendEmailAsync(EmailSendRequest request);

    /**
     * 使用模板发送邮件
     */
    EmailLogVO sendEmailWithTemplate(String recipient, Long templateId, Map<String, Object> variables);

    /**
     * 分页查询邮件日志
     */
    PageResult<EmailLogVO> queryEmailLogs(LogQueryRequest request);

    /**
     * 根据ID获取邮件日志详情
     */
    EmailLogVO getEmailLogById(Long logId);

    /**
     * 重试失败的邮件
     */
    EmailLogVO retryFailedEmail(Long logId);
}
