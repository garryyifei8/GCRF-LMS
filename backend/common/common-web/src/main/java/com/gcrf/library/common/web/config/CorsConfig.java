package com.gcrf.library.common.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS跨域配置类
 * <p>
 * 配置跨域资源共享(CORS)策略，支持前后端分离开发:
 * <ul>
 *   <li>允许指定来源的跨域请求</li>
 *   <li>支持携带认证信息(Cookies, Authorization头)</li>
 *   <li>允许常用HTTP方法</li>
 *   <li>允许常用请求头</li>
 *   <li>设置预检请求缓存时间</li>
 * </ul>
 * <p>
 * 配置项(application.yml):
 * <pre>
 * library:
 *   web:
 *     cors:
 *       enabled: true  # 是否启用CORS
 *       allowed-origins: http://localhost:3000,http://localhost:8080  # 允许的来源
 *       allowed-methods: GET,POST,PUT,DELETE,OPTIONS  # 允许的HTTP方法
 *       max-age: 3600  # 预检请求缓存时间(秒)
 * </pre>
 *
 * @author 张三
 * @date 2025-10-23
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${library.web.cors.enabled:true}")
    private Boolean corsEnabled;

    @Value("${library.web.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${library.web.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${library.web.cors.max-age:3600}")
    private Long maxAge;

    /**
     * 配置CORS过滤器
     * <p>
     * 根据配置创建CORS过滤器:
     * <ul>
     *   <li>如果corsEnabled=false，返回默认配置(不启用CORS)</li>
     *   <li>如果corsEnabled=true，根据配置项设置CORS策略</li>
     * </ul>
     *
     * @return CORS过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        if (!corsEnabled) {
            log.warn("CORS未启用，跨域请求将被拒绝");
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        }

        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源
        if ("*".equals(allowedOrigins)) {
            config.addAllowedOriginPattern("*");
            log.warn("CORS配置允许所有来源(*)，生产环境建议配置具体域名");
        } else {
            String[] origins = allowedOrigins.split(",");
            Arrays.stream(origins)
                    .map(String::trim)
                    .forEach(config::addAllowedOrigin);
            log.info("CORS配置允许的来源: {}", Arrays.toString(origins));
        }

        // 允许的HTTP方法
        String[] methods = allowedMethods.split(",");
        Arrays.stream(methods)
                .map(String::trim)
                .forEach(config::addAllowedMethod);

        // 允许的请求头
        config.addAllowedHeader("*");

        // 允许携带认证信息(Cookies, Authorization头等)
        config.setAllowCredentials(true);

        // 暴露的响应头(前端可以访问的响应头)
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Request-Id",
                "X-Total-Count"
        ));

        // 预检请求的有效期(秒)
        config.setMaxAge(maxAge);

        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS配置完成 - 允许方法: {}, 预检缓存: {}秒", Arrays.toString(methods), maxAge);
        return new CorsFilter(source);
    }
}
