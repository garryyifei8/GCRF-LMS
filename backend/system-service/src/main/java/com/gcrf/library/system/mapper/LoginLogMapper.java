package com.gcrf.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.system.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
