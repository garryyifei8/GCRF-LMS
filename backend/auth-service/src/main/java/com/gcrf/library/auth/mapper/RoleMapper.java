package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
        SELECT r.* FROM gcrf_region.auth_role r
          JOIN gcrf_region.auth_user_role ur ON ur.role_id = r.id
         WHERE ur.user_id = #{userId}
           AND (ur.expires_at IS NULL OR ur.expires_at > now())
        """)
    List<Role> findByUserId(Long userId);
}
