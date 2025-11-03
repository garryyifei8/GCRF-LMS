package com.gcrf.library.system.dto.request;

import lombok.Data;

/**
 * 角色查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class RoleQueryRequest {

    /**
     * 角色编码（模糊查询）
     */
    private String roleCode;

    /**
     * 角色名称（模糊查询）
     */
    private String roleName;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    private String status;

    /**
     * 数据范围
     */
    private String dataScope;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
