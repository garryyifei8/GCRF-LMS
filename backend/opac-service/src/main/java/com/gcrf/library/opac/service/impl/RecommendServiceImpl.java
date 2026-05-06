package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final JdbcTemplate jdbc;

    @Override
    public List<BookSearchItemVO> related(String isbn, int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        String clc = jdbc.query(
            "SELECT classification FROM gcrf_region.book_search_mview WHERE isbn = ? LIMIT 1",
            new Object[] { isbn },
            rs -> rs.next() ? rs.getString(1) : null);
        if (clc == null) return Collections.emptyList();

        return jdbc.query(
            "SELECT school_schema, book_id, isbn, title, author, classification,"
          + " total_count, available_count, created_at"
          + " FROM gcrf_region.book_search_mview"
          + " WHERE classification = ? AND isbn <> ?"
          + " ORDER BY available_count DESC LIMIT ?",
            new Object[] { clc, isbn, safeLimit },
            new BeanPropertyRowMapper<>(BookSearchItemVO.class));
    }
}
