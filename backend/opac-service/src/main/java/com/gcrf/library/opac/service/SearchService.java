package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;

public interface SearchService {

    /** Backward-compatible: no logging. */
    default PageVO<BookSearchItemVO> search(SearchRequest req) {
        return search(req, null);
    }

    /** Records the query into search_log asynchronously when clientIp is non-null. */
    PageVO<BookSearchItemVO> search(SearchRequest req, String clientIp);
}
