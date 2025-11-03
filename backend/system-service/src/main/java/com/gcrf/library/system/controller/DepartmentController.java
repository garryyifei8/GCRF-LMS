package com.gcrf.library.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.system.dto.*;
import com.gcrf.library.system.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 部门管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Tag(name = "部门管理", description = "部门信息管理相关接口")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "创建部门")
    @PostMapping
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        return departmentService.createDepartment(request);
    }

    @Operation(summary = "更新部门")
    @PutMapping
    public DepartmentResponse updateDepartment(@Valid @RequestBody DepartmentUpdateRequest request) {
        return departmentService.updateDepartment(request);
    }

    @Operation(summary = "根据ID获取部门")
    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    @Operation(summary = "分页查询部门列表")
    @GetMapping
    public Page<DepartmentResponse> queryDepartments(DepartmentQueryRequest request) {
        return departmentService.queryDepartments(request);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
