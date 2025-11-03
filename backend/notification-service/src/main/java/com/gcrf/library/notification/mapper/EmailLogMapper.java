package com.gcrf.library.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.notification.entity.EmailLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件记录Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Mapper
public interface EmailLogMapper extends BaseMapper<EmailLog> {
}
