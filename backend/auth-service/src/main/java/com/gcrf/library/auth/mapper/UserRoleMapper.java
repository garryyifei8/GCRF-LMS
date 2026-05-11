package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select({"<script>",
        "SELECT * FROM gcrf_region.auth_user_role",
        " WHERE user_id = #{userId} AND role_id = #{roleId}",
        " AND <choose>",
        "   <when test='schoolId == null'>school_id IS NULL</when>",
        "   <otherwise>school_id = #{schoolId}</otherwise>",
        " </choose>",
        " LIMIT 1",
        "</script>"})
    UserRole findExact(@Param("userId") Long userId,
                       @Param("roleId") Long roleId,
                       @Param("schoolId") Long schoolId);

    @Update({"<script>",
        "DELETE FROM gcrf_region.auth_user_role",
        " WHERE user_id = #{userId} AND role_id = #{roleId}",
        " AND <choose>",
        "   <when test='schoolId == null'>school_id IS NULL</when>",
        "   <otherwise>school_id = #{schoolId}</otherwise>",
        " </choose>",
        "</script>"})
    int deleteByUserRoleSchool(@Param("userId") Long userId,
                               @Param("roleId") Long roleId,
                               @Param("schoolId") Long schoolId);
}
