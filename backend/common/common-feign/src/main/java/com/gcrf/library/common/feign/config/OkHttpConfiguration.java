package com.gcrf.library.common.feign.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp配置类
 * 使用OkHttp作为Feign的HTTP客户端，提供更好的连接池和性能
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Configuration
@ConditionalOnClass(OkHttpClient.class)
@EnableConfigurationProperties(FeignProperties.class)
public class OkHttpConfiguration {

    /**
     * OkHttp连接池配置
     */
    @Bean
    @ConditionalOnMissingBean
    public ConnectionPool okHttpConnectionPool() {
        // 最大空闲连接数: 200
        // 连接保持时间: 5分钟
        return new ConnectionPool(200, 5, TimeUnit.MINUTES);
    }

    /**
     * OkHttp客户端配置
     */
    @Bean
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient(ConnectionPool connectionPool, FeignProperties properties) {
        log.info("初始化OkHttp客户端: connectTimeout={}ms, readTimeout={}ms",
                properties.getConnectTimeout(), properties.getReadTimeout());

        return new OkHttpClient.Builder()
                // 连接池
                .connectionPool(connectionPool)
                // 连接超时
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                // 读取超时
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                // 写入超时
                .writeTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                // 禁用重定向（由Feign处理）
                .followRedirects(false)
                // 禁用SSL重定向
                .followSslRedirects(false)
                // 重试连接失败
                .retryOnConnectionFailure(true)
                .build();
    }
}
