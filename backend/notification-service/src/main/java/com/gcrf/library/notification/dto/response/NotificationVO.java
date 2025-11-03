package com.gcrf.library.notification.dto.response;

import com.gcrf.library.notification.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 通知类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告
     */
    private String notificationType;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急
     */
    private String priority;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 扩展数据（JSON字符串）
     */
    private String extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static NotificationVO from(Notification entity) {
        if (entity == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setNotificationType(entity.getNotificationType());
        vo.setPriority(entity.getPriority());
        vo.setIsRead(entity.getIsRead());
        vo.setReadAt(entity.getReadAt());
        vo.setExtraData(entity.getExtraData());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
