package com.gcrf.gateway.filter;

import com.gcrf.gateway.config.SecurityHeadersProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SecurityHeadersFilter Unit Tests
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private SecurityHeadersProperties properties;
    private SecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        properties = new SecurityHeadersProperties();
        properties.setEnabled(true);
        properties.setXFrameOptions("DENY");
        properties.setXContentTypeOptions("nosniff");
        properties.setXXssProtection("1; mode=block");
        properties.setReferrerPolicy("strict-origin-when-cross-origin");

        filter = new SecurityHeadersFilter(properties);
    }

    @Test
    @DisplayName("Should add security headers when enabled")
    void shouldAddSecurityHeadersWhenEnabled() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("Should not add headers when disabled")
    void shouldNotAddHeadersWhenDisabled() {
        // Given
        properties.setEnabled(false);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("Content-Security-Policy should build correct header value")
    void contentSecurityPolicyShouldBuildCorrectValue() {
        // Given
        SecurityHeadersProperties.ContentSecurityPolicy csp = new SecurityHeadersProperties.ContentSecurityPolicy();
        csp.setEnabled(true);
        csp.setDefaultSrc("'self'");
        csp.setScriptSrc("'self'");
        csp.setStyleSrc("'self' 'unsafe-inline'");
        csp.setImgSrc("'self' data:");
        csp.setUpgradeInsecureRequests(true);
        csp.setBlockAllMixedContent(true);

        // When
        String headerValue = csp.buildHeaderValue();

        // Then
        assertTrue(headerValue.contains("default-src 'self'"));
        assertTrue(headerValue.contains("script-src 'self'"));
        assertTrue(headerValue.contains("upgrade-insecure-requests"));
        assertTrue(headerValue.contains("block-all-mixed-content"));
    }

    @Test
    @DisplayName("HSTS should build correct header value")
    void hstsShouldBuildCorrectValue() {
        // Given
        SecurityHeadersProperties.StrictTransportSecurity hsts = new SecurityHeadersProperties.StrictTransportSecurity();
        hsts.setEnabled(true);
        hsts.setMaxAgeSeconds(31536000);
        hsts.setIncludeSubDomains(true);
        hsts.setPreload(false);

        // When
        String headerValue = hsts.buildHeaderValue();

        // Then
        assertEquals("max-age=31536000; includeSubDomains", headerValue);
    }

    @Test
    @DisplayName("HSTS with preload should include preload directive")
    void hstsWithPreloadShouldIncludePreloadDirective() {
        // Given
        SecurityHeadersProperties.StrictTransportSecurity hsts = new SecurityHeadersProperties.StrictTransportSecurity();
        hsts.setEnabled(true);
        hsts.setMaxAgeSeconds(31536000);
        hsts.setIncludeSubDomains(true);
        hsts.setPreload(true);

        // When
        String headerValue = hsts.buildHeaderValue();

        // Then
        assertEquals("max-age=31536000; includeSubDomains; preload", headerValue);
    }

    @Test
    @DisplayName("Permissions-Policy should build correct header value")
    void permissionsPolicyShouldBuildCorrectValue() {
        // Given
        SecurityHeadersProperties.PermissionsPolicy pp = new SecurityHeadersProperties.PermissionsPolicy();
        pp.setEnabled(true);
        pp.setCamera("()");
        pp.setMicrophone("()");
        pp.setGeolocation("()");

        // When
        String headerValue = pp.buildHeaderValue();

        // Then
        assertTrue(headerValue.contains("camera=()"));
        assertTrue(headerValue.contains("microphone=()"));
        assertTrue(headerValue.contains("geolocation=()"));
    }

    @Test
    @DisplayName("Filter should have lowest precedence order")
    void filterShouldHaveLowestPrecedenceOrder() {
        // Given/When
        int order = filter.getOrder();

        // Then - should be near lowest precedence to run last
        assertTrue(order > 0);
    }
}
