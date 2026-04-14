package com.gcrf.library.recommend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 推荐服务安全配置
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Configuration
@EnableWebSecurity
public class RecommendSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 允许所有推荐API访问（实际生产环境应添加认证）
                .requestMatchers("/api/v1/recommend/**").permitAll()
                // 允许健康检查和监控端点
                .requestMatchers("/actuator/**").permitAll()
                // 允许API文档
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
