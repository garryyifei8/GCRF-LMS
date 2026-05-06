package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
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
class RankingServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired RankingService svc;
    @Autowired SearchMviewService mview;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("DROP SCHEMA IF EXISTS school_000001 CASCADE");
        jdbc.execute("CREATE SCHEMA school_000001");
        jdbc.execute("CREATE TABLE school_000001.book_catalog ("
            + " id BIGSERIAL PRIMARY KEY, isbn TEXT, title TEXT, author TEXT,"
            + " classification TEXT, total_count INT DEFAULT 0, available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ DEFAULT NOW())");
        jdbc.execute("CREATE TABLE school_000001.book_copy ("
            + " id BIGSERIAL PRIMARY KEY, catalog_id BIGINT, barcode TEXT)");
        jdbc.execute("CREATE TABLE school_000001.borrow_record ("
            + " id BIGSERIAL PRIMARY KEY, copy_id BIGINT, reader_id BIGINT,"
            + " borrow_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
            + " due_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
            + " return_at TIMESTAMPTZ)");
        jdbc.update("INSERT INTO school_000001.book_catalog (isbn, title, author, classification) VALUES "
            + "('isbn1','深入理解计算机系统','Bryant','TP'),"
            + "('isbn2','算法导论','Cormen','TP')");
        jdbc.update("INSERT INTO school_000001.book_copy (catalog_id, barcode) VALUES "
            + "(1,'b1'),(1,'b2'),(2,'b3')");
        jdbc.update("INSERT INTO school_000001.borrow_record (copy_id, reader_id) VALUES "
            + "(1, 1), (1, 2), (2, 3), (3, 4)");
        // book 1 has 3 borrows, book 2 has 1
        mview.refresh();
    }

    @Test
    void borrowRanking_top10_ordersByCount() {
        List<RankingItemVO> top = svc.borrowRanking("THIS_MONTH", 10);
        assertThat(top).hasSizeGreaterThanOrEqualTo(2);
        assertThat(top.get(0).getIsbn()).isEqualTo("isbn1");
        assertThat(top.get(0).getBorrowCount()).isEqualTo(3L);
    }
}
