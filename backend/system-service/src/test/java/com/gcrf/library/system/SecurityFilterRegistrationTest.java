package com.gcrf.library.system;

import com.gcrf.library.common.security.filter.SecurityContextFilter;
import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: verifies that {@link SecurityContextFilter} is auto-registered
 * as a Spring {@link FilterRegistrationBean} when common-security is on the
 * classpath of system-service.
 *
 * <p>common-security's {@code SecurityContextAutoConfiguration} is triggered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * The filter registration is conditional on a {@link com.gcrf.library.common.utils.JwtUtil}
 * bean being present in the application context — which is satisfied automatically
 * because {@link com.gcrf.library.common.utils.JwtUtil} is annotated with
 * {@code @Component} and scanned via {@code common} base package.
 *
 * @author GCRF Team
 * @since 2026-05-11
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityFilterRegistrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    FilterRegistrationBean<SecurityContextFilter> securityFilterRegistration;

    @Test
    void securityContextFilterBeanIsRegistered() {
        assertThat(securityFilterRegistration)
                .as("SecurityContextFilter FilterRegistrationBean must be present — "
                        + "check SecurityContextAutoConfiguration and JwtUtil bean wiring")
                .isNotNull();
        assertThat(securityFilterRegistration.getFilter())
                .isInstanceOf(SecurityContextFilter.class);
    }
}
