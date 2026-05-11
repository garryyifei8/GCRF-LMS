package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.entity.Permission;
import com.gcrf.library.auth.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public List<Permission> listAll() {
        return permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortOrder));
    }

    public List<Permission> listForRole(Long roleId) {
        return permissionMapper.findByRoleId(roleId);
    }

    public Set<String> codesForUser(Long userId) {
        return Set.copyOf(permissionMapper.findCodesByUserId(userId));
    }
}
