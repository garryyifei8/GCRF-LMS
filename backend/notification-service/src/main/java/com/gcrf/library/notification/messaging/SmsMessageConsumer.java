package com.gcrf.library.notification.messaging;

import com.gcrf.library.notification.config.RabbitMQConfig;
import com.gcrf.library.notification.entity.SmsLog;
import com.gcrf.library.notification.mapper.SmsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 短信消息消费者
 *
 * 从RabbitMQ队列消费短信发送请求并执行实际发送
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsMessageConsumer {

    private final SmsLogMapper smsLogMapper;

    /**
     * 消费短信消息并发送
     *
     * @param message 短信消息对象
     */
    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void consumeSmsMessage(NotificationMessageProducer.SmsMessage message) {
        log.info("收到短信消息, logId: {}, phoneNumber: {}", message.getLogId(), message.getPhoneNumber());

        // 查询短信日志
        SmsLog smsLog = smsLogMapper.selectById(message.getLogId());
        if (smsLog == null) {
            log.error("短信日志不存在, logId: {}", message.getLogId());
            return;
        }

        // 检查状态
        if (!"PENDING".equals(smsLog.getStatus())) {
            log.warn("短信状态不是PENDING, 跳过发送, logId: {}, status: {}",
                    message.getLogId(), smsLog.getStatus());
            return;
        }

        // 更新状态为SENDING
        smsLog.setStatus("SENDING");
        smsLogMapper.updateById(smsLog);

        try {
            // TODO: 调用第三方短信API (暂时模拟)
            log.info("模拟发送短信 - 手机号: {}, 内容: {}, 类型: {}",
                    message.getPhoneNumber(), message.getContent(), message.getSmsType());

            // 模拟发送延迟(可选)
            // Thread.sleep(100);

            // 模拟成功率(可选,用于测试重试机制)
            // if (Math.random() < 0.2) {
            //     throw new RuntimeException("模拟短信发送失败");
            // }

            // 更新状态为SENT
            smsLog.setStatus("SENT");
            smsLog.setSentAt(LocalDateTime.now());
            smsLogMapper.updateById(smsLog);

            log.info("短信发送成功, logId: {}, phoneNumber: {}", message.getLogId(), message.getPhoneNumber());

        } catch (Exception e) {
            // 发送失败,更新状态为FAILED
            smsLog.setStatus("FAILED");
            smsLog.setErrorMessage(e.getMessage());
            smsLog.setRetryCount(smsLog.getRetryCount() + 1);
            smsLogMapper.updateById(smsLog);

            log.error("短信发送失败, logId: {}, phoneNumber: {}, error: {}",
                    message.getLogId(), message.getPhoneNumber(), e.getMessage(), e);

            // 如果重试次数小于3次,重新抛出异常触发消息重新入队
            if (smsLog.getRetryCount() < 3) {
                throw new RuntimeException("短信发送失败,将重试", e);
            } else {
                log.error("短信发送失败次数超过3次,不再重试, logId: {}", message.getLogId());
            }
        }
    }

    /**
     * 模拟调用第三方短信API
     *
     * 实际生产环境需要对接真实的短信服务商API,例如:
     * - 阿里云短信服务
     * - 腾讯云短信服务
     * - 华为云短信服务
     * - Twilio (国际)
     *
     * @param phoneNumber 手机号
     * @param content 短信内容
     * @param smsType 短信类型
     * @return 发送结果
     */
    private boolean sendSmsViaSmsProvider(String phoneNumber, String content, String smsType) {
        // TODO: 实现第三方短信API调用
        /*
        示例代码 (阿里云短信):

        DefaultProfile profile = DefaultProfile.getProfile(
            "cn-hangzhou",
            System.getenv("ALIYUN_ACCESS_KEY_ID"),
            System.getenv("ALIYUN_ACCESS_KEY_SECRET")
        );

        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("PhoneNumbers", phoneNumber);
        request.putQueryParameter("SignName", "图书馆管理系统");
        request.putQueryParameter("TemplateCode", "SMS_TEMPLATE_CODE");
        request.putQueryParameter("TemplateParam", "{\"code\":\"123456\"}");

        CommonResponse response = client.getCommonResponse(request);
        return response.getHttpStatus() == 200;
        */

        log.info("模拟发送短信: phoneNumber={}, content={}, smsType={}",
                phoneNumber, content, smsType);
        return true;
    }
}
