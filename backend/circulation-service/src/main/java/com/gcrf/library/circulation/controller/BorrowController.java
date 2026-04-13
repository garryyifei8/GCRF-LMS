package com.gcrf.library.circulation.controller;

import com.gcrf.library.circulation.dto.BorrowQueryRequest;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.dto.RenewRequest;
import com.gcrf.library.circulation.dto.ReturnRequest;
import com.gcrf.library.circulation.dto.response.BorrowDetailVO;
import com.gcrf.library.circulation.dto.response.BorrowVO;
import com.gcrf.library.circulation.service.BorrowService;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 借阅管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "借阅管理", description = "图书借阅、归还、续借相关接口")
@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 分页查询借阅记录
     */
    @Operation(summary = "分页查询借阅记录", description = "根据条件分页查询借阅记录")
    @GetMapping
    public Result<PageResult<BorrowVO>> queryBorrows(@Valid BorrowQueryRequest request) {
        log.info("分页查询借阅记录请求: readerId={}, bookId={}, status={}",
                 request.getReaderId(), request.getBookId(), request.getStatus());
        PageResult<BorrowVO> result = borrowService.queryBorrows(request);
        return Result.success(result);
    }

    /**
     * 根据ID获取借阅详情
     */
    @Operation(summary = "根据ID获取借阅详情", description = "根据借阅记录ID查询详细信息")
    @Parameter(name = "id", description = "借阅记录ID", required = true)
    @GetMapping("/{id}")
    public Result<BorrowDetailVO> getBorrowById(@PathVariable Long id) {
        log.info("查询借阅详情: id={}", id);
        BorrowDetailVO result = borrowService.getBorrowById(id);
        return Result.success(result);
    }

    /**
     * 借书
     */
    @Operation(summary = "借书", description = "创建借阅记录，减少图书库存")
    @PostMapping("/borrow")
    public Result<BorrowDetailVO> borrowBook(@Valid @RequestBody BorrowRequest request) {
        log.info("借书请求: bookId={}, readerId={}", request.getBookId(), request.getReaderId());
        BorrowDetailVO result = borrowService.borrowBook(request);
        return Result.success(result);
    }

    /**
     * 还书
     */
    @Operation(summary = "还书", description = "归还图书，增加图书库存，计算罚金")
    @PostMapping("/return")
    public Result<BorrowDetailVO> returnBook(@Valid @RequestBody ReturnRequest request) {
        log.info("还书请求: borrowId={}, payFine={}", request.getBorrowId(), request.getPayFine());
        BorrowDetailVO result = borrowService.returnBook(request);
        return Result.success(result);
    }

    /**
     * 续借
     */
    @Operation(summary = "续借", description = "延长借阅期限")
    @PostMapping("/renew")
    public Result<BorrowDetailVO> renewBook(@Valid @RequestBody RenewRequest request) {
        log.info("续借请求: borrowId={}, renewDays={}", request.getBorrowId(), request.getRenewDays());
        BorrowDetailVO result = borrowService.renewBook(request);
        return Result.success(result);
    }

    /**
     * 获取逾期借阅记录
     */
    @Operation(summary = "获取逾期借阅记录", description = "查询所有已逾期且未归还的借阅记录")
    @GetMapping("/overdue")
    public Result<List<BorrowVO>> getOverdueBorrows() {
        log.info("查询逾期借阅记录");
        List<BorrowVO> result = borrowService.getOverdueBorrows();
        return Result.success(result);
    }

    /**
     * 更新逾期状态（定时任务调用）
     */
    @Operation(summary = "更新逾期状态", description = "批量更新已超过应还日期的记录状态为OVERDUE")
    @PostMapping("/update-overdue-status")
    public Result<Void> updateOverdueStatus() {
        log.info("批量更新逾期状态");
        borrowService.updateOverdueStatus();
        return Result.success();
    }

    /**
     * 批量还书
     */
    @Operation(summary = "批量还书", description = "批量归还图书，返回每本书的归还结果")
    @PostMapping("/batch-return")
    public Result<List<Map<String, Object>>> batchReturn(@RequestBody Map<String, Object> request) {
        log.info("批量还书请求");
        @SuppressWarnings("unchecked")
        List<?> rawIds = (List<?>) request.get("borrowIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return Result.success(List.of());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Object rawId : rawIds) {
            Long borrowId = Long.valueOf(rawId.toString());
            try {
                ReturnRequest returnRequest = new ReturnRequest();
                returnRequest.setBorrowId(borrowId);
                borrowService.returnBook(returnRequest);
                results.add(Map.of("borrowId", borrowId, "success", true));
            } catch (Exception e) {
                results.add(Map.of("borrowId", borrowId, "success", false, "error", e.getMessage()));
            }
        }
        return Result.success(results);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查借阅服务是否正常运行")
    public Result<String> health() {
        return Result.success("Circulation Service (Borrow) is running");
    }
}
