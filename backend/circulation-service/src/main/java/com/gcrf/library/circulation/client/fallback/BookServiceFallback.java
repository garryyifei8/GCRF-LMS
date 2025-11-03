package com.gcrf.library.circulation.client.fallback;

import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图书服务Feign降级处理
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Component
public class BookServiceFallback implements BookServiceClient {

    @Override
    public Result<BookDTO> getBookById(Long bookId) {
        log.error("BookService调用失败 - getBookById, bookId: {}", bookId);
        return Result.error(500, "图书服务暂时不可用,请稍后重试");
    }

    @Override
    public Result<Boolean> checkAvailability(Long bookId) {
        log.error("BookService调用失败 - checkAvailability, bookId: {}", bookId);
        // 降级策略：返回不可借，确保安全
        return Result.success(false);
    }

    @Override
    public Result<Void> decreaseAvailableCopies(Long bookId) {
        log.error("BookService调用失败 - decreaseAvailableCopies, bookId: {}", bookId);
        return Result.error(500, "减少图书库存失败,请稍后重试");
    }

    @Override
    public Result<Void> increaseAvailableCopies(Long bookId) {
        log.error("BookService调用失败 - increaseAvailableCopies, bookId: {}", bookId);
        return Result.error(500, "增加图书库存失败,请稍后重试");
    }

    @Override
    public Result<BookDTO> getBookByBarcode(String barcode) {
        log.error("BookService调用失败 - getBookByBarcode, barcode: {}", barcode);
        return Result.error(500, "图书服务暂时不可用,请稍后重试");
    }
}
