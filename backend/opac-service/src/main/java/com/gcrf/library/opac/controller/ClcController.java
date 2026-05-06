package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.ClcService;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/clc")
@RequiredArgsConstructor
public class ClcController {

    private final ClcService clc;
    private final SearchService search;

    @GetMapping("/tree")
    @RateLimit(value = 1, periodSeconds = 60)
    public Result<List<ClcNodeVO>> tree() {
        return Result.success(clc.getTree());
    }

    @GetMapping("/{code}/books")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<PageVO<BookSearchItemVO>> browse(
            @PathVariable String code,
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        SearchRequest req = new SearchRequest();
        req.setClc(code);
        req.setSchool(school);
        req.setPageNum(pageNum);
        req.setPageSize(pageSize);
        return Result.success(search.search(req));
    }
}
