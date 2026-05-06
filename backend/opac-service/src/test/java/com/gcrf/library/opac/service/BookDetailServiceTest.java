package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookDetailVO;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookDetailServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired BookDetailService detailSvc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("DROP SCHEMA IF EXISTS school_000002 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0,"
            + " available_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.execute("CREATE TABLE school_000001.school_meta (school_code TEXT PRIMARY KEY, school_name TEXT)");
        jdbc.update("INSERT INTO school_000001.school_meta VALUES ('s1', '实验小学')");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count)"
            + " VALUES ('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3)");

        jdbc.execute("CREATE SCHEMA school_000002");
        jdbc.execute("CREATE TABLE school_000002.book_catalog (LIKE school_000001.book_catalog INCLUDING ALL)");
        jdbc.execute("CREATE TABLE school_000002.school_meta (school_code TEXT PRIMARY KEY, school_name TEXT)");
        jdbc.update("INSERT INTO school_000002.school_meta VALUES ('s2', '第二中学')");
        jdbc.update("INSERT INTO school_000002.book_catalog (isbn, title, author, classification, total_count, available_count)"
            + " VALUES ('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 3, 1)");

        mview.refresh();
    }

    @Test
    void getByIsbn_aggregatesAcrossSchools() {
        BookDetailVO d = detailSvc.getByIsbn("9787111000001");
        assertThat(d.getTitle()).isEqualTo("深入理解计算机系统");
        assertThat(d.getSchools()).hasSize(2);
        assertThat(d.getSchools()).extracting("schoolName")
            .containsExactlyInAnyOrder("实验小学", "第二中学");
    }

    @Test
    void getByIsbn_notFound_returnsNull() {
        BookDetailVO d = detailSvc.getByIsbn("0000000000");
        assertThat(d).isNull();
    }
}
