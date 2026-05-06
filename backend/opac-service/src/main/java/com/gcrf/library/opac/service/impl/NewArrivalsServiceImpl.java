package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.service.NewArrivalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewArrivalsServiceImpl implements NewArrivalsService {

    private final JdbcTemplate jdbc;

    @Override
    public List<BookSearchItemVO> newArrivals(String school, int days, int limit) {
        int safeDays = Math.max(1, Math.min(365, days));
        int safeLimit = Math.max(1, Math.min(100, limit));
        String sql = "SELECT school_schema, book_id, isbn, title, author, classification,"
                   + " total_count, available_count, created_at"
                   + " FROM gcrf_region.book_search_mview"
                   + " WHERE created_at >= NOW() - (? || ' days')::INTERVAL";
        if (school != null && !school.isBlank()) {
            sql += " AND school_schema = ?";
        }
        sql += " ORDER BY created_at DESC LIMIT ?";
        Object[] args = (school != null && !school.isBlank())
            ? new Object[] { String.valueOf(safeDays), school, safeLimit }
            : new Object[] { String.valueOf(safeDays), safeLimit };
        return jdbc.query(sql, args, new BeanPropertyRowMapper<>(BookSearchItemVO.class));
    }
}
