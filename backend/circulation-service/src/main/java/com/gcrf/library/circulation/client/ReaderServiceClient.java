package com.gcrf.library.circulation.client;

import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.client.fallback.ReaderServiceFallback;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 读者服务Feign客户端
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@FeignClient(
        name = "reader-service",
        fallback = ReaderServiceFallback.class,
        path = "/api/v1/readers"
)
public interface ReaderServiceClient {

    /**
     * 根据ID获取读者信息
     *
     * @param readerId 读者ID
     * @return 读者信息
     */
    @GetMapping("/{readerId}")
    Result<ReaderDTO> getReaderById(@PathVariable("readerId") Long readerId);

    /**
     * 根据读者证号获取读者信息
     *
     * @param readerCardId 读者证号
     * @return 读者信息
     */
    @GetMapping("/card/{readerCardId}")
    Result<ReaderDTO> getReaderByCardId(@PathVariable("readerCardId") String readerCardId);

    /**
     * 验证读者状态（是否可以借书）
     *
     * @param readerId 读者ID
     * @return 是否可以借书
     */
    @GetMapping("/{readerId}/validate-status")
    Result<Boolean> validateReaderStatus(@PathVariable("readerId") Long readerId);

    /**
     * 检查读者是否有逾期未还图书
     *
     * @param readerId 读者ID
     * @return 是否有逾期
     */
    @GetMapping("/{readerId}/has-overdue")
    Result<Boolean> hasOverdueBooks(@PathVariable("readerId") Long readerId);

    /**
     * 检查读者是否有未支付罚金
     *
     * @param readerId 读者ID
     * @return 是否有未支付罚金
     */
    @GetMapping("/{readerId}/has-unpaid-fine")
    Result<Boolean> hasUnpaidFine(@PathVariable("readerId") Long readerId);
}
