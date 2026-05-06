package com.gcrf.library.opac.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.opac.service.SearchMviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opac/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SearchMviewService mview;

    @PostMapping("/refresh-search-mview")
    public Result<Integer> refresh() {
        return Result.success(mview.refresh());
    }
}
