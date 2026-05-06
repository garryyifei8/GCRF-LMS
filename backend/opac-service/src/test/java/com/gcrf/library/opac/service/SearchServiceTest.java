package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
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
class SearchServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired SearchService search;
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
            + " available_count INT DEFAULT 0,"
            + " created_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");
        jdbc.update("INSERT INTO school_000001.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 5, 3),"
            + "('9787111000002', '算法导论', 'Cormen', 'TP', 8, 7),"
            + "('9787020002207', '红楼梦', '曹雪芹', 'I2', 4, 2)");
        jdbc.execute("CREATE SCHEMA school_000002");
        jdbc.execute("CREATE TABLE school_000002.book_catalog (LIKE school_000001.book_catalog INCLUDING ALL)");
        jdbc.update("INSERT INTO school_000002.book_catalog "
            + "(isbn, title, author, classification, total_count, available_count) VALUES "
            + "('9787111000001', '深入理解计算机系统', 'Bryant', 'TP', 3, 3)");
        mview.refresh();
    }

    @Test
    void search_byKeyword_findsAcrossSchools() {
        SearchRequest req = new SearchRequest();
        req.setQ("深入");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(2);
        assertThat(p.getRecords()).extracting(BookSearchItemVO::getSchoolSchema)
            .containsExactlyInAnyOrder("school_000001", "school_000002");
    }

    @Test
    void search_byClc_filtersToCategory() {
        SearchRequest req = new SearchRequest();
        req.setClc("I");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(1);
        assertThat(p.getRecords().get(0).getTitle()).isEqualTo("红楼梦");
    }

    @Test
    void search_bySchool_limitsToOneSchool() {
        SearchRequest req = new SearchRequest();
        req.setSchool("school_000001");
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getTotal()).isEqualTo(3);
    }

    @Test
    void search_emptyQuery_returnsAll() {
        PageVO<BookSearchItemVO> p = search.search(new SearchRequest());
        assertThat(p.getTotal()).isEqualTo(4);
    }

    @Test
    void search_pagination() {
        SearchRequest req = new SearchRequest();
        req.setPageNum(1); req.setPageSize(2);
        PageVO<BookSearchItemVO> p = search.search(req);
        assertThat(p.getRecords()).hasSize(2);
        assertThat(p.getTotal()).isEqualTo(4);
    }
}
