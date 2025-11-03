package com.gcrf.library.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.notification.entity.NotificationSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知订阅配置Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Mapper
public interface NotificationSubscriptionMapper extends BaseMapper<NotificationSubscription> {
}
