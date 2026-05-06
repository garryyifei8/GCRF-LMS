package com.gcrf.library.opac.service.impl;

import com.gcrf.library.opac.service.SearchMviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchMviewServiceImpl implements SearchMviewService {

    private final JdbcTemplate jdbc;

    @Value("${gcrf.opac.search-mview.auto-refresh-on-startup:true}")
    private boolean autoRefreshOnStartup;

    @Override
    public int refresh() {
        Integer rows = jdbc.queryForObject(
            "SELECT gcrf_region.refresh_book_search_mview()", Integer.class);
        int count = rows == null ? 0 : rows;
        log.info("OPAC search mview refreshed: {} rows", count);
        return count;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (autoRefreshOnStartup) {
            try {
                refresh();
            } catch (Exception e) {
                log.warn("startup mview refresh failed: {}", e.getMessage());
            }
        }
    }
}
