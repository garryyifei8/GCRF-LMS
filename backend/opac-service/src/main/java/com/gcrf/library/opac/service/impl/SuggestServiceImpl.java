package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.SuggestionVO;
import com.gcrf.library.opac.mapper.BookSearchMapper;
import com.gcrf.library.opac.service.SuggestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestServiceImpl implements SuggestService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final BookSearchMapper mapper;

    @Override
    public List<SuggestionVO> suggest(String q, int limit) {
        if (q == null || q.isBlank()) return Collections.emptyList();
        int safeLimit = (limit < 1 || limit > MAX_LIMIT) ? DEFAULT_LIMIT : limit;
        return mapper.suggestByTitle(q.trim(), safeLimit);
    }
}
