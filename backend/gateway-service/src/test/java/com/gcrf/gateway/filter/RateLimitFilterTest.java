package com.gcrf.gateway.filter;

import com.gcrf.gateway.service.RateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitFilter单元测试
 *
 * @author Claude Code
 * @date 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private RateLimitFilter filter;

    @Test
    @DisplayName("filter_whenAllowed_shouldContinue")
    void filter_whenAllowed_shouldContinue() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(99L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("99");
    }

    @Test
    @DisplayName("filter_whenNotAllowed_shouldReturn429")
    void filter_whenNotAllowed_shouldReturn429() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(false);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("filter_shouldExtractClientIpFromXForwardedFor")
    void filter_shouldExtractClientIpFromXForwardedFor() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isAllowed(anyString(), eq("1.2.3.4"), any());
    }

    @Test
    @DisplayName("filter_shouldExtractUserIdFromHeader")
    void filter_shouldExtractUserIdFromHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .header("X-Forwarded-For", "1.2.3.4")
            .header("X-User-Id", "100")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(rateLimiterService).isAllowed(anyString(), anyString(), eq(100L));
    }

    @Test
    @DisplayName("filter_withMissingClientIp_shouldUseFallback")
    void filter_withMissingClientIp_shouldUseFallback() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/books")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimiterService.isAllowed(anyString(), anyString(), any())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString(), anyString(), any())).thenReturn(50L);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }
}
