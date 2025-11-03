package com.gcrf.library.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
