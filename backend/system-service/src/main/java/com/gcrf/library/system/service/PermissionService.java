package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.PermissionCreateRequest;
import com.gcrf.library.system.dto.request.PermissionQueryRequest;
import com.gcrf.library.system.dto.request.PermissionUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;

import java.util.List;

/**
 * 权限服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface PermissionService {

    /**
     * 分页查询权限
     */
    PageResult<PermissionVO> queryPermissions(PermissionQueryRequest request);

    /**
     * 获取所有权限列表
     */
    List<PermissionVO> listAllPermissions();

    /**
     * 根据ID获取权限详情
     */
    PermissionVO getPermissionById(Long id);

    /**
     * 创建权限
     */
    PermissionVO createPermission(PermissionCreateRequest request);

    /**
     * 更新权限
     */
    PermissionVO updatePermission(Long id, PermissionUpdateRequest request);

    /**
     * 删除权限（软删除）
     */
    void deletePermission(Long id);
}
