package com.gcrf.library.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.system.dto.*;

/**
 * 部门服务接口
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentCreateRequest request);

    DepartmentResponse updateDepartment(DepartmentUpdateRequest request);

    DepartmentResponse getDepartmentById(Long id);

    Page<DepartmentResponse> queryDepartments(DepartmentQueryRequest request);

    void deleteDepartment(Long id);
}
