package com.gcrf.library.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理网关层面的异常
 *
 * @author 王五
 * @date 2025-10-11
 */
@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 根据异常类型设置HTTP状态码
        HttpStatus httpStatus = determineHttpStatus(ex);
        response.setStatusCode(httpStatus);

        // 构建响应体
        Map<String, Object> result = new HashMap<>();
        result.put("code", httpStatus.value());
        result.put("message", extractMessage(ex));
        result.put("data", null);
        result.put("path", exchange.getRequest().getPath().value());

        // 记录异常日志
        log.error("Gateway exception occurred: {} - {}",
                exchange.getRequest().getPath(),
                ex.getMessage(),
                ex);

        try {
            byte[] bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response: {}", e.getMessage());
            return response.setComplete();
        }
    }

    /**
     * 根据异常类型确定HTTP状态码
     */
    private HttpStatus determineHttpStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            return ((ResponseStatusException) ex).getStatus();
        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            return HttpStatus.BAD_REQUEST;
        } else if (ex instanceof java.net.ConnectException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 提取异常消息
     */
    private String extractMessage(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            return rse.getReason() != null ? rse.getReason() : rse.getStatus().getReasonPhrase();
        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            return "请求参数错误";
        } else if (ex instanceof java.net.ConnectException) {
            return "服务暂时不可用，请稍后重试";
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            return "请求超时，请稍后重试";
        } else {
            return "系统内部错误: " + ex.getMessage();
        }
    }
}
