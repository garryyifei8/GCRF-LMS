package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.vo.KeywordRankingVO;
import com.gcrf.library.opac.mapper.SearchLogMapper;
import com.gcrf.library.opac.service.KeywordRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class KeywordRankingServiceImpl implements KeywordRankingService {

    private final SearchLogMapper mapper;

    @Override
    public List<KeywordRankingVO> topKeywords(int days, int limit) {
        int safeDays = Math.max(1, Math.min(365, days));
        int safeLimit = Math.max(1, Math.min(100, limit));
        List<KeywordRankingVO> rows = mapper.topKeywords(safeDays, safeLimit);
        AtomicInteger rank = new AtomicInteger(1);
        rows.forEach(r -> r.setRank(rank.getAndIncrement()));
        return rows;
    }
}
