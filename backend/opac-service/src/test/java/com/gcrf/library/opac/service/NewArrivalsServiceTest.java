package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
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
class NewArrivalsServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired NewArrivalsService svc;
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
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count, created_at)"
            + " VALUES ('1', 'Old', 'a', 'I', 1, 1, NOW() - INTERVAL '60 days')");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count, created_at)"
            + " VALUES ('2', 'New', 'b', 'I', 1, 1, NOW())");
        mview.refresh();
    }

    @Test
    void newArrivals_filtersByDays() {
        List<BookSearchItemVO> recent = svc.newArrivals(null, 30, 10);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getTitle()).isEqualTo("New");
    }
}
