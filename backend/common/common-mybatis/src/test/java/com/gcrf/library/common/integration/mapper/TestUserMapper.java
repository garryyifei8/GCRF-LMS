package com.gcrf.library.common.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.common.integration.entity.TestUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试用户Mapper
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@Mapper
public interface TestUserMapper extends BaseMapper<TestUser> {
}
