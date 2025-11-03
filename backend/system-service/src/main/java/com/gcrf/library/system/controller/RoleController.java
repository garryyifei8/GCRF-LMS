package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.AssignPermissionsRequest;
import com.gcrf.library.system.dto.request.RoleCreateRequest;
import com.gcrf.library.system.dto.request.RoleQueryRequest;
import com.gcrf.library.system.dto.request.RoleUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;
import com.gcrf.library.system.dto.response.RoleDetailVO;
import com.gcrf.library.system.dto.response.RoleVO;
import com.gcrf.library.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色CRUD及权限分配")
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色
     */
    @GetMapping
    @Operation(summary = "分页查询角色", description = "支持角色编码、名称、状态等条件查询")
    public Result<PageResult<RoleVO>> queryRoles(@Valid RoleQueryRequest request) {
        log.info("分页查询角色, request: {}", request);
        PageResult<RoleVO> result = roleService.queryRoles(request);
        return Result.success(result);
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息及权限列表")
    public Result<RoleDetailVO> getRoleById(@PathVariable Long id) {
        log.info("获取角色详情, id: {}", id);
        RoleDetailVO result = roleService.getRoleById(id);
        return Result.success(result);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色")
    public Result<RoleDetailVO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("创建角色, request: {}", request);
        RoleDetailVO result = roleService.createRole(request);
        return Result.success(result);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新角色信息")
    public Result<RoleDetailVO> updateRole(@PathVariable Long id,
                                           @Valid @RequestBody RoleUpdateRequest request) {
        log.info("更新角色, id: {}, request: {}", id, request);
        RoleDetailVO result = roleService.updateRole(id, request);
        return Result.success(result);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "软删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        log.info("删除角色, id: {}", id);
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/{id}/permissions")
    @Operation(summary = "分配权限", description = "为角色分配权限列表")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                         @Valid @RequestBody AssignPermissionsRequest request) {
        log.info("为角色分配权限, roleId: {}, permissionIds: {}", id, request.getPermissionIds());
        request.setRoleId(id);
        roleService.assignPermissions(id, request.getPermissionIds());
        return Result.success();
    }

    /**
     * 获取角色的权限列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色权限", description = "获取角色拥有的权限列表")
    public Result<List<PermissionVO>> getRolePermissions(@PathVariable Long id) {
        log.info("获取角色权限, roleId: {}", id);
        List<PermissionVO> result = roleService.getRolePermissions(id);
        return Result.success(result);
    }
}
