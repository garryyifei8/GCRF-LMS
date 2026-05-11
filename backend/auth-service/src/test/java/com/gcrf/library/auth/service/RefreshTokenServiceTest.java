package com.gcrf.library.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RedissonClient redisson;
    @Mock RBucket<Long> bucket;

    @InjectMocks RefreshTokenService svc;

    @Test
    void issueWritesToRedisWith30DayTTL() {
        when(redisson.<Long>getBucket(anyString())).thenReturn(bucket);

        String token = svc.issue(42L);

        assertThat(token).matches("[0-9a-f-]{36}");
        verify(bucket).set(eq(42L), eq(Duration.ofDays(30)));
    }

    @Test
    void consumeReadsAndDeletes() {
        when(redisson.<Long>getBucket("refresh:abc")).thenReturn(bucket);
        when(bucket.get()).thenReturn(42L);

        Long userId = svc.consume("abc");

        assertThat(userId).isEqualTo(42L);
        verify(bucket).delete();
    }

    @Test
    void consumeUnknownThrowsBusinessException() {
        when(redisson.<Long>getBucket("refresh:bad")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        assertThatThrownBy(() -> svc.consume("bad"))
            .hasMessageContaining("refresh");
    }

    @Test
    void revokeDeletesKey() {
        when(redisson.<Long>getBucket("refresh:abc")).thenReturn(bucket);
        svc.revoke("abc");
        verify(bucket).delete();
    }
}
