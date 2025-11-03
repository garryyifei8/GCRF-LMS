package com.gcrf.library.system.dto.request;

import lombok.Data;

/**
 * 权限查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class PermissionQueryRequest {

    /**
     * 权限编码（模糊查询）
     */
    private String permissionCode;

    /**
     * 权限名称（模糊查询）
     */
    private String permissionName;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 权限分组
     */
    private String permissionGroup;

    /**
     * 状态
     */
    private String status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
