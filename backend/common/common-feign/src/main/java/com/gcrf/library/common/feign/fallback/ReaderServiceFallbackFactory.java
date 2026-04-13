package com.gcrf.library.common.feign.fallback;

import com.gcrf.library.common.feign.client.ReaderServiceClient;
import com.gcrf.library.common.feign.dto.ReaderDTO;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 读者服务Fallback工厂
 * 提供读者服务调用失败时的降级处理
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Component
public class ReaderServiceFallbackFactory implements FallbackFactory<ReaderServiceClient> {

    @Override
    public ReaderServiceClient create(Throwable cause) {
        log.error("读者服务调用失败，触发熔断降级: {}", cause.getMessage());

        return new ReaderServiceClient() {

            @Override
            public Result<ReaderDTO> getReaderById(Long readerId) {
                log.error("ReaderService.getReaderById降级: readerId={}, cause={}", readerId, cause.getMessage());
                return Result.error(503, "读者服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<ReaderDTO> getReaderByReaderId(String readerNo) {
                log.error("ReaderService.getReaderByReaderId降级: readerNo={}, cause={}", readerNo, cause.getMessage());
                return Result.error(503, "读者服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> canBorrow(Long readerId) {
                log.error("ReaderService.canBorrow降级: readerId={}, cause={}", readerId, cause.getMessage());
                // 降级策略：返回不可借阅，确保安全
                return Result.success(false);
            }

            @Override
            public Result<Integer> getCurrentBorrowCount(Long readerId) {
                log.error("ReaderService.getCurrentBorrowCount降级: readerId={}, cause={}", readerId, cause.getMessage());
                // 降级策略：返回最大值，防止继续借阅
                return Result.success(Integer.MAX_VALUE);
            }

            @Override
            public Result<Boolean> hasOverdueBooks(Long readerId) {
                log.error("ReaderService.hasOverdueBooks降级: readerId={}, cause={}", readerId, cause.getMessage());
                // 降级策略：假设有逾期图书，确保安全
                return Result.success(true);
            }

            @Override
            public Result<List<ReaderDTO>> getReadersByIds(String readerIds) {
                log.error("ReaderService.getReadersByIds降级: readerIds={}, cause={}", readerIds, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<PageResult<ReaderDTO>> queryReaders(String keyword, String readerType,
                                                              String status, Integer pageNum, Integer pageSize) {
                log.error("ReaderService.queryReaders降级: keyword={}, cause={}", keyword, cause.getMessage());
                PageResult<ReaderDTO> emptyResult = new PageResult<>();
                emptyResult.setRecords(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setPageNum(pageNum);
                emptyResult.setPageSize(pageSize);
                return Result.success(emptyResult);
            }

            @Override
            public Result<ReaderDTO> updateReaderStatus(Long readerId, String status) {
                log.error("ReaderService.updateReaderStatus降级: readerId={}, status={}, cause={}",
                        readerId, status, cause.getMessage());
                return Result.error(503, "更新读者状态失败，读者服务暂时不可用");
            }

            @Override
            public Result<String> health() {
                log.error("ReaderService.health降级: cause={}", cause.getMessage());
                return Result.error(503, "读者服务不可用");
            }
        };
    }
}
