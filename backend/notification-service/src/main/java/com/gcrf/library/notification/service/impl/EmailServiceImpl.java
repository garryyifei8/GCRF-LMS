package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.EmailSendRequest;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.response.EmailLogVO;
import com.gcrf.library.notification.entity.EmailLog;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.mapper.EmailLogMapper;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.messaging.NotificationMessageProducer;
import com.gcrf.library.notification.service.EmailService;
import com.gcrf.library.notification.service.NotificationTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 邮件发送服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailLogMapper emailLogMapper;
    private final NotificationTemplateMapper templateMapper;
    private final NotificationTemplateService templateService;
    private final JavaMailSender mailSender;
    private final NotificationMessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailLogVO sendEmail(EmailSendRequest request) {
        // 创建邮件日志记录
        EmailLog emailLog = new EmailLog();
        emailLog.setRecipient(request.getRecipient());
        emailLog.setSubject(request.getSubject());
        emailLog.setContent(request.getContent());
        emailLog.setTemplateId(request.getTemplateId());
        emailLog.setStatus("PENDING");
        emailLog.setRetryCount(0);
        emailLogMapper.insert(emailLog);

        try {
            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getRecipient());
            helper.setSubject(request.getSubject());
            helper.setText(request.getContent(), true); // true表示HTML格式

            mailSender.send(message);

            // 更新状态为已发送
            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
            emailLogMapper.updateById(emailLog);

            log.info("邮件发送成功, recipient: {}, subject: {}", request.getRecipient(), request.getSubject());

        } catch (Exception e) {
            // 更新状态为失败 (catches MessagingException from helper setup and MailException from send)
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogMapper.updateById(emailLog);

            log.error("邮件发送失败, recipient: {}, error: {}", request.getRecipient(), e.getMessage());
            throw new BusinessException("邮件发送失败: " + e.getMessage());
        }

        return EmailLogVO.from(emailLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailAsync(EmailSendRequest request) {
        // 创建邮件日志记录
        EmailLog emailLog = new EmailLog();
        emailLog.setRecipient(request.getRecipient());
        emailLog.setSubject(request.getSubject());
        emailLog.setContent(request.getContent());
        emailLog.setTemplateId(request.getTemplateId());
        emailLog.setStatus("PENDING");
        emailLog.setRetryCount(0);
        emailLogMapper.insert(emailLog);

        // 发送到RabbitMQ队列
        messageProducer.sendEmailMessage(request, emailLog.getId());
        log.info("邮件已加入异步队列, recipient: {}, logId: {}", request.getRecipient(), emailLog.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailLogVO sendEmailWithTemplate(String recipient, Long templateId, Map<String, Object> variables) {
        // 查询模板
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .eq(NotificationTemplate::getTemplateType, "EMAIL")
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("邮件模板不存在, templateId: " + templateId);
        }

        // 渲染模板
        String content = templateService.renderTemplate(templateId, variables);

        // 发送邮件
        EmailSendRequest request = new EmailSendRequest();
        request.setRecipient(recipient);
        request.setSubject(template.getSubject());
        request.setContent(content);
        request.setTemplateId(templateId);

        return sendEmail(request);
    }

    @Override
    public PageResult<EmailLogVO> queryEmailLogs(LogQueryRequest request) {
        Page<EmailLog> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<EmailLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getStatus()),
                   EmailLog::getStatus, request.getStatus())
               .ge(request.getStartDate() != null, EmailLog::getCreatedAt, request.getStartDate())
               .le(request.getEndDate() != null, EmailLog::getCreatedAt, request.getEndDate())
               .orderByDesc(EmailLog::getCreatedAt);

        Page<EmailLog> result = emailLogMapper.selectPage(page, wrapper);

        List<EmailLogVO> voList = result.getRecords().stream()
                .map(EmailLogVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                voList
        );
    }

    @Override
    public EmailLogVO getEmailLogById(Long logId) {
        EmailLog emailLog = emailLogMapper.selectById(logId);
        if (emailLog == null) {
            throw new BusinessException("邮件日志不存在, id: " + logId);
        }
        return EmailLogVO.from(emailLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailLogVO retryFailedEmail(Long logId) {
        EmailLog emailLog = emailLogMapper.selectById(logId);
        if (emailLog == null) {
            throw new BusinessException("邮件日志不存在, id: " + logId);
        }

        if (!"FAILED".equals(emailLog.getStatus())) {
            throw new BusinessException("只能重试失败的邮件");
        }

        // 重新发送
        EmailSendRequest request = new EmailSendRequest();
        request.setRecipient(emailLog.getRecipient());
        request.setSubject(emailLog.getSubject());
        request.setContent(emailLog.getContent());
        request.setTemplateId(emailLog.getTemplateId());

        // 删除旧记录,创建新记录
        emailLogMapper.deleteById(logId);
        return sendEmail(request);
    }
}
