package com.gcrf.library.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内信实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("notifications")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 内容
     */
    @TableField("content")
    private String content;

    /**
     * 通知类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告
     */
    @TableField("notification_type")
    private String notificationType;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急
     */
    @TableField("priority")
    private String priority;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 已读时间
     */
    @TableField("read_at")
    private LocalDateTime readAt;

    /**
     * 扩展数据（JSON字符串）
     */
    @TableField("extra_data")
    private String extraData;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 删除时间（软删除标记）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
