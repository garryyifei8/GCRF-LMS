package com.gcrf.library.opac.ratelimit;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisRateLimiterTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        r.add("gcrf.opac.rate-limit.enabled", () -> true);
    }

    @Autowired RedisRateLimiter limiter;

    @Test
    void allows_underLimit() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("test:1", 5, 1)).isTrue();
        }
    }

    @Test
    void rejects_atLimit() {
        for (int i = 0; i < 3; i++) limiter.tryAcquire("test:2", 3, 1);
        assertThat(limiter.tryAcquire("test:2", 3, 1)).isFalse();
    }
}
