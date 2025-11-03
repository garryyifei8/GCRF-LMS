package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("permissions")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码 (格式: system:user:list)
     */
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限名称
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 资源类型: API-接口, MENU-菜单, BUTTON-按钮
     */
    @TableField("resource_type")
    private String resourceType;

    /**
     * 资源路径 (API路径或菜单路径)
     */
    @TableField("resource_path")
    private String resourcePath;

    /**
     * HTTP方法: GET, POST, PUT, DELETE, PATCH
     */
    @TableField("http_method")
    private String httpMethod;

    /**
     * 权限分组
     */
    @TableField("permission_group")
    private String permissionGroup;

    /**
     * 显示顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    @TableField("status")
    private String status;

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
     * 删除时间（软删除标记）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
