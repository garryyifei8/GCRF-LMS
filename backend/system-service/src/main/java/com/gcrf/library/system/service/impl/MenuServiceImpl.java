package com.gcrf.library.system.service.impl;

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
import com.gcrf.library.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuTreeVO> getMenuTree() {
        // 查询所有未删除的菜单
        List<Menu> allMenus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>()
                .isNull(Menu::getDeletedAt)
                .orderByAsc(Menu::getSortOrder)
        );

        // 构建树形结构
        return buildMenuTree(allMenus, null);
    }

    @Override
    public List<MenuTreeVO> getUserMenus(Long userId) {
        // 注意: 这里假设有user_roles表来查询用户的角色ID
        // 由于系统设计中用户管理不在system-service，这里简化处理
        // 实际应该从auth-service或通过Feign调用获取用户角色

        // 查询用户的所有角色关联的菜单ID
        // 这里使用简化版本: 查询所有菜单(实际项目中需要根据userId查询role_menus)
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(
            new LambdaQueryWrapper<RoleMenu>()
        );

        if (roleMenus.isEmpty()) {
            return List.of();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 查询菜单详情
        List<Menu> menus = menuMapper.selectBatchIds(menuIds);

        // 过滤已删除和不可见的菜单
        List<Menu> visibleMenus = menus.stream()
                .filter(m -> m.getDeletedAt() == null)
                .filter(m -> Boolean.TRUE.equals(m.getIsVisible()))
                .sorted((m1, m2) -> {
                    int sort = Integer.compare(
                        m1.getSortOrder() != null ? m1.getSortOrder() : 0,
                        m2.getSortOrder() != null ? m2.getSortOrder() : 0
                    );
                    return sort;
                })
                .collect(Collectors.toList());

        // 构建树形结构
        return buildMenuTree(visibleMenus, null);
    }

    @Override
    public MenuVO getMenuById(Long id) {
        Menu menu = menuMapper.selectOne(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getId, id)
                .isNull(Menu::getDeletedAt)
        );
        if (menu == null) {
            throw new BusinessException("菜单不存在, id: " + id);
        }
        return MenuVO.from(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuVO createMenu(MenuCreateRequest request) {
        // 验证父菜单是否存在
        if (request.getParentId() != null) {
            Menu parent = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getId, request.getParentId())
                    .isNull(Menu::getDeletedAt)
            );
            if (parent == null) {
                throw new BusinessException("父菜单不存在, parentId: " + request.getParentId());
            }
        }

        // 创建菜单
        Menu menu = new Menu();
        menu.setMenuName(request.getMenuName());
        menu.setParentId(request.getParentId());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setRedirect(request.getRedirect());
        menu.setIcon(request.getIcon());
        menu.setMenuType(request.getMenuType());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        menu.setIsCache(request.getIsCache() != null ? request.getIsCache() : false);
        menu.setIsExternal(request.getIsExternal() != null ? request.getIsExternal() : false);
        menu.setPermissionCode(request.getPermissionCode());
        menu.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        menuMapper.insert(menu);
        log.info("创建菜单成功, menuName: {}", menu.getMenuName());

        return MenuVO.from(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuVO updateMenu(Long id, MenuUpdateRequest request) {
        Menu menu = menuMapper.selectOne(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getId, id)
                .isNull(Menu::getDeletedAt)
        );
        if (menu == null) {
            throw new BusinessException("菜单不存在, id: " + id);
        }

        // 如果修改父菜单，验证父菜单存在
        if (request.getParentId() != null && !request.getParentId().equals(menu.getParentId())) {
            Menu parent = menuMapper.selectOne(
                new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getId, request.getParentId())
                    .isNull(Menu::getDeletedAt)
            );
            if (parent == null) {
                throw new BusinessException("父菜单不存在, parentId: " + request.getParentId());
            }
            menu.setParentId(request.getParentId());
        }

        // 更新可修改字段
        if (StringUtils.hasText(request.getMenuName())) {
            menu.setMenuName(request.getMenuName());
        }
        if (StringUtils.hasText(request.getPath())) {
            menu.setPath(request.getPath());
        }
        if (StringUtils.hasText(request.getComponent())) {
            menu.setComponent(request.getComponent());
        }
        if (request.getRedirect() != null) {
            menu.setRedirect(request.getRedirect());
        }
        if (request.getIcon() != null) {
            menu.setIcon(request.getIcon());
        }
        if (StringUtils.hasText(request.getMenuType())) {
            menu.setMenuType(request.getMenuType());
        }
        if (request.getSortOrder() != null) {
            menu.setSortOrder(request.getSortOrder());
        }
        if (request.getIsVisible() != null) {
            menu.setIsVisible(request.getIsVisible());
        }
        if (request.getIsCache() != null) {
            menu.setIsCache(request.getIsCache());
        }
        if (request.getIsExternal() != null) {
            menu.setIsExternal(request.getIsExternal());
        }
        if (request.getPermissionCode() != null) {
            menu.setPermissionCode(request.getPermissionCode());
        }
        if (StringUtils.hasText(request.getStatus())) {
            menu.setStatus(request.getStatus());
        }

        menuMapper.updateById(menu);
        log.info("更新菜单成功, id: {}", id);

        return MenuVO.from(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        Menu menu = menuMapper.selectOne(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getId, id)
                .isNull(Menu::getDeletedAt)
        );
        if (menu == null) {
            throw new BusinessException("菜单不存在, id: " + id);
        }

        // 检查是否有子菜单
        Long childCount = menuMapper.selectCount(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, id)
                .isNull(Menu::getDeletedAt)
        );
        if (childCount > 0) {
            throw new BusinessException("存在子菜单，无法删除, id: " + id);
        }

        // 软删除
        menu.setDeletedAt(LocalDateTime.now());
        menuMapper.updateById(menu);
        log.info("删除菜单成功, id: {}", id);
    }

    /**
     * 递归构建菜单树
     */
    private List<MenuTreeVO> buildMenuTree(List<Menu> allMenus, Long parentId) {
        List<MenuTreeVO> tree = new ArrayList<>();

        // 按父ID分组
        Map<Long, List<Menu>> menuMap = allMenus.stream()
                .collect(Collectors.groupingBy(
                    m -> m.getParentId() != null ? m.getParentId() : 0L
                ));

        // 查找当前层级的菜单
        Long searchParentId = parentId != null ? parentId : 0L;
        List<Menu> currentLevelMenus = menuMap.getOrDefault(searchParentId, new ArrayList<>());

        for (Menu menu : currentLevelMenus) {
            MenuTreeVO node = MenuTreeVO.from(menu);

            // 递归查找子菜单
            List<MenuTreeVO> children = buildMenuTree(allMenus, menu.getId());
            node.setChildren(children);

            tree.add(node);
        }

        return tree;
    }
}
