package com.gcrf.library.opac.service;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SearchLogServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SearchLogService svc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void recordAsync_writesRow() {
        svc.recordAsync("深入计算机", "1.2.3.4", 5);

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM gcrf_region.search_log WHERE keyword = '深入计算机'",
                Integer.class);
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    void recordAsync_skipsBlankKeyword() {
        svc.recordAsync("", "1.2.3.4", 0);
        svc.recordAsync(null, "1.2.3.4", 0);

        // wait briefly to confirm nothing got written
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.search_log", Integer.class);
        assertThat(count).isZero();
    }
}
