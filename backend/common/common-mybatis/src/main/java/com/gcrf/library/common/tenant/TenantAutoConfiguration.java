package com.gcrf.library.common.tenant;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.gcrf.library.common.utils.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * 多租户基础设施自动装配。
 * 业务服务通过 {@code gcrf.tenant.enabled=true}（默认 true）启用。
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(JwtUtil.class)
@ConditionalOnProperty(prefix = "gcrf.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    @Bean
    public SearchPathInterceptor searchPathInterceptor() {
        return new SearchPathInterceptor();
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TenantContextFilter(jwtUtil));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 100); // after security filter
        reg.addUrlPatterns("/*");
        return reg;
    }
}
