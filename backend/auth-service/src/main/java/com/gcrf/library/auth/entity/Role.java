package com.gcrf.library.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gcrf_region.auth_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    @TableField("scope_default")
    private String scopeDefault;
    @TableField("is_system")
    private Boolean isSystem;
    @TableField("school_id")
    private Long schoolId;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
