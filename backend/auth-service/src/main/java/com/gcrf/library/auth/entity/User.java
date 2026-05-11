package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
@TableName("gcrf_region.users")
public class User {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户唯一标识（业务ID）
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名（登录名）
     */
    private String username;

    /**
     * 密码（加密后）
     */
    private String password;

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
    @TableField("user_type")
    private String userType;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 账号状态：ACTIVE-启用 INACTIVE-禁用 LOCKED-锁定
     */
    private String status;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 连续登录失败次数
     */
    @TableField("failed_login_count")
    private Integer failedLoginCount;

    /**
     * 账号锁定截止时间
     */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除标识（软删除）NULL-未删除 timestamp-已删除时间
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 组织节点ID（多租户架构：关联 gcrf_region.org_node.id）
     */
    @TableField("org_node_id")
    private Long orgNodeId;

    /**
     * 学校ID（多租户架构：快捷字段，冗余自 org_node）
     */
    @TableField("school_id")
    private Long schoolId;

    /**
     * 租户 Schema 名称（多租户架构：如 school_001、school_002）
     */
    @TableField("tenant_schema")
    private String tenantSchema;
}
