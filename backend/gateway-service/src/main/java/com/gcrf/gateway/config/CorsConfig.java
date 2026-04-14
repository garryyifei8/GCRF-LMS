package com.gcrf.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS配置类
 *
 * 安全增强的跨域资源共享配置：
 * - 限制允许的域名（不使用通配符*）
 * - 限制允许的HTTP方法
 * - 限制允许的请求头
 * - 控制凭证传递
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    /**
     * 创建CORS过滤器
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("初始化CORS配置: enabled={}, allowedOrigins={}",
                corsProperties.isEnabled(),
                corsProperties.getAllowedOrigins());

        CorsConfiguration config = new CorsConfiguration();

        if (!corsProperties.isEnabled()) {
            // CORS禁用时，不添加任何配置
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            return new CorsWebFilter(source);
        }

        // 设置允许的域名
        if (corsProperties.getAllowedOrigins() != null && !corsProperties.getAllowedOrigins().isEmpty()) {
            corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
        }

        // 设置允许的域名模式
        if (corsProperties.getAllowedOriginPatterns() != null && !corsProperties.getAllowedOriginPatterns().isEmpty()) {
            corsProperties.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern);
        }

        // 设置允许的HTTP方法
        if (corsProperties.getAllowedMethods() != null && !corsProperties.getAllowedMethods().isEmpty()) {
            corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
        }

        // 设置允许的请求头
        if (corsProperties.getAllowedHeaders() != null && !corsProperties.getAllowedHeaders().isEmpty()) {
            corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
        }

        // 设置暴露的响应头
        if (corsProperties.getExposedHeaders() != null && !corsProperties.getExposedHeaders().isEmpty()) {
            corsProperties.getExposedHeaders().forEach(config::addExposedHeader);
        }

        // 设置是否允许凭证
        config.setAllowCredentials(corsProperties.isAllowCredentials());

        // 设置预检请求缓存时间
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS配置初始化完成: allowedMethods={}, allowCredentials={}, maxAge={}",
                corsProperties.getAllowedMethods(),
                corsProperties.isAllowCredentials(),
                corsProperties.getMaxAge());

        return new CorsWebFilter(source);
    }
}
