package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
        SELECT DISTINCT p.code
          FROM gcrf_region.auth_permission p
          JOIN gcrf_region.auth_role_permission rp ON rp.permission_id = p.id
          JOIN gcrf_region.auth_user_role ur ON ur.role_id = rp.role_id
         WHERE ur.user_id = #{userId}
           AND (ur.expires_at IS NULL OR ur.expires_at > now())
        """)
    List<String> findCodesByUserId(Long userId);

    @Select("""
        SELECT p.* FROM gcrf_region.auth_permission p
          JOIN gcrf_region.auth_role_permission rp ON rp.permission_id = p.id
         WHERE rp.role_id = #{roleId}
         ORDER BY p.sort_order
        """)
    List<Permission> findByRoleId(Long roleId);
}
