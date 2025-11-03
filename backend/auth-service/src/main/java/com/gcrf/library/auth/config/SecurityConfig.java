package com.gcrf.library.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF (使用JWT不需要CSRF保护)
                .csrf(AbstractHttpConfigurer::disable)

                // 配置Session管理为无状态
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开端点 - 无需认证
                        .requestMatchers(
                                "/api/v1/auth/login",        // 登录接口 (直接访问)
                                "/api/v1/auth/register",     // 注册接口 (直接访问)
                                "/api/v1/auth/validate",     // Token验证 (直接访问,接受任何token)
                                "/api/v1/auth/refresh",      // Token刷新 (直接访问,token在body中)
                                "/api/v1/auth/health",       // 健康检查 (直接访问)
                                "/api/v1/users/health",      // 健康检查 (直接访问)
                                "/auth/login",               // 登录接口 (Gateway转发,StripPrefix后)
                                "/auth/register",            // 注册接口 (Gateway转发,StripPrefix后)
                                "/auth/validate",            // Token验证 (Gateway转发,StripPrefix后)
                                "/auth/refresh",             // Token刷新 (Gateway转发,StripPrefix后)
                                "/auth/health",              // 健康检查 (Gateway转发,StripPrefix后)
                                "/users/health",             // 健康检查 (Gateway转发,StripPrefix后)
                                "/v3/api-docs/**",           // OpenAPI文档
                                "/swagger-ui/**",            // Swagger UI
                                "/swagger-ui.html",          // Swagger UI
                                "/doc.html",                 // Knife4j UI
                                "/webjars/**",               // Knife4j静态资源
                                "/actuator/**"               // 监控端点
                        ).permitAll()

                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 配置异常处理
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器Bean
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
