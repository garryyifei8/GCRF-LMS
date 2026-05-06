package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import java.util.List;

public interface NewArrivalsService {
    List<BookSearchItemVO> newArrivals(String school, int days, int limit);
}
