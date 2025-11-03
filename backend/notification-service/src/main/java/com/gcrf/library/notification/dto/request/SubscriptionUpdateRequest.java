package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新订阅配置请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class SubscriptionUpdateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
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
     * 订阅的通知类型列表
     */
    private List<String> subscribedTypes;
}
