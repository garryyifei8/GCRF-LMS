package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.LogQueryRequest;
import com.gcrf.library.notification.dto.request.SmsSendRequest;
import com.gcrf.library.notification.dto.response.SmsLogVO;
import com.gcrf.library.notification.entity.NotificationTemplate;
import com.gcrf.library.notification.entity.SmsLog;
import com.gcrf.library.notification.mapper.NotificationTemplateMapper;
import com.gcrf.library.notification.mapper.SmsLogMapper;
import com.gcrf.library.notification.messaging.NotificationMessageProducer;
import com.gcrf.library.notification.service.NotificationTemplateService;
import com.gcrf.library.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 短信发送服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final SmsLogMapper smsLogMapper;
    private final NotificationTemplateMapper templateMapper;
    private final NotificationTemplateService templateService;
    private final NotificationMessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsLogVO sendSms(SmsSendRequest request) {
        // 创建短信日志记录
        SmsLog smsLog = new SmsLog();
        smsLog.setPhoneNumber(request.getPhoneNumber());
        smsLog.setContent(request.getContent());
        smsLog.setSmsType(request.getSmsType() != null ? request.getSmsType() : "NOTIFICATION");
        smsLog.setStatus("PENDING");
        smsLog.setRetryCount(0);
        smsLogMapper.insert(smsLog);

        try {
            // TODO: 调用第三方短信API (暂时模拟)
            log.info("模拟发送短信 - 手机号: {}, 内容: {}, 类型: {}",
                    request.getPhoneNumber(), request.getContent(), request.getSmsType());

            // 模拟发送成功
            smsLog.setStatus("SENT");
            smsLog.setSentAt(LocalDateTime.now());
            smsLogMapper.updateById(smsLog);

            log.info("短信发送成功, phoneNumber: {}", request.getPhoneNumber());

        } catch (Exception e) {
            // 更新状态为失败
            smsLog.setStatus("FAILED");
            smsLog.setErrorMessage(e.getMessage());
            smsLog.setRetryCount(smsLog.getRetryCount() + 1);
            smsLogMapper.updateById(smsLog);

            log.error("短信发送失败, phoneNumber: {}, error: {}", request.getPhoneNumber(), e.getMessage());
            throw new BusinessException("短信发送失败: " + e.getMessage());
        }

        return SmsLogVO.from(smsLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSmsAsync(SmsSendRequest request) {
        // 创建短信日志记录
        SmsLog smsLog = new SmsLog();
        smsLog.setPhoneNumber(request.getPhoneNumber());
        smsLog.setContent(request.getContent());
        smsLog.setSmsType(request.getSmsType() != null ? request.getSmsType() : "NOTIFICATION");
        smsLog.setStatus("PENDING");
        smsLog.setRetryCount(0);
        smsLogMapper.insert(smsLog);

        // 发送到RabbitMQ队列
        messageProducer.sendSmsMessage(request, smsLog.getId());
        log.info("短信已加入异步队列, phoneNumber: {}, logId: {}", request.getPhoneNumber(), smsLog.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsLogVO sendVerificationCode(String phoneNumber, String code, Integer expiresMinutes) {
        if (expiresMinutes == null || expiresMinutes <= 0) {
            expiresMinutes = 5; // 默认5分钟
        }

        String content = String.format("您的验证码是%s,有效期%d分钟,请勿泄露给他人。",
                code, expiresMinutes);

        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber(phoneNumber);
        request.setContent(content);
        request.setSmsType("VERIFICATION");

        return sendSms(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsLogVO sendSmsWithTemplate(String phoneNumber, Long templateId, Map<String, Object> variables) {
        // 查询模板
        NotificationTemplate template = templateMapper.selectOne(
            new LambdaQueryWrapper<NotificationTemplate>()
                .eq(NotificationTemplate::getId, templateId)
                .eq(NotificationTemplate::getTemplateType, "SMS")
                .isNull(NotificationTemplate::getDeletedAt)
        );

        if (template == null) {
            throw new BusinessException("短信模板不存在, templateId: " + templateId);
        }

        // 渲染模板
        String content = templateService.renderTemplate(templateId, variables);

        // 发送短信
        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber(phoneNumber);
        request.setContent(content);
        request.setSmsType("NOTIFICATION");

        return sendSms(request);
    }

    @Override
    public PageResult<SmsLogVO> querySmsLogs(LogQueryRequest request) {
        Page<SmsLog> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SmsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getStatus()),
                   SmsLog::getStatus, request.getStatus())
               .ge(request.getStartDate() != null, SmsLog::getCreatedAt, request.getStartDate())
               .le(request.getEndDate() != null, SmsLog::getCreatedAt, request.getEndDate())
               .orderByDesc(SmsLog::getCreatedAt);

        Page<SmsLog> result = smsLogMapper.selectPage(page, wrapper);

        List<SmsLogVO> voList = result.getRecords().stream()
                .map(SmsLogVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                voList
        );
    }

    @Override
    public SmsLogVO getSmsLogById(Long logId) {
        SmsLog smsLog = smsLogMapper.selectById(logId);
        if (smsLog == null) {
            throw new BusinessException("短信日志不存在, id: " + logId);
        }
        return SmsLogVO.from(smsLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsLogVO retryFailedSms(Long logId) {
        SmsLog smsLog = smsLogMapper.selectById(logId);
        if (smsLog == null) {
            throw new BusinessException("短信日志不存在, id: " + logId);
        }

        if (!"FAILED".equals(smsLog.getStatus())) {
            throw new BusinessException("只能重试失败的短信");
        }

        // 重新发送
        SmsSendRequest request = new SmsSendRequest();
        request.setPhoneNumber(smsLog.getPhoneNumber());
        request.setContent(smsLog.getContent());
        request.setSmsType(smsLog.getSmsType());

        // 删除旧记录,创建新记录
        smsLogMapper.deleteById(logId);
        return sendSms(request);
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        // 查询最近5分钟内发送的验证码
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        LambdaQueryWrapper<SmsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsLog::getPhoneNumber, phoneNumber)
               .eq(SmsLog::getSmsType, "VERIFICATION")
               .eq(SmsLog::getStatus, "SENT")
               .ge(SmsLog::getSentAt, fiveMinutesAgo)
               .orderByDesc(SmsLog::getSentAt)
               .last("LIMIT 1");

        SmsLog smsLog = smsLogMapper.selectOne(wrapper);
        if (smsLog == null) {
            log.warn("未找到有效验证码, phoneNumber: {}", phoneNumber);
            return false;
        }

        // 从短信内容中提取验证码进行比对
        // 格式: "您的验证码是123456,有效期5分钟,请勿泄露给他人。"
        String content = smsLog.getContent();
        if (content != null && content.contains(code)) {
            log.info("验证码验证成功, phoneNumber: {}", phoneNumber);
            return true;
        }

        log.warn("验证码不匹配, phoneNumber: {}", phoneNumber);
        return false;
    }
}
