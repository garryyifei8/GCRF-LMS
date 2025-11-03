package com.gcrf.library.notification.service;

import com.gcrf.library.notification.dto.request.NotificationPushRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;

/**
 * WebSocket通知推送服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface WebSocketNotificationService {

    /**
     * 向指定用户推送通知
     *
     * @param userId 用户ID
     * @param notification 通知内容
     */
    void pushToUser(Long userId, NotificationVO notification);

    /**
     * 向所有在线用户广播通知
     *
     * @param notification 通知内容
     */
    void broadcastToAll(NotificationVO notification);

    /**
     * 向指定用户组推送通知
     *
     * @param userIds 用户ID列表
     * @param notification 通知内容
     */
    void pushToUsers(java.util.List<Long> userIds, NotificationVO notification);

    /**
     * 根据主题推送通知
     *
     * @param topic 主题名称
     * @param notification 通知内容
     */
    void pushToTopic(String topic, NotificationVO notification);

    /**
     * 异步推送通知到用户
     *
     * @param request 推送请求
     */
    void pushAsync(NotificationPushRequest request);

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数
     */
    long getOnlineUserCount();
}
