package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建通知模板请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class TemplateCreateRequest {

    /**
     * 模板编码（唯一标识）
     */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过50个字符")
    private String templateCode;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100个字符")
    private String templateName;

    /**
     * 模板类型: EMAIL-邮件, SMS-短信, NOTIFICATION-站内信
     */
    @NotBlank(message = "模板类型不能为空")
    @Pattern(regexp = "^(EMAIL|SMS|NOTIFICATION)$", message = "模板类型必须为EMAIL、SMS或NOTIFICATION")
    private String templateType;

    /**
     * 模板主题（邮件/站内信必填）
     */
    @Size(max = 200, message = "模板主题长度不能超过200个字符")
    private String subject;

    /**
     * 内容模板（支持变量占位符）
     */
    @NotBlank(message = "内容模板不能为空")
    @Size(max = 5000, message = "内容模板长度不能超过5000个字符")
    private String content;

    /**
     * 变量列表
     */
    private List<String> variables;

    /**
     * 模板状态: ACTIVE-启用, INACTIVE-停用
     */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "模板状态必须为ACTIVE或INACTIVE")
    private String status = "ACTIVE";
}
