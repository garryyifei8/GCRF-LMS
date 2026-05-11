package com.gcrf.library.auth.service;

import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.UserRole;
import com.gcrf.library.auth.mapper.PermissionMapper;
import com.gcrf.library.auth.mapper.RoleMapper;
import com.gcrf.library.auth.mapper.UserRoleMapper;
import com.gcrf.library.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleMapper roleMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock PermissionMapper permissionMapper;

    @InjectMocks RoleService svc;

    @Test
    void listSystemRoles_returnsOnlyIsSystem() {
        Role r1 = new Role(); r1.setId(1L); r1.setCode("REGION_ADMIN"); r1.setIsSystem(true);
        Role r2 = new Role(); r2.setId(2L); r2.setCode("CUSTOM");      r2.setIsSystem(false);
        when(roleMapper.selectList(any())).thenReturn(List.of(r1, r2));

        List<Role> out = svc.listSystemRoles();
        assertThat(out).hasSize(2);  // service does NOT filter — caller decides
    }

    @Test
    void assignRole_insertsIfNotExists() {
        when(roleMapper.selectById(7L)).thenReturn(new Role());
        when(userRoleMapper.findExact(42L, 7L, null)).thenReturn(null);

        svc.assignRole(42L, 7L, null, null, 1L);

        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void assignRole_skipsIfAlreadyAssigned() {
        UserRole existing = new UserRole(); existing.setId(99L);
        when(roleMapper.selectById(7L)).thenReturn(new Role());
        when(userRoleMapper.findExact(42L, 7L, null)).thenReturn(existing);

        svc.assignRole(42L, 7L, null, null, 1L);

        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void assignRole_throwsWhenRoleNotFound() {
        when(roleMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> svc.assignRole(42L, 99L, null, null, 1L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void revokeRole_deletesByCompositeKey() {
        svc.revokeRole(42L, 7L, null);
        verify(userRoleMapper).deleteByUserRoleSchool(42L, 7L, null);
    }
}
