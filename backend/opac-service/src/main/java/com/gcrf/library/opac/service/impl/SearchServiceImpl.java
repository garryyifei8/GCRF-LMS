package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.mapper.BookSearchMapper;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final BookSearchMapper mapper;

    @Override
    public PageVO<BookSearchItemVO> search(SearchRequest req) {
        int pageNum = Math.max(1, req.getPageNum());
        int pageSize = Math.max(1, Math.min(100, req.getPageSize()));
        int offset = (pageNum - 1) * pageSize;
        List<BookSearchItemVO> records =
            mapper.search(req.getQ(), req.getClc(), req.getSchool(), pageSize, offset);
        long total = mapper.count(req.getQ(), req.getClc(), req.getSchool());
        return new PageVO<>(records, total, pageNum, pageSize);
    }
}
