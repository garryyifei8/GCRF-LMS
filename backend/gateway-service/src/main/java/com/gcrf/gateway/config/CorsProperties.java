package com.gcrf.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CORS配置属性
 *
 * 安全增强的CORS配置，限制跨域访问
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.cors")
public class CorsProperties {

    /**
     * 是否启用CORS
     */
    private boolean enabled = true;

    /**
     * 允许的域名列表
     * 生产环境应明确指定允许的域名
     */
    private List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3011",     // 本地开发前端
            "http://localhost:5173",     // Vite开发服务器
            "http://127.0.0.1:3011",
            "http://127.0.0.1:5173"
    );

    /**
     * 允许的域名模式（支持通配符）
     * 注意：使用通配符模式时无法设置allowCredentials为true
     */
    private List<String> allowedOriginPatterns = Arrays.asList();

    /**
     * 允许的HTTP方法
     */
    private List<String> allowedMethods = Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    );

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-User-Id",
            "X-Username",
            "X-Trace-Id"
    );

    /**
     * 暴露给客户端的响应头
     */
    private List<String> exposedHeaders = Arrays.asList(
            "X-RateLimit-Remaining",
            "X-RateLimit-Limit",
            "X-RateLimit-Reset",
            "X-Request-Id",
            "X-Trace-Id"
    );

    /**
     * 是否允许发送凭证（cookies、authorization headers等）
     */
    private boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒）
     */
    private long maxAge = 3600L;
}
