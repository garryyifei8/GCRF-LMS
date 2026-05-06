package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import java.util.List;

public interface RecommendService {
    List<BookSearchItemVO> related(String isbn, int limit);
}
