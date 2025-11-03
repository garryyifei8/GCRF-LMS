package com.gcrf.library.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建菜单请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class MenuCreateRequest {

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100, message = "菜单名称长度不能超过100个字符")
    private String menuName;

    /**
     * 父菜单ID (NULL=根菜单)
     */
    private Long parentId;

    /**
     * 路由路径
     */
    @Size(max = 200, message = "路由路径长度不能超过200个字符")
    private String path;

    /**
     * 组件路径
     */
    @Size(max = 200, message = "组件路径长度不能超过200个字符")
    private String component;

    /**
     * 重定向路径
     */
    @Size(max = 200, message = "重定向路径长度不能超过200个字符")
    private String redirect;

    /**
     * 菜单图标
     */
    @Size(max = 100, message = "菜单图标长度不能超过100个字符")
    private String icon;

    /**
     * 菜单类型: DIR-目录, MENU-菜单, BUTTON-按钮
     */
    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "^(DIR|MENU|BUTTON)$", message = "菜单类型必须为DIR、MENU或BUTTON")
    private String menuType;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 是否可见
     */
    private Boolean isVisible;

    /**
     * 是否缓存
     */
    private Boolean isCache;

    /**
     * 是否外链
     */
    private Boolean isExternal;

    /**
     * 权限标识 (关联permissions.permission_code)
     */
    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String permissionCode;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态必须为ACTIVE或DISABLED")
    private String status;
}
