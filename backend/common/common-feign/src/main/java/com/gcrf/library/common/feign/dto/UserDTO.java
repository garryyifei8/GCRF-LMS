package com.gcrf.library.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户DTO - 用于服务间调用
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户类型：ADMIN-管理员, LIBRARIAN-图书馆员, READER-读者
     */
    private String userType;

    /**
     * 关联的读者ID（如果是读者用户）
     */
    private Long readerId;

    /**
     * 状态：ACTIVE-正常, DISABLED-禁用
     */
    private String status;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
