package com.gcrf.gateway.filter;

import com.gcrf.library.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthenticationFilter单元测试
 *
 * @author Claude Code
 * @date 2025-10-28
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class AuthenticationFilterTest {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private JwtUtil jwtUtil;

    private GatewayFilterChain mockChain;

    @BeforeEach
    void setUp() {
        mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testWhitelistPathAllowsAccessWithoutToken() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockChain).filter(exchange);
    }

    @Test
    void testActuatorPathInWhitelist() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockChain).filter(exchange);
    }

    @Test
    void testProtectedPathWithoutTokenReturns401() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/books")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(mockChain, never()).filter(any());
    }

    @Test
    void testProtectedPathWithInvalidTokenReturns401() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(mockChain, never()).filter(any());
    }

    @Test
    void testProtectedPathWithValidTokenPasses() {
        // Arrange - Generate valid JWT token
        Long userId = 123L;
        String username = "testuser";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", "ADMIN");
        String token = jwtUtil.generateToken(username, claims);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockChain).filter(any());
    }

    @Test
    void testValidTokenAddsUserInfoToHeaders() {
        // Arrange
        Long userId = 456L;
        String username = "admin";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", "ADMIN");
        String token = jwtUtil.generateToken(username, claims);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/readers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the modified exchange
        when(mockChain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();

            // Verify user info headers are added
            assertEquals(String.valueOf(userId),
                    modifiedRequest.getHeaders().getFirst("X-User-Id"));
            assertEquals(username,
                    modifiedRequest.getHeaders().getFirst("X-Username"));

            return Mono.empty();
        });

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testFilterOrder() {
        // Test that AuthenticationFilter has correct order (-100)
        assertEquals(-100, authenticationFilter.getOrder());
    }

    @Test
    void testMissingBearerPrefixReturns401() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "InvalidPrefix token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, mockChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(mockChain, never()).filter(any());
    }
}
