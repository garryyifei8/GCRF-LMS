package com.gcrf.gateway.filter;

import com.gcrf.library.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * <p>
 * 功能：
 * <ul>
 *   <li>检查请求路径是否在白名单中</li>
 *   <li>验证JWT token有效性</li>
 *   <li>提取用户信息并传递给下游服务</li>
 * </ul>
 *
 * @author GCRF Team
 * @date 2025-10-23
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 白名单路径 - 这些路径不需要JWT验证
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/health",
            "/actuator/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway处理请求: method={}, path={}", request.getMethod(), path);

        // 检查是否在白名单中
        if (isWhitelistPath(path)) {
            log.debug("白名单路径，跳过JWT验证: {}", path);
            return chain.filter(exchange);
        }

        // 获取Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("未提供有效的Authorization header: path={}", path);
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        // 提取token
        String token = authHeader.substring(7);

        try {
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT token验证失败: path={}", path);
                return unauthorized(exchange.getResponse(), "Invalid or expired token");
            }

            // 提取用户信息
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);

            log.debug("JWT验证成功: userId={}, username={}, path={}", userId, username, path);

            // 将用户信息添加到请求头中，传递给下游服务
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("JWT验证异常: path={}, error={}", path, e.getMessage());
            return unauthorized(exchange.getResponse(), "Token validation failed");
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelistPath(String path) {
        return WHITELIST_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回401 Unauthorized响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * 设置过滤器优先级
     * 数值越小，优先级越高
     */
    @Override
    public int getOrder() {
        return -100;  // 高优先级，在其他过滤器之前执行
    }
}
