package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.KeywordRankingVO;
import java.util.List;

public interface KeywordRankingService {
    List<KeywordRankingVO> topKeywords(int days, int limit);
}
