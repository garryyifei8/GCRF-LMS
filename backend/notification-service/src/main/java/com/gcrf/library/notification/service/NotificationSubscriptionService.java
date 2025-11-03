package com.gcrf.library.notification.service;

import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;

/**
 * 用户通知订阅服务接口
 */
public interface NotificationSubscriptionService {

    /**
     * 获取用户订阅配置
     */
    SubscriptionVO getUserSubscription(Long userId);

    /**
     * 更新用户订阅配置
     */
    SubscriptionVO updateSubscription(Long userId, SubscriptionUpdateRequest request);

    /**
     * 检查用户是否订阅某类型通知
     */
    boolean isSubscribed(Long userId, String notificationType);
}
