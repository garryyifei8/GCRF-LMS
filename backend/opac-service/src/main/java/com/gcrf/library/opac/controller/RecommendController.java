package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opac/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService service;

    @GetMapping("/related")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<List<BookSearchItemVO>> related(
            @RequestParam String isbn,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(service.related(isbn, limit));
    }
}
