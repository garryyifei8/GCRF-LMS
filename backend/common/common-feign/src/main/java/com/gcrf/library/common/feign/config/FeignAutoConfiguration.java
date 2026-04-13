package com.gcrf.library.common.feign.config;

import com.gcrf.library.common.feign.interceptor.FeignRequestInterceptor;
import com.gcrf.library.common.feign.interceptor.FeignResponseInterceptor;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign自动配置类
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Configuration
@ConditionalOnClass(EnableFeignClients.class)
@EnableConfigurationProperties(FeignProperties.class)
public class FeignAutoConfiguration {

    /**
     * Feign请求拦截器 - 用于传递请求头（认证信息、追踪ID等）
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor feignRequestInterceptor() {
        log.info("初始化Feign请求拦截器");
        return new FeignRequestInterceptor();
    }

    /**
     * Feign响应拦截器 - 用于统一处理响应
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignResponseInterceptor feignResponseInterceptor() {
        log.info("初始化Feign响应拦截器");
        return new FeignResponseInterceptor();
    }

    /**
     * Feign错误解码器 - 用于统一处理错误响应
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder feignErrorDecoder() {
        log.info("初始化Feign错误解码器");
        return new FeignErrorDecoder();
    }

    /**
     * Feign日志级别
     * NONE - 不记录日志
     * BASIC - 仅记录请求方法、URL、响应状态码和执行时间
     * HEADERS - 记录BASIC级别日志 + 请求和响应头
     * FULL - 记录请求和响应的头、体、元数据
     */
    @Bean
    @ConditionalOnMissingBean
    public Logger.Level feignLoggerLevel(FeignProperties properties) {
        Logger.Level level = properties.getLoggerLevel();
        log.info("Feign日志级别: {}", level);
        return level;
    }

    /**
     * Feign重试器
     * 默认配置: 最大重试3次，间隔100ms，最大间隔1s
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer(FeignProperties properties) {
        FeignProperties.Retry retry = properties.getRetry();
        if (!retry.isEnabled()) {
            log.info("Feign重试已禁用");
            return Retryer.NEVER_RETRY;
        }
        log.info("Feign重试配置: maxAttempts={}, period={}ms, maxPeriod={}ms",
                retry.getMaxAttempts(), retry.getPeriod(), retry.getMaxPeriod());
        return new Retryer.Default(
                retry.getPeriod(),
                TimeUnit.MILLISECONDS.toMillis(retry.getMaxPeriod()),
                retry.getMaxAttempts()
        );
    }
}
