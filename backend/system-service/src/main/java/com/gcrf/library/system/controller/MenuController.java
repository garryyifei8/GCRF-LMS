package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.MenuCreateRequest;
import com.gcrf.library.system.dto.request.MenuUpdateRequest;
import com.gcrf.library.system.dto.response.MenuTreeVO;
import com.gcrf.library.system.dto.response.MenuVO;
import com.gcrf.library.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/menus")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "菜单树及CRUD操作")
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取完整菜单树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取完整的菜单树结构")
    public Result<List<MenuTreeVO>> getMenuTree() {
        log.info("获取菜单树");
        List<MenuTreeVO> result = menuService.getMenuTree();
        return Result.success(result);
    }

    /**
     * 获取用户菜单树
     */
    @GetMapping("/user-menus")
    @Operation(summary = "获取用户菜单", description = "根据用户ID获取用户有权限的菜单树")
    public Result<List<MenuTreeVO>> getUserMenus(@RequestParam Long userId) {
        log.info("获取用户菜单, userId: {}", userId);
        List<MenuTreeVO> result = menuService.getUserMenus(userId);
        return Result.success(result);
    }

    /**
     * 根据ID获取菜单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取菜单详情", description = "根据ID获取菜单详细信息")
    public Result<MenuVO> getMenuById(@PathVariable Long id) {
        log.info("获取菜单详情, id: {}", id);
        MenuVO result = menuService.getMenuById(id);
        return Result.success(result);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建新菜单")
    public Result<MenuVO> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        log.info("创建菜单, request: {}", request);
        MenuVO result = menuService.createMenu(request);
        return Result.success(result);
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新菜单", description = "更新菜单信息")
    public Result<MenuVO> updateMenu(@PathVariable Long id,
                                     @Valid @RequestBody MenuUpdateRequest request) {
        log.info("更新菜单, id: {}, request: {}", id, request);
        MenuVO result = menuService.updateMenu(id, request);
        return Result.success(result);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "软删除菜单（需检查子菜单）")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        log.info("删除菜单, id: {}", id);
        menuService.deleteMenu(id);
        return Result.success();
    }
}
