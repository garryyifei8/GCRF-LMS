package com.gcrf.library.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel限流配置
 * 配置限流、熔断的自定义响应
 *
 * @author 王五
 * @date 2025-10-11
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        // 自定义限流降级处理器
        GatewayCallbackManager.setBlockHandler(new CustomBlockRequestHandler());
        log.info("Sentinel gateway callback initialized");
    }

    /**
     * 自定义限流降级处理器
     */
    private static class CustomBlockRequestHandler implements BlockRequestHandler {

        @Override
        public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
            log.warn("Request blocked by Sentinel: {} - {}",
                    exchange.getRequest().getPath(), ex.getMessage());

            Map<String, Object> result = new HashMap<>();
            result.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
            result.put("message", "系统繁忙，请稍后再试");
            result.put("data", null);

            return ServerResponse
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(JSON.toJSONString(result));
        }
    }
}
