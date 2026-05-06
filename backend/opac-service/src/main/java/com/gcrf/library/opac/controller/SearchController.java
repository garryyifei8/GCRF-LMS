package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.dto.SearchRequest;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.domain.vo.PageVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.NewArrivalsService;
import com.gcrf.library.opac.service.SearchService;
import com.gcrf.library.opac.service.SuggestService;
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
    private final SuggestService suggestService;

    @org.springframework.web.bind.annotation.GetMapping("/search")
    @com.gcrf.library.opac.ratelimit.RateLimit(value = 10, periodSeconds = 1)
    public com.gcrf.library.common.result.Result<com.gcrf.library.opac.domain.vo.PageVO<com.gcrf.library.opac.domain.vo.BookSearchItemVO>> search(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String clc,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String school,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int pageNum,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int pageSize,
        jakarta.servlet.http.HttpServletRequest request) {
        com.gcrf.library.opac.domain.dto.SearchRequest req = new com.gcrf.library.opac.domain.dto.SearchRequest();
        req.setQ(q); req.setClc(clc); req.setSchool(school);
        req.setPageNum(pageNum); req.setPageSize(pageSize);
        String clientIp = clientIp(request);
        return com.gcrf.library.common.result.Result.success(searchService.search(req, clientIp));
    }

    private String clientIp(jakarta.servlet.http.HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    @GetMapping("/new-arrivals")
    @RateLimit(value = 30, periodSeconds = 60)
    public Result<List<BookSearchItemVO>> newArrivals(
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(newArrivalsService.newArrivals(school, days, limit));
    }

    @GetMapping("/suggest")
    @RateLimit(value = 30, periodSeconds = 1)
    public Result<List<com.gcrf.library.opac.domain.vo.SuggestionVO>> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(suggestService.suggest(q, limit));
    }
}
