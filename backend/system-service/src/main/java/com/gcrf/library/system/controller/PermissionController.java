package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.PermissionCreateRequest;
import com.gcrf.library.system.dto.request.PermissionQueryRequest;
import com.gcrf.library.system.dto.request.PermissionUpdateRequest;
import com.gcrf.library.system.dto.response.PermissionVO;
import com.gcrf.library.system.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限CRUD")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 分页查询权限
     */
    @GetMapping
    @Operation(summary = "分页查询权限", description = "支持权限编码、名称、类型等条件查询")
    public Result<PageResult<PermissionVO>> queryPermissions(@Valid PermissionQueryRequest request) {
        log.info("分页查询权限, request: {}", request);
        PageResult<PermissionVO> result = permissionService.queryPermissions(request);
        return Result.success(result);
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有权限", description = "获取所有权限列表，用于下拉框等场景")
    public Result<List<PermissionVO>> listAllPermissions() {
        log.info("获取所有权限列表");
        List<PermissionVO> result = permissionService.listAllPermissions();
        return Result.success(result);
    }

    /**
     * 根据ID获取权限详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取权限详情", description = "根据ID获取权限详细信息")
    public Result<PermissionVO> getPermissionById(@PathVariable Long id) {
        log.info("获取权限详情, id: {}", id);
        PermissionVO result = permissionService.getPermissionById(id);
        return Result.success(result);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(summary = "创建权限", description = "创建新权限")
    public Result<PermissionVO> createPermission(@Valid @RequestBody PermissionCreateRequest request) {
        log.info("创建权限, request: {}", request);
        PermissionVO result = permissionService.createPermission(request);
        return Result.success(result);
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新权限", description = "更新权限信息")
    public Result<PermissionVO> updatePermission(@PathVariable Long id,
                                                 @Valid @RequestBody PermissionUpdateRequest request) {
        log.info("更新权限, id: {}, request: {}", id, request);
        PermissionVO result = permissionService.updatePermission(id, request);
        return Result.success(result);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限", description = "软删除权限")
    public Result<Void> deletePermission(@PathVariable Long id) {
        log.info("删除权限, id: {}", id);
        permissionService.deletePermission(id);
        return Result.success();
    }
}
