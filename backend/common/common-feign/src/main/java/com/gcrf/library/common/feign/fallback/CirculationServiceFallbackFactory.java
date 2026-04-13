package com.gcrf.library.common.feign.fallback;

import com.gcrf.library.common.feign.client.CirculationServiceClient;
import com.gcrf.library.common.feign.dto.CirculationRecordDTO;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 流通服务Fallback工厂
 * 提供流通服务调用失败时的降级处理
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Component
public class CirculationServiceFallbackFactory implements FallbackFactory<CirculationServiceClient> {

    @Override
    public CirculationServiceClient create(Throwable cause) {
        log.error("流通服务调用失败，触发熔断降级: {}", cause.getMessage());

        return new CirculationServiceClient() {

            @Override
            public Result<CirculationRecordDTO> getRecordById(Long recordId) {
                log.error("CirculationService.getRecordById降级: recordId={}, cause={}", recordId, cause.getMessage());
                return Result.error(503, "流通服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<PageResult<CirculationRecordDTO>> getReaderRecords(Long readerId, String status,
                                                                              Integer pageNum, Integer pageSize) {
                log.error("CirculationService.getReaderRecords降级: readerId={}, cause={}", readerId, cause.getMessage());
                PageResult<CirculationRecordDTO> emptyResult = new PageResult<>();
                emptyResult.setRecords(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setPageNum(pageNum);
                emptyResult.setPageSize(pageSize);
                return Result.success(emptyResult);
            }

            @Override
            public Result<PageResult<CirculationRecordDTO>> getBookRecords(Long bookId, String status,
                                                                            Integer pageNum, Integer pageSize) {
                log.error("CirculationService.getBookRecords降级: bookId={}, cause={}", bookId, cause.getMessage());
                PageResult<CirculationRecordDTO> emptyResult = new PageResult<>();
                emptyResult.setRecords(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setPageNum(pageNum);
                emptyResult.setPageSize(pageSize);
                return Result.success(emptyResult);
            }

            @Override
            public Result<Integer> getCurrentBorrowCount(Long readerId) {
                log.error("CirculationService.getCurrentBorrowCount降级: readerId={}, cause={}", readerId, cause.getMessage());
                // 降级策略：返回最大值，防止继续借阅
                return Result.success(Integer.MAX_VALUE);
            }

            @Override
            public Result<Boolean> hasOverdueBooks(Long readerId) {
                log.error("CirculationService.hasOverdueBooks降级: readerId={}, cause={}", readerId, cause.getMessage());
                // 降级策略：假设有逾期图书，确保安全
                return Result.success(true);
            }

            @Override
            public Result<List<CirculationRecordDTO>> getOverdueRecords(Long readerId) {
                log.error("CirculationService.getOverdueRecords降级: readerId={}, cause={}", readerId, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Boolean> isBookBorrowed(Long bookId) {
                log.error("CirculationService.isBookBorrowed降级: bookId={}, cause={}", bookId, cause.getMessage());
                // 降级策略：假设已借出，确保安全
                return Result.success(true);
            }

            @Override
            public Result<CirculationRecordDTO> getCurrentBorrowRecord(Long bookId) {
                log.error("CirculationService.getCurrentBorrowRecord降级: bookId={}, cause={}", bookId, cause.getMessage());
                return Result.error(503, "流通服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Long> countBorrows(String startDate, String endDate) {
                log.error("CirculationService.countBorrows降级: startDate={}, endDate={}, cause={}",
                        startDate, endDate, cause.getMessage());
                return Result.success(0L);
            }

            @Override
            public Result<String> health() {
                log.error("CirculationService.health降级: cause={}", cause.getMessage());
                return Result.error(503, "流通服务不可用");
            }
        };
    }
}
