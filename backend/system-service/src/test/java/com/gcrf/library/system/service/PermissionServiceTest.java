package com.gcrf.library.system.service;

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
import com.gcrf.library.system.service.impl.PermissionServiceImpl;
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
 * PermissionService单元测试
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission testPermission;
    private PermissionQueryRequest queryRequest;
    private PermissionCreateRequest createRequest;
    private PermissionUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setPermissionCode("user:read");
        testPermission.setPermissionName("查看用户");
        testPermission.setResourceType("MENU");
        testPermission.setResourcePath("/users");
        testPermission.setHttpMethod("GET");
        testPermission.setSortOrder(1);
        testPermission.setStatus("ACTIVE");
        testPermission.setCreatedAt(LocalDateTime.now());

        queryRequest = new PermissionQueryRequest();
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);

        createRequest = new PermissionCreateRequest();
        createRequest.setPermissionCode("user:write");
        createRequest.setPermissionName("编辑用户");
        createRequest.setResourceType("BUTTON");
        createRequest.setResourcePath("/users/*");
        createRequest.setHttpMethod("POST");
        createRequest.setSortOrder(2);
        createRequest.setStatus("ACTIVE");

        updateRequest = new PermissionUpdateRequest();
        updateRequest.setPermissionName("修改用户");
        updateRequest.setResourcePath("/users/update");
    }

    @Test
    void testQueryPermissions_Success() {
        // Given
        Page<Permission> page = new Page<>();
        page.setRecords(Arrays.asList(testPermission));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<PermissionVO> result = permissionService.queryPermissions(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getPermissionCode()).isEqualTo("user:read");
    }

    @Test
    void testListAllPermissions_Success() {
        // Given
        Permission p2 = new Permission();
        p2.setId(2L);
        p2.setPermissionCode("user:write");
        p2.setPermissionName("编辑用户");

        when(permissionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(testPermission, p2));

        // When
        List<PermissionVO> result = permissionService.listAllPermissions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPermissionCode()).isEqualTo("user:read");
        assertThat(result.get(1).getPermissionCode()).isEqualTo("user:write");
    }

    @Test
    void testGetPermissionById_Success() {
        // Given
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testPermission);

        // When
        PermissionVO result = permissionService.getPermissionById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPermissionCode()).isEqualTo("user:read");
    }

    @Test
    void testGetPermissionById_NotFound() {
        // Given
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> permissionService.getPermissionById(999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("权限不存在");
    }

    @Test
    void testCreatePermission_Success() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(0L);
        when(permissionMapper.insert(any(Permission.class)))
            .thenReturn(1);

        // When
        PermissionVO result = permissionService.createPermission(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPermissionCode()).isEqualTo("user:write");
        verify(permissionMapper).insert(any(Permission.class));
    }

    @Test
    void testCreatePermission_DuplicateCode() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> permissionService.createPermission(createRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("权限编码已存在");
        verify(permissionMapper, never()).insert(any(Permission.class));
    }

    @Test
    void testUpdatePermission_Success() {
        // Given
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testPermission);
        when(permissionMapper.updateById(any(Permission.class)))
            .thenReturn(1);

        // When
        PermissionVO result = permissionService.updatePermission(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPermissionName()).isEqualTo("修改用户");
        verify(permissionMapper).updateById(any(Permission.class));
    }

    @Test
    void testDeletePermission_Success() {
        // Given
        when(permissionMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(testPermission);
        when(permissionMapper.updateById(any(Permission.class)))
            .thenReturn(1);

        // When
        permissionService.deletePermission(1L);

        // Then
        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void testQueryPermissions_WithFilters() {
        // Given
        queryRequest.setPermissionCode("user");
        queryRequest.setPermissionName("查看");
        queryRequest.setResourceType("MENU");
        queryRequest.setStatus("ACTIVE");

        Page<Permission> page = new Page<>();
        page.setRecords(Arrays.asList(testPermission));
        page.setTotal(1);

        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<PermissionVO> result = permissionService.queryPermissions(queryRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void testQueryPermissions_SoftDelete() {
        // Given
        Permission deletedPermission = new Permission();
        deletedPermission.setId(2L);
        deletedPermission.setDeletedAt(LocalDateTime.now());

        Page<Permission> page = new Page<>();
        page.setRecords(Arrays.asList(testPermission)); // 不包含已删除的
        page.setTotal(1);

        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        PageResult<PermissionVO> result = permissionService.queryPermissions(queryRequest);

        // Then
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getPermissionCode()).isEqualTo("user:read");
    }
}
