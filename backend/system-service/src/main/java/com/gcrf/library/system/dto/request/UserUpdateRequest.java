package com.gcrf.library.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新用户请求DTO
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Data
@Schema(description = "更新用户请求")
public class UserUpdateRequest {

    @Schema(description = "手机号", example = "13800000000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "user@gcrf.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;
}
