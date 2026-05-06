package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.SuggestionVO;
import java.util.List;

public interface SuggestService {
    /** Empty/blank query → empty list. limit clamped to [1, 50] (default 10). */
    List<SuggestionVO> suggest(String q, int limit);
}
