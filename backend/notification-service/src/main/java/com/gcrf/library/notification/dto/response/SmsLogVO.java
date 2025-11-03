package com.gcrf.library.notification.dto.response;

import com.gcrf.library.notification.entity.SmsLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信记录VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class SmsLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 短信内容
     */
    private String content;

    /**
     * 短信类型: VERIFICATION-验证码, NOTIFICATION-通知, MARKETING-营销
     */
    private String smsType;

    /**
     * 验证码（如果是验证码短信）
     */
    private String verificationCode;

    /**
     * 发送状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static SmsLogVO from(SmsLog entity) {
        if (entity == null) {
            return null;
        }
        SmsLogVO vo = new SmsLogVO();
        vo.setId(entity.getId());
        vo.setPhoneNumber(entity.getPhoneNumber());
        vo.setContent(entity.getContent());
        vo.setSmsType(entity.getSmsType());
        vo.setVerificationCode(entity.getVerificationCode());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setRetryCount(entity.getRetryCount());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setSentAt(entity.getSentAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
