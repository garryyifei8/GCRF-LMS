package com.gcrf.library.common.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置类
 * <p>
 * 微服务架构下的安全配置：
 * <ul>
 *   <li>禁用CSRF（使用JWT，无需CSRF保护）</li>
 *   <li>禁用Session（无状态认证）</li>
 *   <li>允许所有请求通过（由Gateway统一认证）</li>
 *   <li>提供密码加密器（用于用户密码加密）</li>
 * </ul>
 * <p>
 * 注意：在微服务架构中，认证由API Gateway统一处理，业务服务只需验证token有效性。
 * <p>
 * ⚠️ 仅在Servlet环境下启用，避免与WebFlux Gateway冲突
 *
 * @author GCRF Team
 * @date 2025-10-23
 */
@Slf4j
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonSecurityConfig {

    /**
     * 配置Security过滤链
     * <p>
 * 微服务架构下的简化配置：
     * <ul>
     *   <li>禁用CSRF - JWT认证不需要CSRF保护</li>
     *   <li>禁用Session - 使用无状态JWT认证</li>
     *   <li>允许所有请求 - 由API Gateway统一认证</li>
     * </ul>
     *
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护（使用JWT，不需要CSRF）
                .csrf(AbstractHttpConfigurer::disable)

                // 配置Session管理为无状态
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 允许所有请求（由Gateway统一认证）
                        .anyRequest().permitAll()
                )

                // 禁用框架默认登录页
                .formLogin(AbstractHttpConfigurer::disable)

                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)

                // 禁用登出功能（由业务服务自己处理）
                .logout(AbstractHttpConfigurer::disable);

        log.info("Spring Security配置完成 - 无状态模式，允许所有请求（由Gateway认证）");
        return http.build();
    }

    /**
     * 密码加密器
     * <p>
     * 使用BCrypt算法加密密码：
     * <ul>
     *   <li>自动加盐，每次加密结果不同</li>
     *   <li>单向加密，无法解密</li>
     *   <li>适配度高，Spring Security原生支持</li>
     * </ul>
     *
     * @return PasswordEncoder
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
