package com.gcrf.library.analytics.client.fallback;

import com.gcrf.library.analytics.client.BookServiceClient;
import com.gcrf.library.analytics.client.dto.BookDTO;
import com.gcrf.library.analytics.client.dto.BookStatsDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图书服务降级实现
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class BookServiceFallback implements BookServiceClient {

    @Override
    public Result<BookDTO> getBookById(Long id) {
        log.warn("图书服务不可用，获取图书详情降级: id={}", id);
        return Result.error("图书服务暂时不可用");
    }

    @Override
    public Result<BookStatsDTO> getBookStats() {
        log.warn("图书服务不可用，获取图书统计降级");
        return Result.error("图书服务暂时不可用");
    }

    @Override
    public Result<Long> getTotalBookCount() {
        log.warn("图书服务不可用，获取图书总数降级");
        return Result.success(0L);
    }

    @Override
    public Result<Long> getTotalCopiesCount() {
        log.warn("图书服务不可用，获取副本总数降级");
        return Result.success(0L);
    }
}
