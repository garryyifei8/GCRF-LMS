package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.RoleCreateRequest;
import com.gcrf.library.system.dto.request.RoleQueryRequest;
import com.gcrf.library.system.dto.request.RoleUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;
import com.gcrf.library.system.dto.response.RoleDetailVO;
import com.gcrf.library.system.dto.response.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface RoleService {

    /**
     * 分页查询角色
     */
    PageResult<RoleVO> queryRoles(RoleQueryRequest request);

    /**
     * 根据ID获取角色详情
     */
    RoleDetailVO getRoleById(Long id);

    /**
     * 创建角色
     */
    RoleDetailVO createRole(RoleCreateRequest request);

    /**
     * 更新角色
     */
    RoleDetailVO updateRole(Long id, RoleUpdateRequest request);

    /**
     * 删除角色（软删除）
     */
    void deleteRole(Long id);

    /**
     * 为角色分配权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的权限列表
     */
    List<PermissionVO> getRolePermissions(Long roleId);
}
