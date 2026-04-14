package com.gcrf.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置属性
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class RateLimitProperties {

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 默认匿名用户限流配置（每分钟请求数）
     */
    private int anonymousRequestsPerMinute = 100;

    /**
     * 默认认证用户限流配置（每分钟请求数）
     */
    private int authenticatedRequestsPerMinute = 1000;

    /**
     * 路径特定限流配置
     * key: 路径模式 (支持Ant风格)
     * value: 每分钟请求数限制
     */
    private Map<String, PathRateLimit> paths = new HashMap<>();

    /**
     * Redis键前缀
     */
    private String redisKeyPrefix = "gateway:ratelimit:";

    /**
     * 限流窗口大小（秒）
     */
    private int windowSizeSeconds = 60;

    /**
     * 路径限流配置
     */
    @Data
    public static class PathRateLimit {
        /**
         * 每分钟请求数限制
         */
        private int requestsPerMinute;

        /**
         * 限流类型: IP, USER, GLOBAL
         */
        private RateLimitType type = RateLimitType.IP;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 限流类型枚举
     */
    public enum RateLimitType {
        /**
         * 按IP限流
         */
        IP,
        /**
         * 按用户ID限流
         */
        USER,
        /**
         * 全局限流
         */
        GLOBAL
    }
}
