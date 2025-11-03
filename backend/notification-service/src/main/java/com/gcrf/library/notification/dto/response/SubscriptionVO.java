package com.gcrf.library.notification.dto.response;

import com.gcrf.library.notification.entity.NotificationSubscription;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅配置VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class SubscriptionVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 邮件通知开关
     */
    private Boolean emailEnabled;

    /**
     * 短信通知开关
     */
    private Boolean smsEnabled;

    /**
     * 站内信通知开关
     */
    private Boolean notificationEnabled;

    /**
     * 订阅的通知类型（JSON字符串数组）
     */
    private String subscribedTypes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static SubscriptionVO from(NotificationSubscription entity) {
        if (entity == null) {
            return null;
        }
        SubscriptionVO vo = new SubscriptionVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setEmailEnabled(entity.getEmailEnabled());
        vo.setSmsEnabled(entity.getSmsEnabled());
        vo.setNotificationEnabled(entity.getNotificationEnabled());
        vo.setSubscribedTypes(entity.getSubscribedTypes());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
