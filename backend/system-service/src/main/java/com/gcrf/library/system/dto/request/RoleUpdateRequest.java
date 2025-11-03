package com.gcrf.library.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新角色请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class RoleUpdateRequest {

    /**
     * 主键ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /**
     * 角色名称
     */
    @Size(max = 100, message = "角色名称长度不能超过100个字符")
    private String roleName;

    /**
     * 角色描述
     */
    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    private String roleDesc;

    /**
     * 数据范围: ALL-全部, DEPT-本部门, DEPT_AND_CHILD-本部门及子部门, CUSTOM-自定义
     */
    @Pattern(regexp = "^(ALL|DEPT|DEPT_AND_CHILD|CUSTOM)$", message = "数据范围必须为ALL、DEPT、DEPT_AND_CHILD或CUSTOM")
    private String dataScope;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态必须为ACTIVE或DISABLED")
    private String status;
}
