package com.gcrf.library.analytics.client;

import com.gcrf.library.analytics.client.dto.BookDTO;
import com.gcrf.library.analytics.client.dto.BookStatsDTO;
import com.gcrf.library.analytics.client.fallback.BookServiceFallback;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 图书服务Feign客户端
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@FeignClient(
        name = "book-service",
        fallback = BookServiceFallback.class
)
public interface BookServiceClient {

    /**
     * 获取图书详情
     */
    @GetMapping("/api/v1/books/{id}")
    Result<BookDTO> getBookById(@PathVariable("id") Long id);

    /**
     * 获取图书统计数据
     */
    @GetMapping("/api/v1/books/stats")
    Result<BookStatsDTO> getBookStats();

    /**
     * 获取图书总数
     */
    @GetMapping("/api/v1/books/count")
    Result<Long> getTotalBookCount();

    /**
     * 获取图书副本总数
     */
    @GetMapping("/api/v1/books/copies/count")
    Result<Long> getTotalCopiesCount();
}
