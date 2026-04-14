package com.gcrf.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全响应头配置属性
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security.headers")
public class SecurityHeadersProperties {

    /**
     * 是否启用安全响应头
     */
    private boolean enabled = true;

    /**
     * Content-Security-Policy配置
     * 控制页面可以加载的资源来源
     */
    private ContentSecurityPolicy contentSecurityPolicy = new ContentSecurityPolicy();

    /**
     * X-Frame-Options配置
     * 防止点击劫持攻击
     */
    private String xFrameOptions = "DENY";

    /**
     * X-Content-Type-Options配置
     * 防止MIME类型嗅探
     */
    private String xContentTypeOptions = "nosniff";

    /**
     * Strict-Transport-Security配置
     * 强制HTTPS连接
     */
    private StrictTransportSecurity strictTransportSecurity = new StrictTransportSecurity();

    /**
     * X-XSS-Protection配置（虽然已弃用，但作为兼容性保护仍有价值）
     */
    private String xXssProtection = "1; mode=block";

    /**
     * Referrer-Policy配置
     * 控制Referrer头的发送策略
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";

    /**
     * Permissions-Policy配置
     * 控制浏览器功能权限
     */
    private PermissionsPolicy permissionsPolicy = new PermissionsPolicy();

    /**
     * Cache-Control配置（用于敏感响应）
     */
    private String cacheControl = "no-store, no-cache, must-revalidate, proxy-revalidate";

    /**
     * Content-Security-Policy详细配置
     */
    @Data
    public static class ContentSecurityPolicy {
        private boolean enabled = true;
        private String defaultSrc = "'self'";
        private String scriptSrc = "'self'";
        private String styleSrc = "'self' 'unsafe-inline'";
        private String imgSrc = "'self' data: https:";
        private String fontSrc = "'self' data:";
        private String connectSrc = "'self'";
        private String frameSrc = "'none'";
        private String objectSrc = "'none'";
        private String baseUri = "'self'";
        private String formAction = "'self'";
        private boolean upgradeInsecureRequests = true;
        private boolean blockAllMixedContent = true;

        /**
         * 构建CSP头值
         */
        public String buildHeaderValue() {
            StringBuilder csp = new StringBuilder();
            csp.append("default-src ").append(defaultSrc).append("; ");
            csp.append("script-src ").append(scriptSrc).append("; ");
            csp.append("style-src ").append(styleSrc).append("; ");
            csp.append("img-src ").append(imgSrc).append("; ");
            csp.append("font-src ").append(fontSrc).append("; ");
            csp.append("connect-src ").append(connectSrc).append("; ");
            csp.append("frame-src ").append(frameSrc).append("; ");
            csp.append("object-src ").append(objectSrc).append("; ");
            csp.append("base-uri ").append(baseUri).append("; ");
            csp.append("form-action ").append(formAction).append("; ");
            if (upgradeInsecureRequests) {
                csp.append("upgrade-insecure-requests; ");
            }
            if (blockAllMixedContent) {
                csp.append("block-all-mixed-content; ");
            }
            return csp.toString().trim();
        }
    }

    /**
     * Strict-Transport-Security详细配置
     */
    @Data
    public static class StrictTransportSecurity {
        private boolean enabled = true;
        private long maxAgeSeconds = 31536000; // 1年
        private boolean includeSubDomains = true;
        private boolean preload = false;

        /**
         * 构建HSTS头值
         */
        public String buildHeaderValue() {
            StringBuilder hsts = new StringBuilder();
            hsts.append("max-age=").append(maxAgeSeconds);
            if (includeSubDomains) {
                hsts.append("; includeSubDomains");
            }
            if (preload) {
                hsts.append("; preload");
            }
            return hsts.toString();
        }
    }

    /**
     * Permissions-Policy详细配置
     */
    @Data
    public static class PermissionsPolicy {
        private boolean enabled = true;
        private String camera = "()";
        private String microphone = "()";
        private String geolocation = "()";
        private String payment = "()";
        private String usb = "()";
        private String fullscreen = "(self)";

        /**
         * 构建Permissions-Policy头值
         */
        public String buildHeaderValue() {
            return String.format(
                    "camera=%s, microphone=%s, geolocation=%s, payment=%s, usb=%s, fullscreen=%s",
                    camera, microphone, geolocation, payment, usb, fullscreen
            );
        }
    }
}
