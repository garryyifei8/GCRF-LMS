package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
import com.gcrf.library.opac.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final JdbcTemplate jdbc;

    private String intervalFor(String range) {
        if (range == null) return "30 days";
        return switch (range) {
            case "THIS_WEEK"  -> "7 days";
            case "THIS_TERM"  -> "180 days";
            case "THIS_MONTH" -> "30 days";
            default           -> "30 days";
        };
    }

    @Override
    public List<RankingItemVO> borrowRanking(String range, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        String interval = intervalFor(range);

        // Discover schools — schema names are constrained by the LIKE pattern
        // (only alphanumeric + underscore survives), so embedding them in SQL is safe.
        List<String> schools = jdbc.queryForList(
            "SELECT nspname FROM pg_namespace WHERE nspname LIKE 'school\\_%' ESCAPE '\\'",
            String.class);
        if (schools.isEmpty()) return new ArrayList<>();

        // Build a UNION ALL across every school schema to aggregate borrow counts.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < schools.size(); i++) {
            if (i > 0) sb.append(" UNION ALL ");
            String s = schools.get(i);
            sb.append("SELECT c.isbn, c.title, c.author, c.classification, b.id AS borrow_id ")
              .append("FROM ").append(s).append(".borrow_record b ")
              .append("JOIN ").append(s).append(".book_copy bc ON bc.id = b.copy_id ")
              .append("JOIN ").append(s).append(".book_catalog c ON c.id = bc.catalog_id ")
              .append("WHERE b.borrow_at >= NOW() - INTERVAL '").append(interval).append("'");
        }
        String sql = "SELECT isbn, title, author, classification, count(*) AS bc FROM ("
            + sb + ") t GROUP BY isbn, title, author, classification "
            + "ORDER BY bc DESC LIMIT ?";

        log.debug("borrowRanking sql: {}", sql);

        AtomicInteger rank = new AtomicInteger(1);
        RowMapper<RankingItemVO> mapper = (rs, i) -> {
            RankingItemVO v = new RankingItemVO();
            v.setRank(rank.getAndIncrement());
            v.setIsbn(rs.getString("isbn"));
            v.setTitle(rs.getString("title"));
            v.setAuthor(rs.getString("author"));
            v.setClassification(rs.getString("classification"));
            v.setBorrowCount(rs.getLong("bc"));
            return v;
        };
        return jdbc.query(sql, mapper, safeLimit);
    }
}
