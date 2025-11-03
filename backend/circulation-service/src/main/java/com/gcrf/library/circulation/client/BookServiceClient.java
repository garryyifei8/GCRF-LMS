package com.gcrf.library.circulation.client;

import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.fallback.BookServiceFallback;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 图书服务Feign客户端
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@FeignClient(
        name = "book-service",
        fallback = BookServiceFallback.class,
        path = "/api/v1/books"
)
public interface BookServiceClient {

    /**
     * 根据ID获取图书信息
     *
     * @param bookId 图书ID
     * @return 图书信息
     */
    @GetMapping("/{bookId}")
    Result<BookDTO> getBookById(@PathVariable("bookId") Long bookId);

    /**
     * 检查图书可借状态
     *
     * @param bookId 图书ID
     * @return 是否可借
     */
    @GetMapping("/{bookId}/availability")
    Result<Boolean> checkAvailability(@PathVariable("bookId") Long bookId);

    /**
     * 减少图书可借数量（借书时调用）
     *
     * @param bookId 图书ID
     * @return 操作结果
     */
    @PostMapping("/{bookId}/decrease-copies")
    Result<Void> decreaseAvailableCopies(@PathVariable("bookId") Long bookId);

    /**
     * 增加图书可借数量（还书时调用）
     *
     * @param bookId 图书ID
     * @return 操作结果
     */
    @PostMapping("/{bookId}/increase-copies")
    Result<Void> increaseAvailableCopies(@PathVariable("bookId") Long bookId);

    /**
     * 根据条码获取图书信息
     *
     * @param barcode 图书条码
     * @return 图书信息
     */
    @GetMapping("/barcode/{barcode}")
    Result<BookDTO> getBookByBarcode(@PathVariable("barcode") String barcode);
}
