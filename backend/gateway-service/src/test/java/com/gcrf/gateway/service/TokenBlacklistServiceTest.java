package com.gcrf.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TokenBlacklistService Unit Tests
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TokenBlacklistServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Object> bucket;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redissonClient);
        ReflectionTestUtils.setField(tokenBlacklistService, "tokenExpirationMs", 7200000L);
    }

    @Test
    @DisplayName("Should return false for null or empty token")
    void shouldReturnFalseForNullOrEmptyToken() {
        assertFalse(tokenBlacklistService.isBlacklisted(null));
        assertFalse(tokenBlacklistService.isBlacklisted(""));
    }

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void shouldReturnTrueWhenTokenIsBlacklisted() {
        // Given
        String token = "test-token-123";
        when(redissonClient.<Object>getBucket("auth:blacklist:" + token)).thenReturn(bucket);
        when(bucket.isExists()).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        // Given
        String token = "valid-token-456";
        when(redissonClient.<Object>getBucket("auth:blacklist:" + token)).thenReturn(bucket);
        when(bucket.isExists()).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true on Redis error (fail-closed for security)")
    void shouldReturnTrueOnRedisError() {
        // Given
        String token = "test-token";
        when(redissonClient.<Object>getBucket(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Then - fail-closed: treat as blacklisted when Redis is unavailable (security measure)
        assertTrue(result);
    }

    @Test
    @DisplayName("Should add token to blacklist with expiration")
    void shouldAddTokenToBlacklistWithExpiration() {
        // Given
        String token = "token-to-blacklist";
        when(redissonClient.<Object>getBucket("auth:blacklist:" + token)).thenReturn(bucket);

        // When
        tokenBlacklistService.addToBlacklist(token, "logged_out");

        // Then
        verify(bucket).set(eq("logged_out"), eq(7200L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should not throw on null token when adding to blacklist")
    void shouldNotThrowOnNullTokenWhenAdding() {
        // When/Then - should not throw
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(null, "test"));
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist("", "test"));
    }

    @Test
    @DisplayName("Should remove token from blacklist")
    void shouldRemoveTokenFromBlacklist() {
        // Given
        String token = "token-to-remove";
        when(redissonClient.<Object>getBucket("auth:blacklist:" + token)).thenReturn(bucket);

        // When
        tokenBlacklistService.removeFromBlacklist(token);

        // Then
        verify(bucket).delete();
    }
}
