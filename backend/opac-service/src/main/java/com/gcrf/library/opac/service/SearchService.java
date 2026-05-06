package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;

public interface SearchService {
    PageVO<BookSearchItemVO> search(SearchRequest req);
}
