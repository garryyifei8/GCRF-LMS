package com.gcrf.library.analytics.client.fallback;

import com.gcrf.library.analytics.client.ReaderServiceClient;
import com.gcrf.library.analytics.client.dto.ReaderDTO;
import com.gcrf.library.analytics.client.dto.ReaderStatsDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读者服务降级实现
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class ReaderServiceFallback implements ReaderServiceClient {

    @Override
    public Result<ReaderDTO> getReaderById(Long id) {
        log.warn("读者服务不可用，获取读者详情降级: id={}", id);
        return Result.error("读者服务暂时不可用");
    }

    @Override
    public Result<ReaderStatsDTO> getReaderStats() {
        log.warn("读者服务不可用，获取读者统计降级");
        return Result.error("读者服务暂时不可用");
    }

    @Override
    public Result<Long> getTotalReaderCount() {
        log.warn("读者服务不可用，获取读者总数降级");
        return Result.success(0L);
    }

    @Override
    public Result<Long> getTodayNewReaderCount() {
        log.warn("读者服务不可用，获取今日新增读者降级");
        return Result.success(0L);
    }
}
