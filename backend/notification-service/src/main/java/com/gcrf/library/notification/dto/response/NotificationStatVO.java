package com.gcrf.library.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知统计VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatVO {

    /**
     * 总发送数
     */
    private Long totalSent;

    /**
     * 已读总数
     */
    private Long totalRead;

    /**
     * 未读总数
     */
    private Long totalUnread;

    /**
     * 系统通知数
     */
    private Long systemCount;

    /**
     * 用户消息数
     */
    private Long userCount;

    /**
     * 活动通知数
     */
    private Long activityCount;

    /**
     * 公告数
     */
    private Long announcementCount;
}
