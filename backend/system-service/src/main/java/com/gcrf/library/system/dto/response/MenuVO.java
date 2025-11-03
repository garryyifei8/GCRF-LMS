package com.gcrf.library.system.dto.response;

import com.gcrf.library.system.entity.Menu;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class MenuVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 重定向路径
     */
    private String redirect;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单类型: DIR-目录, MENU-菜单, BUTTON-按钮
     */
    private String menuType;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 是否可见
     */
    private Boolean isVisible;

    /**
     * 是否缓存
     */
    private Boolean isCache;

    /**
     * 是否外链
     */
    private Boolean isExternal;

    /**
     * 权限标识
     */
    private String permissionCode;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    private String status;

    /**
     * 子菜单列表（用于树形结构）
     */
    private List<MenuVO> children;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static MenuVO from(Menu menu) {
        if (menu == null) {
            return null;
        }
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setParentId(menu.getParentId());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setRedirect(menu.getRedirect());
        vo.setIcon(menu.getIcon());
        vo.setMenuType(menu.getMenuType());
        vo.setSortOrder(menu.getSortOrder());
        vo.setIsVisible(menu.getIsVisible());
        vo.setIsCache(menu.getIsCache());
        vo.setIsExternal(menu.getIsExternal());
        vo.setPermissionCode(menu.getPermissionCode());
        vo.setStatus(menu.getStatus());
        vo.setCreatedAt(menu.getCreatedAt());
        vo.setUpdatedAt(menu.getUpdatedAt());
        return vo;
    }
}
