package com.gcrf.library.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建用户请求DTO
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Data
@Schema(description = "创建用户请求")
public class UserCreateRequest {

    @Schema(description = "用户名（4-20位字母、数字或下划线）", example = "newuser")
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名必须是4-20位字母、数字或下划线")
    private String username;

    @Schema(description = "密码（8-32位，含大小写字母和数字）", example = "Admin1234")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "手机号", example = "13800000000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "user@gcrf.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "用户类型：STUDENT/TEACHER/ADMIN", example = "ADMIN")
    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER|ADMIN)$", message = "用户类型必须是STUDENT、TEACHER或ADMIN")
    private String userType;

    @Schema(description = "头像URL")
    private String avatarUrl;
}
