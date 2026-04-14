package com.gcrf.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LogFilter单元测试
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class LogFilterTest {

    @Autowired
    private LogFilter logFilter;

    private GatewayFilterChain mockChain;

    @BeforeEach
    void setUp() {
        mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testLogFilterRecordsRequest() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/books?page=1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = logFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        // Verify that the chain was called
        verify(mockChain).filter(any());

        // Verify that startTime was recorded
        assertNotNull(exchange.getAttribute("startTime"));
    }

    @Test
    void testLogFilterRecordsStartTime() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/readers")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = logFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        // Verify startTime is set as an attribute
        Long startTime = exchange.getAttribute("startTime");
        assertNotNull(startTime);
        assertTrue(startTime <= System.currentTimeMillis());
    }

    @Test
    void testLogFilterCallsChain() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/books")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = logFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockChain, times(1)).filter(any());
    }

    @Test
    void testLogFilterHandlesSuccessResponse() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/circulation")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        // Act
        Mono<Void> result = logFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void testLogFilterOrder() {
        // Test that LogFilter has correct order (-50)
        // It should execute after AuthenticationFilter (-100)
        assertEquals(-50, logFilter.getOrder());
    }

    @Test
    void testLogFilterOrderIsAfterAuthFilter() {
        // Verify that LogFilter order is greater than AuthenticationFilter
        // (lower priority, executes later)
        // AuthenticationFilter has order -100, LogFilter has order -50
        // Higher number means later execution
        int authFilterOrder = -100;  // AuthenticationFilter.getOrder()
        assertTrue(logFilter.getOrder() > authFilterOrder);
    }
}
