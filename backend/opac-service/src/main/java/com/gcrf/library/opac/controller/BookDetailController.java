package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.domain.vo.BookDetailVO;
import com.gcrf.library.opac.ratelimit.RateLimit;
import com.gcrf.library.opac.service.BookDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opac/books")
@RequiredArgsConstructor
public class BookDetailController {

    private final BookDetailService service;

    @GetMapping("/{isbn}")
    @RateLimit(value = 10, periodSeconds = 1)
    public Result<BookDetailVO> getByIsbn(@PathVariable String isbn) {
        return Result.success(service.getByIsbn(isbn));
    }
}
