package com.gcrf.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.system.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门 Mapper
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
