package com.gcrf.library.system.service;

import com.gcrf.library.system.dto.request.MenuCreateRequest;
import com.gcrf.library.system.dto.request.MenuUpdateRequest;
import com.gcrf.library.system.dto.response.MenuTreeVO;
import com.gcrf.library.system.dto.response.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface MenuService {

    /**
     * 获取菜单树
     */
    List<MenuTreeVO> getMenuTree();

    /**
     * 根据用户ID获取用户菜单树
     */
    List<MenuTreeVO> getUserMenus(Long userId);

    /**
     * 根据ID获取菜单详情
     */
    MenuVO getMenuById(Long id);

    /**
     * 创建菜单
     */
    MenuVO createMenu(MenuCreateRequest request);

    /**
     * 更新菜单
     */
    MenuVO updateMenu(Long id, MenuUpdateRequest request);

    /**
     * 删除菜单（软删除）
     */
    void deleteMenu(Long id);
}
