package com.gcrf.library.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 登录响应DTO
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
@Builder
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
    @Builder.Default
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

    // ── IAM rich fields (Task 8) ──────────────────────────────────────────────

    /** Opaque refresh token — stored in Redis by Task 9. */
    private String refreshToken;

    /** Role codes assigned to this user, e.g. ["REGION_ADMIN"]. */
    private List<String> roles;

    /** Tenant schema name (null for region-level users). */
    private String tenant;

    /** School/tenant ID (null for region-level users). */
    private Long tenantId;

    /** Effective data-access scope: SELF | CLASS | GRADE | SCHOOL | REGION. */
    private String scope;

    /** Flat set of permission codes, e.g. {"book.read","book.write"}. */
    private Set<String> permissions;

    /**
     * Backward-compat constructor used by existing callers (refreshToken path etc.)
     * that were written before the rich fields were added.
     */
    public LoginResponse(String accessToken, Long expiresIn, Long userId, String username, String userType) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.userType = userType;
    }
}
