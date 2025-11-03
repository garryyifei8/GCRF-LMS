package com.gcrf.library.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内信Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
