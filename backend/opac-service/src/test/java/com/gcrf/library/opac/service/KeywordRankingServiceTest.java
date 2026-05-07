package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.KeywordRankingVO;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class KeywordRankingServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired KeywordRankingService svc;
    @Autowired SearchLogService logSvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanup() {
        // clear the log table; @Async writes from previous tests may still be settling
        jdbc.update("DELETE FROM gcrf_region.search_log");
    }

    @Test
    void topKeywords_groupsAndRanks() {
        logSvc.recordAsync("深入计算机", "1.1.1.1", 5);
        logSvc.recordAsync("深入计算机", "2.2.2.2", 5);
        logSvc.recordAsync("深入计算机", "3.3.3.3", 5);
        logSvc.recordAsync("算法导论", "4.4.4.4", 8);

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM gcrf_region.search_log", Integer.class);
            assertThat(total).isEqualTo(4);
        });

        List<KeywordRankingVO> top = svc.topKeywords(30, 10);
        assertThat(top).hasSize(2);
        assertThat(top.get(0).getKeyword()).isEqualTo("深入计算机");
        assertThat(top.get(0).getCount()).isEqualTo(3L);
        assertThat(top.get(0).getRank()).isEqualTo(1);
        assertThat(top.get(1).getKeyword()).isEqualTo("算法导论");
        assertThat(top.get(1).getCount()).isEqualTo(1L);
        assertThat(top.get(1).getRank()).isEqualTo(2);
    }
}
