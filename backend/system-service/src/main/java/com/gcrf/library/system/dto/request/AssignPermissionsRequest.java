package com.gcrf.library.system.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配权限请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class AssignPermissionsRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 权限ID列表
     */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
