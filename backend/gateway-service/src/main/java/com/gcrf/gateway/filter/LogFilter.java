package com.gcrf.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 日志过滤器
 * <p>
 * 功能：
 * <ul>
 *   <li>记录请求的基本信息（method, path, params）</li>
 *   <li>记录响应的状态码</li>
 *   <li>记录请求处理耗时</li>
 * </ul>
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@Slf4j
@Component
public class LogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 记录请求开始时间
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String remoteAddress = request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        log.info("Gateway Request: method={}, path={}, query={}, remoteAddress={}",
                method, path, query, remoteAddress);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Long startTime = exchange.getAttribute(START_TIME);
            long executionTime = startTime != null ?
                    System.currentTimeMillis() - startTime : 0;

            log.info("Gateway Response: method={}, path={}, status={}, executionTime={}ms",
                    method, path, response.getStatusCode(), executionTime);
        }));
    }

    /**
     * 设置过滤器优先级
     * 数值越小，优先级越高
     * LogFilter应该在AuthenticationFilter之后执行
     */
    @Override
    public int getOrder() {
        return -50;  // 在AuthenticationFilter(-100)之后执行
    }
}
