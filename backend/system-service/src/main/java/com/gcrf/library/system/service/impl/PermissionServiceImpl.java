package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.PermissionCreateRequest;
import com.gcrf.library.system.dto.request.PermissionQueryRequest;
import com.gcrf.library.system.dto.request.PermissionUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;
import com.gcrf.library.system.entity.Permission;
import com.gcrf.library.system.mapper.PermissionMapper;
import com.gcrf.library.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public PageResult<PermissionVO> queryPermissions(PermissionQueryRequest request) {
        Page<Permission> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Permission::getDeletedAt)
               .like(StringUtils.hasText(request.getPermissionCode()), Permission::getPermissionCode, request.getPermissionCode())
               .like(StringUtils.hasText(request.getPermissionName()), Permission::getPermissionName, request.getPermissionName())
               .eq(StringUtils.hasText(request.getResourceType()), Permission::getResourceType, request.getResourceType())
               .eq(StringUtils.hasText(request.getPermissionGroup()), Permission::getPermissionGroup, request.getPermissionGroup())
               .eq(StringUtils.hasText(request.getStatus()), Permission::getStatus, request.getStatus())
               .orderByAsc(Permission::getSortOrder);

        Page<Permission> permissionPage = permissionMapper.selectPage(page, wrapper);

        List<PermissionVO> permissionVOList = permissionPage.getRecords().stream()
                .map(PermissionVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                permissionPage.getTotal(),
                (int) permissionPage.getCurrent(),
                (int) permissionPage.getSize(),
                permissionVOList
        );
    }

    @Override
    public List<PermissionVO> listAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>()
                .isNull(Permission::getDeletedAt)
                .orderByAsc(Permission::getSortOrder)
        );

        return permissions.stream()
                .map(PermissionVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionVO getPermissionById(Long id) {
        Permission permission = permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getId, id)
                .isNull(Permission::getDeletedAt)
        );
        if (permission == null) {
            throw new BusinessException("权限不存在, id: " + id);
        }
        return PermissionVO.from(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO createPermission(PermissionCreateRequest request) {
        Long count = permissionMapper.selectCount(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionCode, request.getPermissionCode())
                .isNull(Permission::getDeletedAt)
        );
        if (count > 0) {
            throw new BusinessException("权限编码已存在: " + request.getPermissionCode());
        }

        Permission permission = new Permission();
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionName(request.getPermissionName());
        permission.setResourceType(request.getResourceType());
        permission.setResourcePath(request.getResourcePath());
        permission.setHttpMethod(request.getHttpMethod());
        permission.setPermissionGroup(request.getPermissionGroup());
        permission.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        permission.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        permissionMapper.insert(permission);
        log.info("创建权限成功, permissionCode: {}", permission.getPermissionCode());

        return PermissionVO.from(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO updatePermission(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getId, id)
                .isNull(Permission::getDeletedAt)
        );
        if (permission == null) {
            throw new BusinessException("权限不存在, id: " + id);
        }

        if (StringUtils.hasText(request.getPermissionName())) {
            permission.setPermissionName(request.getPermissionName());
        }
        if (StringUtils.hasText(request.getResourceType())) {
            permission.setResourceType(request.getResourceType());
        }
        if (StringUtils.hasText(request.getResourcePath())) {
            permission.setResourcePath(request.getResourcePath());
        }
        if (StringUtils.hasText(request.getHttpMethod())) {
            permission.setHttpMethod(request.getHttpMethod());
        }
        if (StringUtils.hasText(request.getPermissionGroup())) {
            permission.setPermissionGroup(request.getPermissionGroup());
        }
        if (request.getSortOrder() != null) {
            permission.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.hasText(request.getStatus())) {
            permission.setStatus(request.getStatus());
        }

        permissionMapper.updateById(permission);
        log.info("更新权限成功, id: {}", id);

        return PermissionVO.from(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        Permission permission = permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getId, id)
                .isNull(Permission::getDeletedAt)
        );
        if (permission == null) {
            throw new BusinessException("权限不存在, id: " + id);
        }

        permission.setDeletedAt(LocalDateTime.now());
        permissionMapper.updateById(permission);
        log.info("删除权限成功, id: {}", id);
    }
}
