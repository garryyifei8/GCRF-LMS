package com.gcrf.library.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

/**
 * 创建部门请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Data
public class DepartmentCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "部门编码不能为空")
    @Size(max = 50, message = "部门编码长度不能超过50个字符")
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String deptName;

    private Long parentId;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}
