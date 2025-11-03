package com.gcrf.library.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建用户请求DTO
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名必须是4-20位字母、数字或下划线")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,32}$",
             message = "密码必须包含大小写字母和数字,长度8-32位")
    private String password;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 用户类型：STUDENT-学生 TEACHER-教师 ADMIN-管理员
     */
    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER|ADMIN)$", message = "用户类型必须是STUDENT、TEACHER或ADMIN")
    private String userType;

    /**
     * 头像URL
     */
    private String avatarUrl;
}
