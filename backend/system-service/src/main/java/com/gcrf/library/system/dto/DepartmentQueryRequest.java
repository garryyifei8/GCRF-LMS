package com.gcrf.library.system.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 查询部门请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Data
public class DepartmentQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deptCode;
    private String deptName;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
