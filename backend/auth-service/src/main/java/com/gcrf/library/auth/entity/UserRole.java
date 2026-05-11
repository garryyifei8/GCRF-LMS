package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gcrf_region.auth_user_role")
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;
    @TableField("school_id")
    private Long schoolId;
    @TableField("scope_override")
    private String scopeOverride;
    @TableField("scope_path")
    private String scopePath;
    @TableField("assigned_by")
    private Long assignedBy;
    @TableField("assigned_at")
    private LocalDateTime assignedAt;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
