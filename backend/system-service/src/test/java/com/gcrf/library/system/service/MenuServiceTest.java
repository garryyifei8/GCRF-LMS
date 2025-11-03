package com.gcrf.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.system.dto.request.MenuCreateRequest;
import com.gcrf.library.system.dto.request.MenuUpdateRequest;
import com.gcrf.library.system.dto.response.MenuTreeVO;
import com.gcrf.library.system.dto.response.MenuVO;
import com.gcrf.library.system.entity.Menu;
import com.gcrf.library.system.entity.RoleMenu;
import com.gcrf.library.system.mapper.MenuMapper;
import com.gcrf.library.system.mapper.RoleMenuMapper;
import com.gcrf.library.system.service.impl.MenuServiceImpl;
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
 * MenuService单元测试
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    private Menu rootMenu;
    private Menu childMenu;
    private MenuCreateRequest createRequest;
    private MenuUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        rootMenu = new Menu();
        rootMenu.setId(1L);
        rootMenu.setMenuName("系统管理");
        rootMenu.setParentId(null);
        rootMenu.setPath("/system");
        rootMenu.setComponent("Layout");
        rootMenu.setMenuType("DIR");
        rootMenu.setSortOrder(1);
        rootMenu.setIsVisible(true);
        rootMenu.setStatus("ACTIVE");
        rootMenu.setCreatedAt(LocalDateTime.now());

        childMenu = new Menu();
        childMenu.setId(2L);
        childMenu.setMenuName("用户管理");
        childMenu.setParentId(1L);
        childMenu.setPath("/system/users");
        childMenu.setComponent("system/users/index");
        childMenu.setMenuType("MENU");
        childMenu.setSortOrder(1);
        childMenu.setIsVisible(true);
        childMenu.setStatus("ACTIVE");
        childMenu.setCreatedAt(LocalDateTime.now());

        createRequest = new MenuCreateRequest();
        createRequest.setMenuName("角色管理");
        createRequest.setParentId(1L);
        createRequest.setPath("/system/roles");
        createRequest.setComponent("system/roles/index");
        createRequest.setMenuType("MENU");
        createRequest.setSortOrder(2);
        createRequest.setIsVisible(true);
        createRequest.setStatus("ACTIVE");

        updateRequest = new MenuUpdateRequest();
        updateRequest.setMenuName("用户列表");
        updateRequest.setSortOrder(0);
    }

    @Test
    void testGetMenuTree_Success() {
        // Given
        when(menuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(rootMenu, childMenu));

        // When
        List<MenuTreeVO> result = menuService.getMenuTree();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getMenuName()).isEqualTo("系统管理");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getMenuName()).isEqualTo("用户管理");
    }

    @Test
    void testGetMenuTree_EmptyTree() {
        // Given
        when(menuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        // When
        List<MenuTreeVO> result = menuService.getMenuTree();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetMenuTree_MultiLevel() {
        // Given
        Menu grandChild = new Menu();
        grandChild.setId(3L);
        grandChild.setMenuName("用户详情");
        grandChild.setParentId(2L);
        grandChild.setPath("/system/users/detail");
        grandChild.setMenuType("BUTTON");
        grandChild.setSortOrder(1);
        grandChild.setIsVisible(true);

        when(menuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(rootMenu, childMenu, grandChild));

        // When
        List<MenuTreeVO> result = menuService.getMenuTree();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    @Test
    void testGetUserMenus_Success() {
        // Given
        RoleMenu rm1 = new RoleMenu();
        rm1.setRoleId(1L);
        rm1.setMenuId(1L);

        RoleMenu rm2 = new RoleMenu();
        rm2.setRoleId(1L);
        rm2.setMenuId(2L);

        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(rm1, rm2));
        when(menuMapper.selectBatchIds(any(List.class)))
            .thenReturn(Arrays.asList(rootMenu, childMenu));

        // When
        List<MenuTreeVO> result = menuService.getUserMenus(1L);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getMenuName()).isEqualTo("系统管理");
    }

    @Test
    void testGetUserMenus_EmptyResult() {
        // Given
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        // When
        List<MenuTreeVO> result = menuService.getUserMenus(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetMenuById_Success() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(rootMenu);

        // When
        MenuVO result = menuService.getMenuById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMenuName()).isEqualTo("系统管理");
    }

    @Test
    void testCreateMenu_Success() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(rootMenu); // 父菜单存在
        when(menuMapper.insert(any(Menu.class)))
            .thenReturn(1);

        // When
        MenuVO result = menuService.createMenu(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMenuName()).isEqualTo("角色管理");
        verify(menuMapper).insert(any(Menu.class));
    }

    @Test
    void testCreateMenu_InvalidParent() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(null); // 父菜单不存在

        // When & Then
        assertThatThrownBy(() -> menuService.createMenu(createRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("父菜单不存在");
        verify(menuMapper, never()).insert(any(Menu.class));
    }

    @Test
    void testUpdateMenu_Success() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(childMenu);
        when(menuMapper.updateById(any(Menu.class)))
            .thenReturn(1);

        // When
        MenuVO result = menuService.updateMenu(2L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMenuName()).isEqualTo("用户列表");
        verify(menuMapper).updateById(any(Menu.class));
    }

    @Test
    void testDeleteMenu_Success() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(childMenu);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(0L); // 无子菜单
        when(menuMapper.updateById(any(Menu.class)))
            .thenReturn(1);

        // When
        menuService.deleteMenu(2L);

        // Then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuMapper).updateById(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void testDeleteMenu_HasChildren() {
        // Given
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(rootMenu);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(1L); // 有子菜单

        // When & Then
        assertThatThrownBy(() -> menuService.deleteMenu(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("存在子菜单");
        verify(menuMapper, never()).updateById(any(Menu.class));
    }

    @Test
    void testGetUserMenus_FiltersByRole() {
        // Given
        childMenu.setIsVisible(false); // 不可见菜单

        RoleMenu rm = new RoleMenu();
        rm.setMenuId(2L);

        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(rm));
        when(menuMapper.selectBatchIds(any(List.class)))
            .thenReturn(List.of(childMenu));

        // When
        List<MenuTreeVO> result = menuService.getUserMenus(1L);

        // Then
        assertThat(result).isEmpty(); // 不可见菜单被过滤
    }
}
