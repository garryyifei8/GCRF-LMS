package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.system.dto.*;
import com.gcrf.library.system.entity.Department;
import com.gcrf.library.system.mapper.DepartmentMapper;
import com.gcrf.library.system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 部门服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        log.info("创建部门: {}", request.getDeptName());

        // 检查部门编码是否已存在
        Long count = departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptCode, request.getDeptCode())
        );
        if (count > 0) {
            throw new RuntimeException("部门编码已存在: " + request.getDeptCode());
        }

        Department department = new Department();
        BeanUtils.copyProperties(request, department);
        department.setStatus("ACTIVE");
        department.setSortOrder(0);

        // 计算部门层级和路径
        if (request.getParentId() != null) {
            Department parent = departmentMapper.selectById(request.getParentId());
            if (parent != null) {
                department.setDeptLevel(parent.getDeptLevel() + 1);
            } else {
                department.setDeptLevel(1);
            }
        } else {
            department.setDeptLevel(1);
        }

        departmentMapper.insert(department);

        // 更新部门路径
        department.setDeptPath(buildDeptPath(department));
        departmentMapper.updateById(department);

        log.info("部门创建成功: id={}", department.getId());
        return convertToResponse(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentResponse updateDepartment(DepartmentUpdateRequest request) {
        log.info("更新部门: id={}", request.getId());

        Department department = departmentMapper.selectById(request.getId());
        if (department == null) {
            throw new RuntimeException("部门不存在: " + request.getId());
        }

        if (StringUtils.hasText(request.getDeptName())) {
            department.setDeptName(request.getDeptName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            department.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            department.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getDescription())) {
            department.setDescription(request.getDescription());
        }

        departmentMapper.updateById(department);
        log.info("部门更新成功: id={}", request.getId());

        return convertToResponse(department);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new RuntimeException("部门不存在: " + id);
        }
        return convertToResponse(department);
    }

    @Override
    public Page<DepartmentResponse> queryDepartments(DepartmentQueryRequest request) {
        Page<Department> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getDeptCode()), Department::getDeptCode, request.getDeptCode())
               .like(StringUtils.hasText(request.getDeptName()), Department::getDeptName, request.getDeptName())
               .eq(StringUtils.hasText(request.getStatus()), Department::getStatus, request.getStatus())
               .orderByAsc(Department::getSortOrder)
               .orderByAsc(Department::getId);

        Page<Department> departmentPage = departmentMapper.selectPage(page, wrapper);

        Page<DepartmentResponse> responsePage = new Page<>();
        BeanUtils.copyProperties(departmentPage, responsePage, "records");
        responsePage.setRecords(
            departmentPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList()
        );

        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        log.info("删除部门: id={}", id);

        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new RuntimeException("部门不存在: " + id);
        }

        // 检查是否有子部门
        Long childCount = departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id)
        );
        if (childCount > 0) {
            throw new RuntimeException("该部门下有子部门，无法删除");
        }

        departmentMapper.deleteById(id);
        log.info("部门删除成功: id={}", id);
    }

    private String buildDeptPath(Department department) {
        if (department.getParentId() == null) {
            return "/" + department.getId();
        }

        Department parent = departmentMapper.selectById(department.getParentId());
        if (parent != null && StringUtils.hasText(parent.getDeptPath())) {
            return parent.getDeptPath() + "/" + department.getId();
        }

        return "/" + department.getId();
    }

    private DepartmentResponse convertToResponse(Department department) {
        DepartmentResponse response = new DepartmentResponse();
        BeanUtils.copyProperties(department, response);
        return response;
    }
}
