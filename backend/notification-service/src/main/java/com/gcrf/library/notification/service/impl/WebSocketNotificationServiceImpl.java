package com.gcrf.library.notification.service.impl;

import com.gcrf.library.notification.dto.request.NotificationPushRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket通知推送服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    /**
     * 在线用户缓存: userId -> sessionId
     * 用于快速判断用户是否在线
     */
    private final Map<Long, String> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public void pushToUser(Long userId, NotificationVO notification) {
        try {
            // 检查用户是否在线
            if (!isUserOnline(userId)) {
                log.warn("用户不在线,无法推送WebSocket通知, userId: {}", userId);
                return;
            }

            // 推送到用户的个人队列: /user/{userId}/queue/notifications
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    destination,
                    notification
            );

            log.info("WebSocket通知已推送到用户, userId: {}, notificationId: {}",
                    userId, notification.getId());

        } catch (Exception e) {
            log.error("推送WebSocket通知失败, userId: {}, error: {}",
                    userId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastToAll(NotificationVO notification) {
        try {
            // 广播到所有订阅了 /topic/notifications 的客户端
            String destination = "/topic/notifications";
            messagingTemplate.convertAndSend(destination, notification);

            log.info("WebSocket通知已广播, notificationId: {}, onlineUsers: {}",
                    notification.getId(), getOnlineUserCount());

        } catch (Exception e) {
            log.error("广播WebSocket通知失败, error: {}", e.getMessage(), e);
        }
    }

    @Override
    public void pushToUsers(List<Long> userIds, NotificationVO notification) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("用户ID列表为空,无法推送通知");
            return;
        }

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                pushToUser(userId, notification);
                successCount++;
            } catch (Exception e) {
                log.error("推送通知到用户失败, userId: {}, error: {}",
                        userId, e.getMessage());
            }
        }

        log.info("批量推送WebSocket通知完成, total: {}, success: {}, notificationId: {}",
                userIds.size(), successCount, notification.getId());
    }

    @Override
    public void pushToTopic(String topic, NotificationVO notification) {
        try {
            // 推送到指定主题: /topic/{topic}
            String destination = "/topic/" + topic;
            messagingTemplate.convertAndSend(destination, notification);

            log.info("WebSocket通知已推送到主题, topic: {}, notificationId: {}",
                    topic, notification.getId());

        } catch (Exception e) {
            log.error("推送通知到主题失败, topic: {}, error: {}",
                    topic, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void pushAsync(NotificationPushRequest request) {
        try {
            log.info("开始异步推送WebSocket通知, targetType: {}, targets: {}",
                    request.getTargetType(), request.getTargetIds());

            // 根据目标类型推送
            switch (request.getTargetType()) {
                case "USER" -> {
                    // 推送到指定用户
                    if (request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
                        for (Long userId : request.getTargetIds()) {
                            NotificationVO notification = buildNotificationVO(request);
                            pushToUser(userId, notification);
                        }
                    }
                }
                case "ALL" -> {
                    // 广播到所有用户
                    NotificationVO notification = buildNotificationVO(request);
                    broadcastToAll(notification);
                }
                case "TOPIC" -> {
                    // 推送到主题
                    if (request.getTopic() != null) {
                        NotificationVO notification = buildNotificationVO(request);
                        pushToTopic(request.getTopic(), notification);
                    }
                }
                default -> log.warn("不支持的目标类型: {}", request.getTargetType());
            }

            log.info("异步推送WebSocket通知完成");

        } catch (Exception e) {
            log.error("异步推送WebSocket通知失败, error: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }

        // 方式1: 检查缓存(快速)
        if (onlineUsers.containsKey(userId)) {
            return true;
        }

        // 方式2: 检查SimpUserRegistry(准确)
        // 用户连接时的 principal name 应该是 userId
        return userRegistry.getUser(userId.toString()) != null;
    }

    @Override
    public long getOnlineUserCount() {
        // 从SimpUserRegistry获取在线用户数
        return userRegistry.getUserCount();
    }

    /**
     * 用户连接时调用
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void userConnected(Long userId, String sessionId) {
        onlineUsers.put(userId, sessionId);
        log.info("用户WebSocket连接成功, userId: {}, sessionId: {}, total: {}",
                userId, sessionId, onlineUsers.size());
    }

    /**
     * 用户断开连接时调用
     *
     * @param userId 用户ID
     */
    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
        log.info("用户WebSocket断开连接, userId: {}, remaining: {}",
                userId, onlineUsers.size());
    }

    /**
     * 构建通知VO对象
     *
     * @param request 推送请求
     * @return NotificationVO
     */
    private NotificationVO buildNotificationVO(NotificationPushRequest request) {
        NotificationVO vo = new NotificationVO();
        vo.setTitle(request.getTitle());
        vo.setContent(request.getContent());
        vo.setNotificationType(request.getNotificationType());
        vo.setPriority(request.getPriority());
        vo.setCreatedAt(java.time.LocalDateTime.now());
        return vo;
    }
}
