package com.gcrf.library.circulation.client.fallback;

import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读者服务Feign降级处理
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Component
public class ReaderServiceFallback implements ReaderServiceClient {

    @Override
    public Result<ReaderDTO> getReaderById(Long readerId) {
        log.error("ReaderService调用失败 - getReaderById, readerId: {}", readerId);
        return Result.error(500, "读者服务暂时不可用,请稍后重试");
    }

    @Override
    public Result<ReaderDTO> getReaderByCardId(String readerCardId) {
        log.error("ReaderService调用失败 - getReaderByCardId, readerCardId: {}", readerCardId);
        return Result.error(500, "读者服务暂时不可用,请稍后重试");
    }

    @Override
    public Result<Boolean> validateReaderStatus(Long readerId) {
        log.error("ReaderService调用失败 - validateReaderStatus, readerId: {}", readerId);
        // 降级策略：返回不可借，确保安全
        return Result.success(false);
    }

    @Override
    public Result<Boolean> hasOverdueBooks(Long readerId) {
        log.error("ReaderService调用失败 - hasOverdueBooks, readerId: {}", readerId);
        // 降级策略：假设有逾期，拒绝借书
        return Result.success(true);
    }

    @Override
    public Result<Boolean> hasUnpaidFine(Long readerId) {
        log.error("ReaderService调用失败 - hasUnpaidFine, readerId: {}", readerId);
        // 降级策略：假设有未支付罚金，拒绝借书
        return Result.success(true);
    }
}
