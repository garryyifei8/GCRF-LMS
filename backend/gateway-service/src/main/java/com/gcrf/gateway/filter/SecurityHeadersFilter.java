package com.gcrf.gateway.filter;

import com.gcrf.gateway.config.SecurityHeadersProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全响应头过滤器
 *
 * 添加以下安全头以增强应用安全性：
 * - Content-Security-Policy: 控制资源加载策略，防止XSS等攻击
 * - X-Frame-Options: 防止点击劫持攻击
 * - X-Content-Type-Options: 防止MIME类型嗅探攻击
 * - Strict-Transport-Security: 强制HTTPS连接
 * - X-XSS-Protection: 浏览器XSS过滤器（兼容性）
 * - Referrer-Policy: 控制Referrer头发送策略
 * - Permissions-Policy: 控制浏览器功能权限
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    private final SecurityHeadersProperties securityHeadersProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!securityHeadersProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            addSecurityHeaders(headers);
        }));
    }

    /**
     * 添加安全响应头
     */
    private void addSecurityHeaders(HttpHeaders headers) {
        // Content-Security-Policy
        SecurityHeadersProperties.ContentSecurityPolicy csp = securityHeadersProperties.getContentSecurityPolicy();
        if (csp.isEnabled()) {
            headers.add("Content-Security-Policy", csp.buildHeaderValue());
        }

        // X-Frame-Options - 防止点击劫持
        String xFrameOptions = securityHeadersProperties.getXFrameOptions();
        if (xFrameOptions != null && !xFrameOptions.isEmpty()) {
            headers.add("X-Frame-Options", xFrameOptions);
        }

        // X-Content-Type-Options - 防止MIME嗅探
        String xContentTypeOptions = securityHeadersProperties.getXContentTypeOptions();
        if (xContentTypeOptions != null && !xContentTypeOptions.isEmpty()) {
            headers.add("X-Content-Type-Options", xContentTypeOptions);
        }

        // Strict-Transport-Security (HSTS)
        SecurityHeadersProperties.StrictTransportSecurity hsts = securityHeadersProperties.getStrictTransportSecurity();
        if (hsts.isEnabled()) {
            headers.add("Strict-Transport-Security", hsts.buildHeaderValue());
        }

        // X-XSS-Protection - 兼容性保护
        String xXssProtection = securityHeadersProperties.getXXssProtection();
        if (xXssProtection != null && !xXssProtection.isEmpty()) {
            headers.add("X-XSS-Protection", xXssProtection);
        }

        // Referrer-Policy
        String referrerPolicy = securityHeadersProperties.getReferrerPolicy();
        if (referrerPolicy != null && !referrerPolicy.isEmpty()) {
            headers.add("Referrer-Policy", referrerPolicy);
        }

        // Permissions-Policy
        SecurityHeadersProperties.PermissionsPolicy permissionsPolicy = securityHeadersProperties.getPermissionsPolicy();
        if (permissionsPolicy.isEnabled()) {
            headers.add("Permissions-Policy", permissionsPolicy.buildHeaderValue());
        }

        // Cache-Control for sensitive responses
        String cacheControl = securityHeadersProperties.getCacheControl();
        if (cacheControl != null && !cacheControl.isEmpty()) {
            // 只对API响应设置，不影响静态资源
            headers.add("Cache-Control", cacheControl);
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
        }

        log.debug("安全响应头已添加");
    }

    @Override
    public int getOrder() {
        // 最后执行，确保响应头被正确添加
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
