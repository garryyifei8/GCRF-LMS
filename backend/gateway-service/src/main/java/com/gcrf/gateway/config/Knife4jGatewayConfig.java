package com.gcrf.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j Gateway聚合配置
 * 用于聚合所有微服务的API文档
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@Slf4j
@Configuration
public class Knife4jGatewayConfig {

    @Value("${spring.application.name:gateway-service}")
    private String applicationName;

    /**
     * 配置Knife4j文档路由
     * 将所有微服务的OpenAPI文档聚合到Gateway
     */
    @Bean
    public RouteLocator knife4jRouteLocator(RouteLocatorBuilder builder) {
        log.info("Initializing Knife4j Gateway aggregation routes for {}", applicationName);

        return builder.routes()
                // 认证服务API文档路由
                .route("auth-service-docs", r -> r
                        .path("/v3/api-docs/auth-service")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://auth-service"))

                // 图书服务API文档路由
                .route("book-service-docs", r -> r
                        .path("/v3/api-docs/book-service")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://book-service"))

                // 读者服务API文档路由
                .route("reader-service-docs", r -> r
                        .path("/v3/api-docs/reader-service")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://reader-service"))

                // 流通服务API文档路由
                .route("circulation-service-docs", r -> r
                        .path("/v3/api-docs/circulation-service")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://circulation-service"))

                .build();
    }
}
