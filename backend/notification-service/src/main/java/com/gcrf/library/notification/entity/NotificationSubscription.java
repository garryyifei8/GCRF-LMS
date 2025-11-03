package com.gcrf.library.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知订阅配置实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_subscriptions")
public class NotificationSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（唯一）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 邮件通知开关
     */
    @TableField("email_enabled")
    private Boolean emailEnabled;

    /**
     * 短信通知开关
     */
    @TableField("sms_enabled")
    private Boolean smsEnabled;

    /**
     * 站内信通知开关
     */
    @TableField("notification_enabled")
    private Boolean notificationEnabled;

    /**
     * 订阅的通知类型（JSON字符串数组）
     */
    @TableField("subscribed_types")
    private String subscribedTypes;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
