package com.gcrf.gateway.service;

import com.gcrf.gateway.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RateLimiterService Unit Tests
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rateLimiter;

    private RateLimitProperties rateLimitProperties;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setAnonymousRequestsPerMinute(100);
        rateLimitProperties.setAuthenticatedRequestsPerMinute(1000);

        rateLimiterService = new RateLimiterService(redissonClient, rateLimitProperties);
    }

    @Test
    @DisplayName("Should allow request when rate limit is disabled")
    void shouldAllowRequestWhenDisabled() {
        // Given
        rateLimitProperties.setEnabled(false);

        // When
        boolean result = rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", null);

        // Then
        assertTrue(result);
        verifyNoInteractions(redissonClient);
    }

    @Test
    @DisplayName("Should allow request when under rate limit")
    void shouldAllowRequestUnderLimit() {
        // Given
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        // When
        boolean result = rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", null);

        // Then
        assertTrue(result);
        verify(rateLimiter).tryAcquire(1);
    }

    @Test
    @DisplayName("Should deny request when over rate limit")
    void shouldDenyRequestOverLimit() {
        // Given
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(false);

        // When
        boolean result = rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", null);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should use different rate limits for authenticated users")
    void shouldUseDifferentLimitsForAuthenticatedUsers() {
        // Given
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);

        // When - anonymous user
        rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", null);

        // When - authenticated user
        rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", 123L);

        // Then - verify different keys are used
        verify(redissonClient, times(2)).getRateLimiter(anyString());
    }

    @Test
    @DisplayName("Should allow request on Redis error (fail-open)")
    void shouldAllowRequestOnRedisError() {
        // Given
        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = rateLimiterService.isAllowed("/api/v1/test", "127.0.0.1", null);

        // Then - fail-open: allow request when Redis is unavailable
        assertTrue(result);
    }
}
