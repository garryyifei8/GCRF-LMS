package com.gcrf.library.system.dto.response;

import com.gcrf.library.system.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class RoleVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 数据范围: ALL-全部, DEPT-本部门, DEPT_AND_CHILD-本部门及子部门, CUSTOM-自定义
     */
    private String dataScope;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态: ACTIVE-正常, DISABLED-停用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static RoleVO from(Role role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleDesc(role.getRoleDesc());
        vo.setDataScope(role.getDataScope());
        vo.setSortOrder(role.getSortOrder());
        vo.setStatus(role.getStatus());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        return vo;
    }
}
