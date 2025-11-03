package com.gcrf.library.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型（Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户类型：STUDENT-学生 TEACHER-教师 ADMIN-管理员
     */
    private String userType;

    public LoginResponse(String accessToken, Long expiresIn, Long userId, String username, String userType) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.userType = userType;
    }
}
