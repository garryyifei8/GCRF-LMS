package com.gcrf.library.book.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.Mockito.mock;

/**
 * Test cache configuration: replaces Redis-backed caches with no-op implementations.
 * Prevents test failures due to unavailable Redis server.
 *
 * <p>Import this in @SpringBootTest classes via @Import(TestCacheConfig.class).
 */
@TestConfiguration
public class TestCacheConfig {

    /**
     * Provide a mock Redis connection factory so Spring's Redis auto-configuration
     * does not try to open a real connection during tests.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    /**
     * Replace the Redis-backed CacheManager with a no-op implementation so
     * @Cacheable / @CacheEvict annotations are effectively disabled in tests.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
