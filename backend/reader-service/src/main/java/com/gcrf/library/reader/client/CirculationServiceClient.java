package com.gcrf.library.reader.client;

import com.gcrf.library.reader.client.dto.BorrowHistoryDTO;
import com.gcrf.library.reader.client.fallback.CirculationServiceFallback;
import com.gcrf.library.common.result.PageResult;
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
        fallback = CirculationServiceFallback.class,
        path = "/api/v1/borrows"
)
public interface CirculationServiceClient {

    /**
     * 根据读者ID查询借阅记录
     *
     * @param readerId 读者ID
     * @param status 状态过滤（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 借阅记录分页结果
     */
    @GetMapping
    Result<PageResult<BorrowHistoryDTO>> getBorrowsByReaderId(
            @RequestParam("readerId") Long readerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 获取读者当前借阅中的图书数量
     *
     * @param readerId 读者ID
     * @return 借阅中的图书数量
     */
    @GetMapping("/count")
    Result<Integer> getCurrentBorrowCount(@RequestParam("readerId") Long readerId);
}
