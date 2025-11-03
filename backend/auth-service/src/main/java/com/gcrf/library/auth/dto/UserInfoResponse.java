package com.gcrf.library.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户详细信息响应DTO
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户类型：STUDENT-学生 TEACHER-教师 ADMIN-管理员
     */
    private String userType;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 账号状态：ACTIVE-启用 INACTIVE-禁用 LOCKED-锁定
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
