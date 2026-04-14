package com.gcrf.gateway.filter;

import com.gcrf.gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * API限流过滤器
 *
 * 功能：
 * - 按IP限流（匿名用户）
 * - 按用户ID限流（认证用户）
 * - 支持路径特定限流规则
 * - 返回标准限流响应头
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimiterService rateLimiterService;

    /**
     * X-Forwarded-For请求头名称
     */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * X-Real-IP请求头名称
     */
    private static final String X_REAL_IP = "X-Real-IP";

    /**
     * 用户ID请求头名称（由AuthenticationFilter设置）
     */
    private static final String X_USER_ID = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);
        Long userId = getUserId(request);

        log.debug("限流检查: path={}, clientIp={}, userId={}", path, clientIp, userId);

        // 检查是否允许请求
        if (!rateLimiterService.isAllowed(path, clientIp, userId)) {
            log.warn("请求被限流: path={}, clientIp={}, userId={}", path, clientIp, userId);
            return tooManyRequests(exchange.getResponse());
        }

        // 添加限流响应头
        long remainingRequests = rateLimiterService.getRemainingRequests(path, clientIp, userId);
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remainingRequests));

        return chain.filter(exchange);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先从X-Forwarded-For获取
        String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For可能包含多个IP，取第一个
            String ip = xForwardedFor.split(",")[0].trim();
            if (isValidIp(ip)) {
                return ip;
            }
        }

        // 其次从X-Real-IP获取
        String xRealIp = request.getHeaders().getFirst(X_REAL_IP);
        if (xRealIp != null && !xRealIp.isEmpty() && isValidIp(xRealIp)) {
            return xRealIp;
        }

        // 最后从连接地址获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }

        return "unknown";
    }

    /**
     * 验证IP地址格式
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // 简单验证：不是unknown且不是内部地址标识
        return !ip.equalsIgnoreCase("unknown") && !ip.equalsIgnoreCase("null");
    }

    /**
     * 从请求头获取用户ID
     */
    private Long getUserId(ServerHttpRequest request) {
        String userIdStr = request.getHeaders().getFirst(X_USER_ID);
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", userIdStr);
            }
        }
        return null;
    }

    /**
     * 返回429 Too Many Requests响应
     */
    private Mono<Void> tooManyRequests(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.getHeaders().add("Retry-After", "60"); // 建议60秒后重试

        String body = """
                {"code":429,"message":"请求过于频繁，请稍后重试","data":null}
                """;
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 在AuthenticationFilter之后执行，以便获取用户ID
        return -50;
    }
}
