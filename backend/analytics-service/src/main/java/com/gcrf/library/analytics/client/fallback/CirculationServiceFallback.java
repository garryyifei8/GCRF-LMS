package com.gcrf.library.analytics.client.fallback;

import com.gcrf.library.analytics.client.CirculationServiceClient;
import com.gcrf.library.analytics.client.dto.CirculationStatsDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 流通服务降级实现
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class CirculationServiceFallback implements CirculationServiceClient {

    @Override
    public Result<CirculationStatsDTO> getCirculationStats() {
        log.warn("流通服务不可用，获取流通统计降级");
        return Result.error("流通服务暂时不可用");
    }

    @Override
    public Result<Long> getCurrentBorrowedCount() {
        log.warn("流通服务不可用，获取当前借出数量降级");
        return Result.success(0L);
    }

    @Override
    public Result<Long> getOverdueCount() {
        log.warn("流通服务不可用，获取逾期数量降级");
        return Result.success(0L);
    }

    @Override
    public Result<Long> getTodayBorrowCount() {
        log.warn("流通服务不可用，获取今日借阅量降级");
        return Result.success(0L);
    }

    @Override
    public Result<Long> getBorrowCountInRange(String startDate, String endDate) {
        log.warn("流通服务不可用，获取指定范围借阅量降级: {} - {}", startDate, endDate);
        return Result.success(0L);
    }

    @Override
    public Result<Long> getActiveReservationCount() {
        log.warn("流通服务不可用，获取预约数量降级");
        return Result.success(0L);
    }
}
