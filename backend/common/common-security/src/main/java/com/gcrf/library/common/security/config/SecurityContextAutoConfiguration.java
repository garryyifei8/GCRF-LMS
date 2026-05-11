package com.gcrf.library.common.security.config;

import com.gcrf.library.common.security.filter.SecurityContextFilter;
import com.gcrf.library.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * 自动配置：将 {@link SecurityContextFilter} 注册为 Servlet 过滤器。
 *
 * <p>仅在 Servlet Web 环境且 Spring 上下文中存在 {@link JwtUtil} bean 时激活。
 * 运行顺序 {@code HIGHEST_PRECEDENCE + 50}，早于 TenantContextFilter ({@code HIGHEST_PRECEDENCE + 100})。
 *
 * @author GCRF Team
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(JwtUtil.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityContextAutoConfiguration {

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<SecurityContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new SecurityContextFilter(jwtUtil));
        // Run BEFORE TenantContextFilter so its SecurityContext is available when tenant context is computed.
        // TenantContextFilter currently uses HIGHEST_PRECEDENCE + 100; we use HIGHEST_PRECEDENCE + 50.
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        reg.addUrlPatterns("/*");
        log.info("SecurityContextFilter registered (order={})", reg.getOrder());
        return reg;
    }
}
