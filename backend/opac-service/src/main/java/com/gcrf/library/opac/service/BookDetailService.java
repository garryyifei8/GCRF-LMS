package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookDetailVO;

public interface BookDetailService {
    /** Returns null if the ISBN isn't found in any school. */
    BookDetailVO getByIsbn(String isbn);
}
