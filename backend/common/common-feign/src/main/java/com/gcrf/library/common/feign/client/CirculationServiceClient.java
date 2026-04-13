package com.gcrf.library.common.feign.client;

import com.gcrf.library.common.feign.constant.FeignConstants;
import com.gcrf.library.common.feign.dto.CirculationRecordDTO;
import com.gcrf.library.common.feign.fallback.CirculationServiceFallbackFactory;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流通服务Feign客户端
 * 提供借阅记录查询、借还书操作等接口
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@FeignClient(
        name = FeignConstants.CIRCULATION_SERVICE,
        path = FeignConstants.CIRCULATION_API_PREFIX,
        fallbackFactory = CirculationServiceFallbackFactory.class
)
public interface CirculationServiceClient {

    /**
     * 获取借阅记录详情
     *
     * @param recordId 记录ID
     * @return 借阅记录
     */
    @GetMapping("/{recordId}")
    Result<CirculationRecordDTO> getRecordById(@PathVariable("recordId") Long recordId);

    /**
     * 查询读者借阅记录
     *
     * @param readerId 读者ID
     * @param status   状态过滤（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 借阅记录分页结果
     */
    @GetMapping("/reader/{readerId}")
    Result<PageResult<CirculationRecordDTO>> getReaderRecords(
            @PathVariable("readerId") Long readerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 查询图书借阅记录
     *
     * @param bookId   图书ID
     * @param status   状态过滤（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 借阅记录分页结果
     */
    @GetMapping("/book/{bookId}")
    Result<PageResult<CirculationRecordDTO>> getBookRecords(
            @PathVariable("bookId") Long bookId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    );

    /**
     * 获取读者当前借阅中的图书数量
     *
     * @param readerId 读者ID
     * @return 借阅中的图书数量
     */
    @GetMapping("/reader/{readerId}/borrow-count")
    Result<Integer> getCurrentBorrowCount(@PathVariable("readerId") Long readerId);

    /**
     * 检查读者是否有逾期图书
     *
     * @param readerId 读者ID
     * @return 是否有逾期图书
     */
    @GetMapping("/reader/{readerId}/has-overdue")
    Result<Boolean> hasOverdueBooks(@PathVariable("readerId") Long readerId);

    /**
     * 获取读者所有逾期记录
     *
     * @param readerId 读者ID
     * @return 逾期记录列表
     */
    @GetMapping("/reader/{readerId}/overdue")
    Result<List<CirculationRecordDTO>> getOverdueRecords(@PathVariable("readerId") Long readerId);

    /**
     * 检查图书是否被借出
     *
     * @param bookId 图书ID
     * @return 是否被借出
     */
    @GetMapping("/book/{bookId}/is-borrowed")
    Result<Boolean> isBookBorrowed(@PathVariable("bookId") Long bookId);

    /**
     * 获取图书当前借阅记录（如果有）
     *
     * @param bookId 图书ID
     * @return 当前借阅记录（如果存在）
     */
    @GetMapping("/book/{bookId}/current")
    Result<CirculationRecordDTO> getCurrentBorrowRecord(@PathVariable("bookId") Long bookId);

    /**
     * 统计借阅数量
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 借阅总数
     */
    @GetMapping("/statistics/count")
    Result<Long> countBorrows(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    );

    /**
     * 健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    Result<String> health();
}
