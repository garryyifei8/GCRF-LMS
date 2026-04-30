package com.gcrf.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.system.entity.SystemMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统消息Mapper接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Mapper
public interface SystemMessageMapper extends BaseMapper<SystemMessage> {
}
