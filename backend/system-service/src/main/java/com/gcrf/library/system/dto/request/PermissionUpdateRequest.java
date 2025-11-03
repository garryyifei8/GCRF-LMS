package com.gcrf.library.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新权限请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class PermissionUpdateRequest {

    /**
     * 主键ID
     */
    @NotNull(message = "权限ID不能为空")
    private Long id;

    /**
     * 权限名称
     */
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String permissionName;

    /**
     * 资源类型: API-接口, MENU-菜单, BUTTON-按钮
     */
    @Pattern(regexp = "^(API|MENU|BUTTON)$", message = "资源类型必须为API、MENU或BUTTON")
    private String resourceType;

    /**
     * 资源路径 (API路径或菜单路径)
     */
    @Size(max = 200, message = "资源路径长度不能超过200个字符")
    private String resourcePath;

    /**
     * HTTP方法: GET, POST, PUT, DELETE, PATCH
     */
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "HTTP方法必须为GET、POST、PUT、DELETE或PATCH")
    private String httpMethod;

    /**
     * 权限分组
     */
    @Size(max = 50, message = "权限分组长度不能超过50个字符")
    private String permissionGroup;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态必须为ACTIVE或DISABLED")
    private String status;
}
