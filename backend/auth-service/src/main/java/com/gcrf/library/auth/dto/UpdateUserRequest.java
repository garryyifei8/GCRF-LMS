package com.gcrf.library.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户请求DTO
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

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
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 账号状态：ACTIVE-启用 INACTIVE-禁用 LOCKED-锁定
     */
    @Pattern(regexp = "^(ACTIVE|INACTIVE|LOCKED)$", message = "状态必须是ACTIVE、INACTIVE或LOCKED")
    private String status;
}
