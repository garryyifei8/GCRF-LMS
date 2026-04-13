package com.gcrf.library.common.feign.config;

import feign.Logger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feign配置属性
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Data
@ConfigurationProperties(prefix = "library.feign")
public class FeignProperties {

    /**
     * 是否启用Feign配置
     */
    private boolean enabled = true;

    /**
     * 日志级别
     */
    private Logger.Level loggerLevel = Logger.Level.BASIC;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeout = 10000;

    /**
     * 是否传递认证头
     */
    private boolean forwardAuth = true;

    /**
     * 是否传递追踪ID
     */
    private boolean forwardTraceId = true;

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * Sentinel配置
     */
    private Sentinel sentinel = new Sentinel();

    /**
     * 重试配置类
     */
    @Data
    public static class Retry {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long period = 100;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxPeriod = 1000;
    }

    /**
     * Sentinel配置类
     */
    @Data
    public static class Sentinel {
        /**
         * 是否启用Sentinel
         */
        private boolean enabled = true;

        /**
         * 慢调用阈值（毫秒）
         */
        private long slowCallThreshold = 3000;

        /**
         * 慢调用比例阈值
         */
        private double slowCallRatioThreshold = 0.5;

        /**
         * 异常比例阈值
         */
        private double errorRatioThreshold = 0.5;

        /**
         * 熔断时长（秒）
         */
        private int circuitBreakerTimeout = 10;

        /**
         * 最小请求数
         */
        private int minRequestAmount = 5;

        /**
         * 统计时长（秒）
         */
        private int statIntervalMs = 10000;
    }
}
