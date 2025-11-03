package com.gcrf.library.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

/**
 * 更新部门请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Data
public class DepartmentUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "部门ID不能为空")
    private Long id;

    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String deptName;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}
