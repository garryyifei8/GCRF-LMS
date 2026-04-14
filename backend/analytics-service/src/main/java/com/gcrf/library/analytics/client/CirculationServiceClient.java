package com.gcrf.library.analytics.client;

import com.gcrf.library.analytics.client.dto.CirculationStatsDTO;
import com.gcrf.library.analytics.client.fallback.CirculationServiceFallback;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 流通服务Feign客户端
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@FeignClient(
        name = "circulation-service",
        fallback = CirculationServiceFallback.class
)
public interface CirculationServiceClient {

    /**
     * 获取流通统计数据
     */
    @GetMapping("/api/v1/circulation/stats")
    Result<CirculationStatsDTO> getCirculationStats();

    /**
     * 获取当前借出数量
     */
    @GetMapping("/api/v1/borrows/count/borrowed")
    Result<Long> getCurrentBorrowedCount();

    /**
     * 获取逾期数量
     */
    @GetMapping("/api/v1/borrows/count/overdue")
    Result<Long> getOverdueCount();

    /**
     * 获取今日借阅量
     */
    @GetMapping("/api/v1/borrows/count/today")
    Result<Long> getTodayBorrowCount();

    /**
     * 获取指定时间范围的借阅量
     */
    @GetMapping("/api/v1/borrows/count/range")
    Result<Long> getBorrowCountInRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    /**
     * 获取预约数量
     */
    @GetMapping("/api/v1/reserves/count/active")
    Result<Long> getActiveReservationCount();
}
