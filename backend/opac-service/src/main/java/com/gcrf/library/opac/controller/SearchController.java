package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.NewArrivalsService;
import com.gcrf.library.opac.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final NewArrivalsService newArrivalsService;

    @GetMapping("/search")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<PageVO<BookSearchItemVO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String clc,
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        SearchRequest req = new SearchRequest();
        req.setQ(q);
        req.setClc(clc);
        req.setSchool(school);
        req.setPageNum(pageNum);
        req.setPageSize(pageSize);
        return Result.success(searchService.search(req));
    }

    @GetMapping("/new-arrivals")
    @RateLimit(value = 30, periodSeconds = 60)
    public Result<List<BookSearchItemVO>> newArrivals(
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(newArrivalsService.newArrivals(school, days, limit));
    }
}
