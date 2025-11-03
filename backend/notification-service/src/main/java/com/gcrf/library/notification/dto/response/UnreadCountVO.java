package com.gcrf.library.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息统计VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 未读消息总数
     */
    private Long unreadCount;

    /**
     * 紧急未读消息数
     */
    private Long urgentCount;
}
