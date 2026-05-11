package com.gcrf.library.common.security.config;

import com.gcrf.library.common.security.aspect.SecurityRequirementAspect;
import com.gcrf.library.common.security.permission.PermissionLookup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@EnableAspectJAutoProxy
public class SecurityRequirementAutoConfiguration {

    /** 仅当业务服务提供了 PermissionLookup 实现时启用 aspect（permit 注解可不查 DB）。 */
    @Bean
    @ConditionalOnBean(PermissionLookup.class)
    public SecurityRequirementAspect securityRequirementAspect(PermissionLookup lookup) {
        return new SecurityRequirementAspect(lookup);
    }
}
