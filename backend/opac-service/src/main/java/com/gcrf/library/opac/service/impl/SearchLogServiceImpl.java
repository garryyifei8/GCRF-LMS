package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.mapper.SearchLogMapper;
import com.gcrf.library.opac.service.SearchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogServiceImpl implements SearchLogService {

    private final SearchLogMapper mapper;

    @Override
    @Async("opacAsyncExecutor")
    public void recordAsync(String keyword, String clientIp, long resultCount) {
        if (keyword == null || keyword.isBlank()) return;
        try {
            String trimmed = keyword.trim();
            // truncate over-long keywords defensively (TEXT can hold any length but logging huge keys is wasteful)
            if (trimmed.length() > 200) trimmed = trimmed.substring(0, 200);
            mapper.insert(trimmed, clientIp, resultCount);
        } catch (Exception e) {
            log.warn("search_log insert failed: {}", e.getMessage());
        }
    }
}
