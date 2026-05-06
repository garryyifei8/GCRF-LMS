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
class RecommendServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired RecommendService svc;
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
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification, total_count, available_count) VALUES "
            + "('isbn1','深入理解计算机系统','Bryant','TP',5,3),"
            + "('isbn2','算法导论','Cormen','TP',8,7),"
            + "('isbn3','红楼梦','曹雪芹','I2',4,2)");
        mview.refresh();
    }

    @Test
    void related_returnsSameClassification() {
        List<BookSearchItemVO> rel = svc.related("isbn1", 5);
        assertThat(rel).extracting("classification").allMatch(c -> c.equals("TP"));
        assertThat(rel).extracting("isbn").doesNotContain("isbn1");
    }
}
