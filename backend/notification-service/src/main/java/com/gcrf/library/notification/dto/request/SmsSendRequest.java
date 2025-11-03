package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送短信请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class SmsSendRequest {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    /**
     * 短信内容
     */
    @NotBlank(message = "短信内容不能为空")
    @Size(max = 500, message = "短信内容长度不能超过500个字符")
    private String content;

    /**
     * 短信类型: VERIFICATION-验证码, NOTIFICATION-通知, MARKETING-营销
     */
    @NotBlank(message = "短信类型不能为空")
    @Pattern(regexp = "^(VERIFICATION|NOTIFICATION|MARKETING)$", message = "短信类型必须为VERIFICATION、NOTIFICATION或MARKETING")
    private String smsType;

    /**
     * 验证码（验证码类型时必填）
     */
    @Size(max = 10, message = "验证码长度不能超过10个字符")
    private String verificationCode;

    /**
     * 验证码过期分钟数（默认5分钟）
     */
    private Integer expiresMinutes = 5;
}
