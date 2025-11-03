package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 通知推送请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class NotificationPushRequest {

    /**
     * 通知标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 通知类型: SYSTEM-系统, BUSINESS-业务, REMINDER-提醒
     */
    @NotBlank(message = "通知类型不能为空")
    private String notificationType;

    /**
     * 优先级: LOW-低, MEDIUM-中, HIGH-高
     */
    private String priority;

    /**
     * 目标类型: USER-指定用户, ALL-所有用户, TOPIC-主题
     */
    @NotNull(message = "目标类型不能为空")
    private String targetType;

    /**
     * 目标用户ID列表 (targetType=USER时使用)
     */
    private List<Long> targetIds;

    /**
     * 主题名称 (targetType=TOPIC时使用)
     */
    private String topic;

    /**
     * 通知方式: WEB-网页, EMAIL-邮件, SMS-短信, APP-移动应用
     * 可多选,逗号分隔,如: "WEB,EMAIL"
     */
    private String channels;

    /**
     * 是否持久化到数据库
     */
    private Boolean persistent;

    /**
     * 附加数据(JSON格式)
     */
    private String extraData;
}
