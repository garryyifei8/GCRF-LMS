package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.SuggestionVO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SuggestServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SuggestService svc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification) VALUES "
            + "('isbn1', '深入理解计算机系统', 'Bryant', 'TP'),"
            + "('isbn2', '深入理解Java虚拟机', '周志明', 'TP'),"
            + "('isbn3', '红楼梦', '曹雪芹', 'I2')");
        mview.refresh();
    }

    @Test
    void suggest_returnsPrefixMatches() {
        List<SuggestionVO> out = svc.suggest("深入", 10);
        assertThat(out).hasSize(2);
        assertThat(out).extracting("text")
            .containsExactlyInAnyOrder("深入理解计算机系统", "深入理解Java虚拟机");
    }

    @Test
    void suggest_emptyQuery_returnsEmpty() {
        assertThat(svc.suggest(null, 10)).isEmpty();
        assertThat(svc.suggest("", 10)).isEmpty();
        assertThat(svc.suggest("   ", 10)).isEmpty();
    }

    @Test
    void suggest_clampLimit() {
        // limit 0 / negative / huge — all clamped
        assertThat(svc.suggest("深入", 0)).hasSize(2);    // clamped to default
        assertThat(svc.suggest("深入", -5)).hasSize(2);   // clamped to default
    }
}
