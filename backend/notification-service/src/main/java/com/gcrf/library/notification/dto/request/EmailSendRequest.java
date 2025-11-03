package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 发送邮件请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class EmailSendRequest {

    /**
     * 收件人邮箱
     */
    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String recipient;

    /**
     * 邮件主题
     */
    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 200, message = "邮件主题长度不能超过200个字符")
    private String subject;

    /**
     * 邮件内容
     */
    @NotBlank(message = "邮件内容不能为空")
    @Size(max = 5000, message = "邮件内容长度不能超过5000个字符")
    private String content;

    /**
     * 模板ID（可选，使用模板时填写）
     */
    private Long templateId;

    /**
     * 模板变量（使用模板时填写）
     */
    private Map<String, Object> templateVariables;
}
