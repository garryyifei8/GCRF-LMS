package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询站内信请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationQueryRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告
     */
    @Pattern(regexp = "^(SYSTEM|USER|ACTIVITY|ANNOUNCEMENT)$", message = "通知类型必须为SYSTEM、USER、ACTIVITY或ANNOUNCEMENT")
    private String notificationType;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急
     */
    @Pattern(regexp = "^(LOW|NORMAL|HIGH|URGENT)$", message = "优先级必须为LOW、NORMAL、HIGH或URGENT")
    private String priority;

    /**
     * 开始日期
     */
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    private LocalDateTime endDate;

    /**
     * 关键词（搜索标题/内容）
     */
    private String keyword;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
