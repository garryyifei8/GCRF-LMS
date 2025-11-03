package com.gcrf.library.system.dto.response;

import com.gcrf.library.system.entity.Permission;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class PermissionVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 权限编码 (格式: system:user:list)
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 资源类型: API-接口, MENU-菜单, BUTTON-按钮
     */
    private String resourceType;

    /**
     * 资源路径 (API路径或菜单路径)
     */
    private String resourcePath;

    /**
     * HTTP方法: GET, POST, PUT, DELETE, PATCH
     */
    private String httpMethod;

    /**
     * 权限分组
     */
    private String permissionGroup;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static PermissionVO from(Permission permission) {
        if (permission == null) {
            return null;
        }
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setPermissionName(permission.getPermissionName());
        vo.setResourceType(permission.getResourceType());
        vo.setResourcePath(permission.getResourcePath());
        vo.setHttpMethod(permission.getHttpMethod());
        vo.setPermissionGroup(permission.getPermissionGroup());
        vo.setSortOrder(permission.getSortOrder());
        vo.setStatus(permission.getStatus());
        vo.setCreatedAt(permission.getCreatedAt());
        vo.setUpdatedAt(permission.getUpdatedAt());
        return vo;
    }
}
