package com.gcrf.library.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 *
 * 配置STOMP协议WebSocket通信:
 * - 消息代理: 使用内存消息代理
 * - 端点: /ws/notifications
 * - 应用目的地前缀: /app
 * - 用户目的地前缀: /user
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     *
     * @param registry 消息代理注册器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单内存消息代理,用于向客户端发送消息
        // 客户端订阅目的地: /topic/xxx (广播), /queue/xxx (点对点)
        registry.enableSimpleBroker("/topic", "/queue");

        // 设置应用目的地前缀,客户端发送消息时的目的地前缀
        // 客户端发送: /app/xxx -> 路由到 @MessageMapping("/xxx")
        registry.setApplicationDestinationPrefixes("/app");

        // 设置用户目的地前缀,用于点对点消息
        // 服务端发送到: /user/{username}/queue/xxx
        // 客户端订阅: /user/queue/xxx (自动添加当前用户名)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     *
     * @param registry STOMP端点注册器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点: /ws/notifications
        // 客户端连接: ws://localhost:8086/ws/notifications
        registry.addEndpoint("/ws/notifications")
                // 允许所有源跨域访问(生产环境应限制具体域名)
                .setAllowedOriginPatterns("*")
                // 启用SockJS回退选项(当WebSocket不可用时)
                .withSockJS();
    }
}
