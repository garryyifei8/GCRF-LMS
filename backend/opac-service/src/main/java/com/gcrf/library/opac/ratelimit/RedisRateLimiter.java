package com.gcrf.library.opac.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** INCR + EXPIRE atomic-ish rate limiter (window starts on first request). */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public boolean tryAcquire(String key, int limit, int periodSeconds) {
        String redisKey = "ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofSeconds(periodSeconds));
        }
        return count != null && count <= limit;
    }
}
