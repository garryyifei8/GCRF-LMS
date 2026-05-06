package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.RankingItemVO;
import java.util.List;

public interface RankingService {
    /** range: THIS_WEEK / THIS_MONTH / THIS_TERM (last 6 months) */
    List<RankingItemVO> borrowRanking(String range, int limit);
}
