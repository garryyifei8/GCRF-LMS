package com.gcrf.library.reader.client.fallback;

import com.gcrf.library.reader.client.CirculationServiceClient;
import com.gcrf.library.reader.client.dto.BorrowHistoryDTO;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 流通服务Feign降级处理
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
public class CirculationServiceFallback implements CirculationServiceClient {

    @Override
    public Result<PageResult<BorrowHistoryDTO>> getBorrowsByReaderId(
            Long readerId, String status, Integer pageNum, Integer pageSize) {
        log.error("CirculationService调用失败 - getBorrowsByReaderId, readerId: {}", readerId);

        // 降级策略：返回空结果
        PageResult<BorrowHistoryDTO> emptyResult = PageResult.of(
                0L, pageNum, pageSize, Collections.emptyList()
        );
        return Result.success(emptyResult);
    }

    @Override
    public Result<Integer> getCurrentBorrowCount(Long readerId) {
        log.error("CirculationService调用失败 - getCurrentBorrowCount, readerId: {}", readerId);
        // 降级策略：返回0，不影响业务流程
        return Result.success(0);
    }
}
