package com.gcrf.library.notification.controller;

import com.gcrf.library.notification.dto.request.NotificationPushRequest;
import com.gcrf.library.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket通知控制器
 *
 * 处理WebSocket连接、订阅和消息
 *
 * 客户端使用示例:
 * <pre>
 * // 1. 连接WebSocket
 * const socket = new SockJS('http://localhost:8086/ws/notifications');
 * const stompClient = Stomp.over(socket);
 *
 * // 2. 连接并订阅
 * stompClient.connect({}, function(frame) {
 *     // 订阅个人通知队列
 *     stompClient.subscribe('/user/queue/notifications', function(message) {
 *         const notification = JSON.parse(message.body);
 *         console.log('收到通知:', notification);
 *     });
 *
 *     // 订阅广播主题
 *     stompClient.subscribe('/topic/notifications', function(message) {
 *         const notification = JSON.parse(message.body);
 *         console.log('收到广播通知:', notification);
 *     });
 *
 *     // 发送心跳
 *     stompClient.send('/app/ping', {}, JSON.stringify({userId: 123}));
 * });
 * </pre>
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketNotificationController {

    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * 处理客户端订阅通知主题
     *
     * 客户端订阅: stompClient.subscribe('/app/notifications', callback)
     * 服务端响应订阅确认消息
     *
     * @param principal 当前用户身份
     * @return 订阅确认消息
     */
    @SubscribeMapping("/notifications")
    public Map<String, Object> handleSubscribe(Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("客户端订阅通知, userId: {}", userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "订阅成功");
        response.put("userId", userId);
        response.put("timestamp", LocalDateTime.now());
        response.put("onlineUsers", webSocketNotificationService.getOnlineUserCount());

        return response;
    }

    /**
     * 处理心跳消息
     *
     * 客户端发送: stompClient.send('/app/ping', {}, JSON.stringify({userId: 123}))
     * 服务端响应: 返回pong消息到 /topic/pong
     *
     * @param payload 心跳数据
     * @return 心跳响应
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing(@Payload Map<String, Object> payload) {
        log.debug("收到心跳消息: {}", payload);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", LocalDateTime.now());
        response.put("received", payload);

        return response;
    }

    /**
     * 处理客户端发送的通知推送请求
     *
     * 客户端发送: stompClient.send('/app/push', {}, JSON.stringify(request))
     *
     * @param request 推送请求
     * @param principal 当前用户身份
     */
    @MessageMapping("/push")
    public void handlePushNotification(
            @Payload NotificationPushRequest request,
            Principal principal
    ) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("收到通知推送请求, from: {}, targetType: {}", userId, request.getTargetType());

        // 异步推送通知
        webSocketNotificationService.pushAsync(request);
    }

    /**
     * REST接口: 获取在线用户统计
     *
     * GET /api/v1/ws/stats
     *
     * @return 统计信息
     */
    @GetMapping("/api/v1/ws/stats")
    @ResponseBody
    public Map<String, Object> getWebSocketStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineUsers", webSocketNotificationService.getOnlineUserCount());
        stats.put("timestamp", LocalDateTime.now());

        return stats;
    }

    /**
     * REST接口: 检查用户是否在线
     *
     * GET /api/v1/ws/online/{userId}
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    @GetMapping("/api/v1/ws/online/{userId}")
    @ResponseBody
    public Map<String, Object> checkUserOnline(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        boolean online = webSocketNotificationService.isUserOnline(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("online", online);
        result.put("timestamp", LocalDateTime.now());

        return result;
    }
}
