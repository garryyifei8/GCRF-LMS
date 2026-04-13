package com.gcrf.library.common.feign.fallback;

import com.gcrf.library.common.feign.client.BookServiceClient;
import com.gcrf.library.common.feign.dto.BookDTO;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 图书服务Fallback工厂
 * 提供图书服务调用失败时的降级处理
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Component
public class BookServiceFallbackFactory implements FallbackFactory<BookServiceClient> {

    @Override
    public BookServiceClient create(Throwable cause) {
        log.error("图书服务调用失败，触发熔断降级: {}", cause.getMessage());

        return new BookServiceClient() {

            @Override
            public Result<BookDTO> getBookById(Long bookId) {
                log.error("BookService.getBookById降级: bookId={}, cause={}", bookId, cause.getMessage());
                return Result.error(503, "图书服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<BookDTO> getBookByIsbn(String isbn) {
                log.error("BookService.getBookByIsbn降级: isbn={}, cause={}", isbn, cause.getMessage());
                return Result.error(503, "图书服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> checkAvailability(Long bookId) {
                log.error("BookService.checkAvailability降级: bookId={}, cause={}", bookId, cause.getMessage());
                // 降级策略：返回不可借，确保安全
                return Result.success(false);
            }

            @Override
            public Result<Void> decreaseAvailableCopies(Long bookId) {
                log.error("BookService.decreaseAvailableCopies降级: bookId={}, cause={}", bookId, cause.getMessage());
                return Result.error(503, "减少图书库存失败，图书服务暂时不可用");
            }

            @Override
            public Result<Void> increaseAvailableCopies(Long bookId) {
                log.error("BookService.increaseAvailableCopies降级: bookId={}, cause={}", bookId, cause.getMessage());
                return Result.error(503, "增加图书库存失败，图书服务暂时不可用");
            }

            @Override
            public Result<List<BookDTO>> getBooksByIds(String bookIds) {
                log.error("BookService.getBooksByIds降级: bookIds={}, cause={}", bookIds, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<PageResult<BookDTO>> queryBooks(String keyword, Integer pageNum, Integer pageSize) {
                log.error("BookService.queryBooks降级: keyword={}, cause={}", keyword, cause.getMessage());
                PageResult<BookDTO> emptyResult = new PageResult<>();
                emptyResult.setRecords(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setPageNum(pageNum);
                emptyResult.setPageSize(pageSize);
                return Result.success(emptyResult);
            }

            @Override
            public Result<String> health() {
                log.error("BookService.health降级: cause={}", cause.getMessage());
                return Result.error(503, "图书服务不可用");
            }
        };
    }
}
