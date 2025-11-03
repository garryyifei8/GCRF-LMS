package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送站内信请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationSendRequest {

    /**
     * 接收用户ID
     */
    @NotNull(message = "接收用户ID不能为空")
    private Long userId;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /**
     * 内容
     */
    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容长度不能超过2000个字符")
    private String content;

    /**
     * 通知类型: SYSTEM-系统通知, USER-用户消息, ACTIVITY-活动通知, ANNOUNCEMENT-公告
     */
    @NotBlank(message = "通知类型不能为空")
    @Pattern(regexp = "^(SYSTEM|USER|ACTIVITY|ANNOUNCEMENT)$", message = "通知类型必须为SYSTEM、USER、ACTIVITY或ANNOUNCEMENT")
    private String notificationType;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急（默认NORMAL）
     */
    @Pattern(regexp = "^(LOW|NORMAL|HIGH|URGENT)$", message = "优先级必须为LOW、NORMAL、HIGH或URGENT")
    private String priority = "NORMAL";

    /**
     * 扩展数据（JSON字符串，可选）
     */
    @Size(max = 1000, message = "扩展数据长度不能超过1000个字符")
    private String extraData;
}
