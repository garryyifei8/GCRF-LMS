package com.gcrf.library.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证过滤器
 * 负责验证JWT Token，拦截未授权的请求
 *
 * @author 王五
 * @date 2025-10-11
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.whitelist.paths}")
    private List<String> whitelistPaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Request path: {}", path);

        // 检查是否在白名单中
        if (isWhitelisted(path)) {
            log.debug("Path {} is whitelisted, skip authentication", path);
            return chain.filter(exchange);
        }

        // 获取Token
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("Token is missing for path: {}", path);
            return unauthorized(exchange.getResponse(), "Token is missing");
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid token for path: {}", path);
            return unauthorized(exchange.getResponse(), "Invalid or expired token");
        }

        // 从Token中获取用户信息并添加到请求头
        try {
            String userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            if (userId != null && username != null) {
                // 将用户信息添加到请求头，传递给下游服务
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                exchange = exchange.mutate().request(mutatedRequest).build();
                log.debug("User {} authenticated successfully", username);
            }
        } catch (Exception e) {
            log.error("Failed to extract user info from token: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "Failed to parse token");
        }

        return chain.filter(exchange);
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : whitelistPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerHttpRequest request) {
        // 从Header中获取Token
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从Query Parameter中获取Token（备用方案）
        List<String> tokenParams = request.getQueryParams().get("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            return tokenParams.get(0);
        }

        return null;
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.UNAUTHORIZED.value());
        result.put("message", message);
        result.put("data", null);

        try {
            byte[] bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 设置较高优先级，确保在其他过滤器之前执行
        return -100;
    }
}
