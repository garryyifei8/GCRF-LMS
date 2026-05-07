package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.RankingItemVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService service;
    private final com.gcrf.library.opac.service.KeywordRankingService keywordRankingService;

    @GetMapping("/borrow")
    @RateLimit(value = 30, periodSeconds = 60)
    public Result<List<RankingItemVO>> borrow(
            @RequestParam(defaultValue = "THIS_MONTH") String range,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(service.borrowRanking(range, limit));
    }

    @org.springframework.web.bind.annotation.GetMapping("/keywords")
    @com.gcrf.library.opac.ratelimit.RateLimit(value = 30, periodSeconds = 60)
    public com.gcrf.library.common.result.Result<java.util.List<com.gcrf.library.opac.domain.vo.KeywordRankingVO>> keywords(
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit) {
        return com.gcrf.library.common.result.Result.success(
            keywordRankingService.topKeywords(days, limit));
    }
}
