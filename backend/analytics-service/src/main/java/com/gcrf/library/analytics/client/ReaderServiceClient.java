package com.gcrf.library.analytics.client;

import com.gcrf.library.analytics.client.dto.ReaderDTO;
import com.gcrf.library.analytics.client.dto.ReaderStatsDTO;
import com.gcrf.library.analytics.client.fallback.ReaderServiceFallback;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 读者服务Feign客户端
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@FeignClient(
        name = "reader-service",
        fallback = ReaderServiceFallback.class
)
public interface ReaderServiceClient {

    /**
     * 获取读者详情
     */
    @GetMapping("/api/v1/readers/{id}")
    Result<ReaderDTO> getReaderById(@PathVariable("id") Long id);

    /**
     * 获取读者统计数据
     */
    @GetMapping("/api/v1/readers/stats")
    Result<ReaderStatsDTO> getReaderStats();

    /**
     * 获取读者总数
     */
    @GetMapping("/api/v1/readers/count")
    Result<Long> getTotalReaderCount();

    /**
     * 获取今日新增读者数
     */
    @GetMapping("/api/v1/readers/count/today")
    Result<Long> getTodayNewReaderCount();
}
