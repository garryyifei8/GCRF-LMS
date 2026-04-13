package com.gcrf.library.common.feign.client;

import com.gcrf.library.common.feign.constant.FeignConstants;
import com.gcrf.library.common.feign.dto.BookDTO;
import com.gcrf.library.common.feign.fallback.BookServiceFallbackFactory;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 图书服务Feign客户端
 * 提供图书信息查询、库存管理等接口
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@FeignClient(
        name = FeignConstants.BOOK_SERVICE,
        path = FeignConstants.BOOK_API_PREFIX,
        fallbackFactory = BookServiceFallbackFactory.class
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
     * 根据ISBN获取图书信息
     *
     * @param isbn ISBN号
     * @return 图书信息
     */
    @GetMapping("/isbn/{isbn}")
    Result<BookDTO> getBookByIsbn(@PathVariable("isbn") String isbn);

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
     * 批量获取图书信息
     *
     * @param bookIds 图书ID列表（逗号分隔）
     * @return 图书信息列表
     */
    @GetMapping("/batch")
    Result<java.util.List<BookDTO>> getBooksByIds(@RequestParam("ids") String bookIds);

    /**
     * 分页查询图书
     *
     * @param keyword  关键词（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping
    Result<PageResult<BookDTO>> queryBooks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    Result<String> health();
}
