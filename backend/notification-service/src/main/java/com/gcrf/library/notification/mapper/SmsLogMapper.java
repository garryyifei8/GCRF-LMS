package com.gcrf.library.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.notification.entity.SmsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信记录Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Mapper
public interface SmsLogMapper extends BaseMapper<SmsLog> {
}
