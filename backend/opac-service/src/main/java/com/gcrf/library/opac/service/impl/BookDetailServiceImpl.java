package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.BookDetailVO;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.SchoolAvailabilityVO;
import com.gcrf.library.opac.mapper.CrossSchoolMapper;
import com.gcrf.library.opac.service.BookDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookDetailServiceImpl implements BookDetailService {

    private final CrossSchoolMapper mapper;
    private final JdbcTemplate jdbc;

    @Override
    public BookDetailVO getByIsbn(String isbn) {
        List<BookSearchItemVO> rows = mapper.findByIsbn(isbn);
        if (rows.isEmpty()) return null;

        BookSearchItemVO any = rows.get(0);
        BookDetailVO d = new BookDetailVO();
        d.setIsbn(any.getIsbn());
        d.setTitle(any.getTitle());
        d.setAuthor(any.getAuthor());
        d.setClassification(any.getClassification());

        List<SchoolAvailabilityVO> schools = new ArrayList<>();
        for (BookSearchItemVO r : rows) {
            String name = lookupSchoolName(r.getSchoolSchema());
            schools.add(new SchoolAvailabilityVO(
                r.getSchoolSchema(), name, r.getTotalCount(), r.getAvailableCount()));
        }
        d.setSchools(schools);
        return d;
    }

    private String lookupSchoolName(String schema) {
        if (schema == null || !schema.matches("^school_\\d+$")) return schema;
        try {
            return jdbc.queryForObject(
                "SELECT school_name FROM \"" + schema + "\".school_meta LIMIT 1", String.class);
        } catch (Exception e) {
            return schema;
        }
    }
}
