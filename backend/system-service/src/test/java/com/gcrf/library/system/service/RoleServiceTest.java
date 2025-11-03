package com.gcrf.library.system.service;

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
import com.gcrf.library.system.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoleService单元测试
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;
    private RoleQueryRequest queryRequest;
    private RoleCreateRequest createRequest;
    private RoleUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRoleCode("ADMIN");
        testRole.setRoleName("管理员");
        testRole.setRoleDesc("系统管理员");
        testRole.setDataScope("ALL");
        testRole.setSortOrder(1);
        testRole.setStatus("ACTIVE");
        testRole.setCreatedAt(LocalDateTime.now());
        testRole.setUpdatedAt(LocalDateTime.now());

        queryRequest = new RoleQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

        createRequest = new RoleCreateRequest();
        createRequest.setRoleCode("MANAGER");
        createRequest.setRoleName("经理");
        createRequest.setRoleDesc("部门经理");
        createRequest.setDataScope("DEPT");
        createRequest.setSortOrder(2);
        createRequest.setStatus("ACTIVE");

        updateRequest = new RoleUpdateRequest();
        updateRequest.setRoleName("高级管理员");
        updateRequest.setRoleDesc("高级系统管理员");
        updateRequest.setSortOrder(0);
    }

    @Test
    void testQueryRoles_Success() {
        // Given
        Page<Role> page = new Page<>();
        page.setRecords(Arrays.asList(testRole));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<RoleVO> result = roleService.queryRoles(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRoleCode()).isEqualTo("ADMIN");
        verify(roleMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testQueryRoles_WithFilters() {
        // Given
        queryRequest.setRoleCode("ADMIN");
        queryRequest.setRoleName("管理");
        queryRequest.setStatus("ACTIVE");
        queryRequest.setDataScope("ALL");

        Page<Role> page = new Page<>();
        page.setRecords(Arrays.asList(testRole));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<RoleVO> result = roleService.queryRoles(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        verify(roleMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetRoleById_Success() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testRole);
        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        // When
        RoleDetailVO result = roleService.getRoleById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoleCode()).isEqualTo("ADMIN");
        assertThat(result.getPermissions()).isEmpty();
        verify(roleMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetRoleById_NotFound() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> roleService.getRoleById(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("角色不存在");
    }

    @Test
    void testCreateRole_Success() {
        // Given
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(0L);
        when(roleMapper.insert(any(Role.class)))
            .thenReturn(1);

        // When
        RoleDetailVO result = roleService.createRole(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoleCode()).isEqualTo("MANAGER");
        verify(roleMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(roleMapper).insert(any(Role.class));
    }

    @Test
    void testCreateRole_DuplicateCode() {
        // Given
        when(roleMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> roleService.createRole(createRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("角色编码已存在");
        verify(roleMapper, never()).insert(any(Role.class));
    }

    @Test
    void testUpdateRole_Success() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testRole);
        when(roleMapper.updateById(any(Role.class)))
            .thenReturn(1);

        // When
        RoleDetailVO result = roleService.updateRole(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoleName()).isEqualTo("高级管理员");
        verify(roleMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(roleMapper).updateById(any(Role.class));
    }

    @Test
    void testUpdateRole_NotFound() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> roleService.updateRole(999L, updateRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("角色不存在");
        verify(roleMapper, never()).updateById(any(Role.class));
    }

    @Test
    void testDeleteRole_Success() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testRole);
        when(roleMapper.updateById(any(Role.class)))
            .thenReturn(1);

        // When
        roleService.deleteRole(1L);

        // Then
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void testDeleteRole_NotFound() {
        // Given
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> roleService.deleteRole(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("角色不存在");
    }

    @Test
    void testAssignPermissions_Success() {
        // Given
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testRole);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class)))
            .thenReturn(0);
        when(rolePermissionMapper.insert(any(RolePermission.class)))
            .thenReturn(1);

        // When
        roleService.assignPermissions(1L, permissionIds);

        // Then
        verify(roleMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper, times(3)).insert(any(RolePermission.class));
    }

    @Test
    void testAssignPermissions_RemoveOldAndAddNew() {
        // Given
        List<Long> permissionIds = Arrays.asList(4L, 5L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testRole);
        when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class)))
            .thenReturn(3); // 删除旧的3个权限
        when(rolePermissionMapper.insert(any(RolePermission.class)))
            .thenReturn(1);

        // When
        roleService.assignPermissions(1L, permissionIds);

        // Then
        verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
        verify(rolePermissionMapper, times(2)).insert(any(RolePermission.class));
    }

    @Test
    void testGetRolePermissions_Success() {
        // Given
        RolePermission rp1 = new RolePermission();
        rp1.setRoleId(1L);
        rp1.setPermissionId(1L);

        RolePermission rp2 = new RolePermission();
        rp2.setRoleId(1L);
        rp2.setPermissionId(2L);

        Permission p1 = new Permission();
        p1.setId(1L);
        p1.setPermissionCode("user:read");
        p1.setPermissionName("查看用户");

        Permission p2 = new Permission();
        p2.setId(2L);
        p2.setPermissionCode("user:write");
        p2.setPermissionName("编辑用户");

        when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(rp1, rp2));
        when(permissionMapper.selectBatchIds(any(List.class)))
            .thenReturn(Arrays.asList(p1, p2));

        // When
        List<PermissionVO> result = roleService.getRolePermissions(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPermissionCode()).isEqualTo("user:read");
        assertThat(result.get(1).getPermissionCode()).isEqualTo("user:write");
    }

    @Test
    void testQueryRoles_Pagination() {
        // Given
        queryRequest.setPageNum(2);
        queryRequest.setPageSize(5);

        Page<Role> page = new Page<>();
        page.setRecords(List.of());
        page.setTotal(0);
        page.setCurrent(2);
        page.setSize(5);

        when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<RoleVO> result = roleService.queryRoles(queryRequest);

        // Then
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotal()).isEqualTo(0);
    }
}
