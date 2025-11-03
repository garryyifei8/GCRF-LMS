package com.gcrf.library.notification.dto.request;

import lombok.Data;

/**
 * 标记消息已读请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationMarkReadRequest {

    /**
     * 通知ID（单个标记时使用）
     */
    private Long notificationId;

    /**
     * 用户ID（批量标记时使用）
     */
    private Long userId;

    /**
     * 是否标记全部已读
     */
    private Boolean markAll = false;
}
