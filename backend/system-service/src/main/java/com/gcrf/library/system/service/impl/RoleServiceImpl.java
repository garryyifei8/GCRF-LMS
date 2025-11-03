package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.RoleCreateRequest;
import com.gcrf.library.system.dto.request.RoleQueryRequest;
import com.gcrf.library.system.dto.request.RoleUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;
import com.gcrf.library.system.dto.response.RoleDetailVO;
import com.gcrf.library.system.dto.response.RoleVO;
import com.gcrf.library.system.entity.Permission;
import com.gcrf.library.system.entity.Role;
import com.gcrf.library.system.entity.RolePermission;
import com.gcrf.library.system.mapper.PermissionMapper;
import com.gcrf.library.system.mapper.RoleMapper;
import com.gcrf.library.system.mapper.RolePermissionMapper;
import com.gcrf.library.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<RoleVO> queryRoles(RoleQueryRequest request) {
        Page<Role> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Role::getDeletedAt)
               .like(StringUtils.hasText(request.getRoleCode()), Role::getRoleCode, request.getRoleCode())
               .like(StringUtils.hasText(request.getRoleName()), Role::getRoleName, request.getRoleName())
               .eq(StringUtils.hasText(request.getStatus()), Role::getStatus, request.getStatus())
               .eq(StringUtils.hasText(request.getDataScope()), Role::getDataScope, request.getDataScope())
               .orderByAsc(Role::getSortOrder)
               .orderByDesc(Role::getCreatedAt);

        Page<Role> rolePage = roleMapper.selectPage(page, wrapper);

        List<RoleVO> roleVOList = rolePage.getRecords().stream()
                .map(RoleVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                rolePage.getTotal(),
                (int) rolePage.getCurrent(),
                (int) rolePage.getSize(),
                roleVOList
        );
    }

    @Override
    public RoleDetailVO getRoleById(Long id) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getId, id)
                .isNull(Role::getDeletedAt)
        );
        if (role == null) {
            throw new BusinessException("角色不存在, id: " + id);
        }

        RoleDetailVO vo = RoleDetailVO.from(role);

        // 查询角色的权限列表
        List<PermissionVO> permissions = getRolePermissions(id);
        vo.setPermissions(permissions);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDetailVO createRole(RoleCreateRequest request) {
        // 检查角色编码是否已存在
        Long count = roleMapper.selectCount(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, request.getRoleCode())
                .isNull(Role::getDeletedAt)
        );
        if (count > 0) {
            throw new BusinessException("角色编码已存在: " + request.getRoleCode());
        }

        // 创建角色
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRoleDesc(request.getRoleDesc());
        role.setDataScope(request.getDataScope());
        role.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        role.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        roleMapper.insert(role);
        log.info("创建角色成功, roleCode: {}", role.getRoleCode());

        return RoleDetailVO.from(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDetailVO updateRole(Long id, RoleUpdateRequest request) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getId, id)
                .isNull(Role::getDeletedAt)
        );
        if (role == null) {
            throw new BusinessException("角色不存在, id: " + id);
        }

        // 更新可修改字段
        if (StringUtils.hasText(request.getRoleName())) {
            role.setRoleName(request.getRoleName());
        }
        if (StringUtils.hasText(request.getRoleDesc())) {
            role.setRoleDesc(request.getRoleDesc());
        }
        if (StringUtils.hasText(request.getDataScope())) {
            role.setDataScope(request.getDataScope());
        }
        if (request.getSortOrder() != null) {
            role.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.hasText(request.getStatus())) {
            role.setStatus(request.getStatus());
        }

        roleMapper.updateById(role);
        log.info("更新角色成功, id: {}", id);

        return RoleDetailVO.from(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getId, id)
                .isNull(Role::getDeletedAt)
        );
        if (role == null) {
            throw new BusinessException("角色不存在, id: " + id);
        }

        // 软删除 - 设置deleted_at时间戳
        role.setDeletedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        log.info("删除角色成功, id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 检查角色是否存在
        Role role = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getId, roleId)
                .isNull(Role::getDeletedAt)
        );
        if (role == null) {
            throw new BusinessException("角色不存在, id: " + roleId);
        }

        // 删除角色的所有权限关联
        rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
        );

        // 批量插入新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }

        log.info("为角色分配权限成功, roleId: {}, permissionCount: {}", roleId, permissionIds.size());
    }

    @Override
    public List<PermissionVO> getRolePermissions(Long roleId) {
        // 查询角色的权限ID列表
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
        );

        if (rolePermissions.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 查询权限详情
        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);

        return permissions.stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionVO::from)
                .collect(Collectors.toList());
    }
}
