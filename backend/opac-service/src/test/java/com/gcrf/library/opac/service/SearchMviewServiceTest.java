package com.gcrf.library.opac.service;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SearchMviewServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SearchMviewService svc;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Order(1)
    void refresh_emptyCluster_succeedsWithZeroRows() {
        int rows = svc.refresh();
        assertThat(rows).isZero();
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.book_search_mview", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    @Order(2)
    void refresh_withSchoolBooks_returnsRowCount() {
        // simulate one school
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3),"
            + "('9787111000002', '算法导论', 'Cormen', 'TP', 8, 7)");

        int rows = svc.refresh();
        assertThat(rows).isEqualTo(2);

        Integer found = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.book_search_mview WHERE title ILIKE '%深入%'",
            Integer.class);
        assertThat(found).isEqualTo(1);
    }
}
