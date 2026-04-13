package com.gcrf.library.common.feign.client;

import com.gcrf.library.common.feign.constant.FeignConstants;
import com.gcrf.library.common.feign.dto.ReaderDTO;
import com.gcrf.library.common.feign.fallback.ReaderServiceFallbackFactory;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 读者服务Feign客户端
 * 提供读者信息查询、状态管理等接口
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@FeignClient(
        name = FeignConstants.READER_SERVICE,
        path = FeignConstants.READER_API_PREFIX,
        fallbackFactory = ReaderServiceFallbackFactory.class
)
public interface ReaderServiceClient {

    /**
     * 根据ID获取读者信息
     *
     * @param readerId 读者ID（主键）
     * @return 读者信息
     */
    @GetMapping("/{readerId}")
    Result<ReaderDTO> getReaderById(@PathVariable("readerId") Long readerId);

    /**
     * 根据读者证号获取读者信息
     *
     * @param readerNo 读者证号
     * @return 读者信息
     */
    @GetMapping("/readerId/{readerNo}")
    Result<ReaderDTO> getReaderByReaderId(@PathVariable("readerNo") String readerNo);

    /**
     * 检查读者是否可以借阅
     * 检查条件：状态正常、未过期、借阅数量未达上限、无逾期图书
     *
     * @param readerId 读者ID
     * @return 是否可借阅
     */
    @GetMapping("/{readerId}/can-borrow")
    Result<Boolean> canBorrow(@PathVariable("readerId") Long readerId);

    /**
     * 获取读者当前借阅数量
     *
     * @param readerId 读者ID
     * @return 当前借阅数量
     */
    @GetMapping("/{readerId}/borrow-count")
    Result<Integer> getCurrentBorrowCount(@PathVariable("readerId") Long readerId);

    /**
     * 检查读者是否有逾期图书
     *
     * @param readerId 读者ID
     * @return 是否有逾期图书
     */
    @GetMapping("/{readerId}/has-overdue")
    Result<Boolean> hasOverdueBooks(@PathVariable("readerId") Long readerId);

    /**
     * 批量获取读者信息
     *
     * @param readerIds 读者ID列表（逗号分隔）
     * @return 读者信息列表
     */
    @GetMapping("/batch")
    Result<java.util.List<ReaderDTO>> getReadersByIds(@RequestParam("ids") String readerIds);

    /**
     * 分页查询读者
     *
     * @param keyword    关键词（可选）
     * @param readerType 读者类型（可选）
     * @param status     状态（可选）
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @return 分页结果
     */
    @GetMapping
    Result<PageResult<ReaderDTO>> queryReaders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "readerType", required = false) String readerType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 更新读者状态
     *
     * @param readerId 读者ID
     * @param status   新状态
     * @return 更新后的读者信息
     */
    @PutMapping("/{readerId}/status")
    Result<ReaderDTO> updateReaderStatus(
            @PathVariable("readerId") Long readerId,
            @RequestParam("status") String status
    );

    /**
     * 健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    Result<String> health();
}
