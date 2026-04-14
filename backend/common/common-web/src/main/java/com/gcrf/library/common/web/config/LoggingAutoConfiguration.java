package com.gcrf.library.common.web.config;

import com.gcrf.library.common.web.filter.MdcLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 日志自动配置
 * <p>
 * 自动注册MDC日志过滤器，为所有HTTP请求添加追踪上下文。
 * <p>
 * 功能:
 * <ul>
 *   <li>自动注册MdcLoggingFilter作为最高优先级过滤器</li>
 *   <li>为所有请求添加traceId、spanId、requestId等上下文</li>
 *   <li>支持分布式追踪头的传递和生成</li>
 * </ul>
 *
 * @author GCRF Team
 * @date 2025-12-01
 * @see MdcLoggingFilter
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LoggingAutoConfiguration {

    /**
     * 注册MDC日志过滤器
     * <p>
     * 设置为最高优先级，确保在所有其他过滤器之前执行，
     * 使得整个请求处理链都能获取到追踪上下文。
     *
     * @param mdcLoggingFilter MDC日志过滤器
     * @return 过滤器注册Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "mdcLoggingFilterRegistration")
    public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilterRegistration(
            MdcLoggingFilter mdcLoggingFilter) {
        log.info("Registering MDC Logging Filter for distributed tracing support");

        FilterRegistrationBean<MdcLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(mdcLoggingFilter);
        registration.addUrlPatterns("/*");
        registration.setName("mdcLoggingFilter");
        // 最高优先级，确保在所有其他过滤器之前执行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }

    /**
     * MDC日志过滤器Bean
     *
     * @return MDC日志过滤器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MdcLoggingFilter mdcLoggingFilter() {
        return new MdcLoggingFilter();
    }
}
