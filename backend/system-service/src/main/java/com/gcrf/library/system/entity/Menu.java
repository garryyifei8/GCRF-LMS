package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("menus")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 父菜单ID (NULL=根菜单)
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 路由路径
     */
    @TableField("path")
    private String path;

    /**
     * 组件路径
     */
    @TableField("component")
    private String component;

    /**
     * 重定向路径
     */
    @TableField("redirect")
    private String redirect;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 菜单类型: DIR-目录, MENU-菜单, BUTTON-按钮
     */
    @TableField("menu_type")
    private String menuType;

    /**
     * 显示顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否可见
     */
    @TableField("is_visible")
    private Boolean isVisible;

    /**
     * 是否缓存
     */
    @TableField("is_cache")
    private Boolean isCache;

    /**
     * 是否外链
     */
    @TableField("is_external")
    private Boolean isExternal;

    /**
     * 权限标识 (关联permissions.permission_code)
     */
    @TableField("permission_code")
    private String permissionCode;

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
