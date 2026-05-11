package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.UserRole;
import com.gcrf.library.auth.mapper.PermissionMapper;
import com.gcrf.library.auth.mapper.RoleMapper;
import com.gcrf.library.auth.mapper.UserRoleMapper;
import com.gcrf.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;

    public List<Role> listSystemRoles() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<Role>().orderByAsc(Role::getSortOrder));
    }

    public Role getById(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(404, "角色不存在: " + id);
        return r;
    }

    public List<Role> rolesOfUser(Long userId) {
        return roleMapper.findByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRole(Long userId, Long roleId, Long schoolId,
                           LocalDateTime expiresAt, Long operatorId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(404, "角色不存在: " + roleId);
        }
        if (userRoleMapper.findExact(userId, roleId, schoolId) != null) {
            return;
        }
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setSchoolId(schoolId);
        ur.setAssignedBy(operatorId);
        ur.setAssignedAt(LocalDateTime.now());
        ur.setExpiresAt(expiresAt);
        userRoleMapper.insert(ur);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeRole(Long userId, Long roleId, Long schoolId) {
        userRoleMapper.deleteByUserRoleSchool(userId, roleId, schoolId);
    }
}
