package com.gcrf.library.notification.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.NotificationMarkReadRequest;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.dto.response.UnreadCountVO;

import java.util.List;

/**
 * 站内通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知
     */
    NotificationVO sendNotification(NotificationSendRequest request);

    /**
     * 分页查询用户通知
     */
    PageResult<NotificationVO> queryNotifications(Long userId, NotificationQueryRequest request);

    /**
     * 标记已读
     */
    void markAsRead(Long userId, NotificationMarkReadRequest request);

    /**
     * 标记全部已读
     */
    void markAllAsRead(Long userId);

    /**
     * 获取未读数量
     */
    UnreadCountVO getUnreadCount(Long userId);

    /**
     * 根据ID获取通知详情
     */
    NotificationVO getNotificationById(Long userId, Long notificationId);

    /**
     * 删除通知(软删除)
     */
    void deleteNotification(Long userId, Long notificationId);

    /**
     * 批量删除通知
     */
    void batchDeleteNotifications(Long userId, List<Long> notificationIds);
}
