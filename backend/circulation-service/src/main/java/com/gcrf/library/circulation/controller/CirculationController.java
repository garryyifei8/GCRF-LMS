package com.gcrf.library.circulation.controller;

import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.entity.CirculationRecord;
import com.gcrf.library.circulation.service.CirculationService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流通控制器
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/circulation")
@RequiredArgsConstructor
@Tag(name = "流通管理", description = "图书借阅、归还、续借等接口")
public class CirculationController {

    private final CirculationService circulationService;

    /**
     * 借阅图书
     */
    @PostMapping("/borrow")
    @Operation(summary = "借阅图书", description = "创建借阅记录并减少图书库存")
    public Result<CirculationRecord> borrowBook(@Validated @RequestBody BorrowRequest request) {
        log.info("借阅图书请求: {}", request);
        CirculationRecord record = circulationService.borrowBook(request);
        return Result.success(record);
    }

    /**
     * 归还图书
     */
    @PostMapping("/return/{recordId}")
    @Operation(summary = "归还图书", description = "更新借阅记录并增加图书库存")
    public Result<CirculationRecord> returnBook(@PathVariable Long recordId) {
        log.info("归还图书: recordId={}", recordId);
        CirculationRecord record = circulationService.returnBook(recordId);
        return Result.success(record);
    }

    /**
     * 续借图书
     */
    @PostMapping("/renew/{recordId}")
    @Operation(summary = "续借图书", description = "延长借阅期限")
    public Result<CirculationRecord> renewBook(
            @PathVariable Long recordId,
            @RequestParam(defaultValue = "30") Integer renewDays) {
        log.info("续借图书: recordId={}, renewDays={}", recordId, renewDays);
        CirculationRecord record = circulationService.renewBook(recordId, renewDays);
        return Result.success(record);
    }

    /**
     * 查询读者借阅记录
     */
    @GetMapping("/reader/{readerId}")
    @Operation(summary = "查询读者借阅记录", description = "查询指定读者的借阅历史")
    public Result<List<CirculationRecord>> getReaderRecords(
            @PathVariable Long readerId,
            @RequestParam(required = false) Integer status) {
        log.info("查询读者借阅记录: readerId={}, status={}", readerId, status);
        List<CirculationRecord> records = circulationService.getReaderRecords(readerId, status);
        return Result.success(records);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查流通服务是否正常运行")
    public Result<String> health() {
        return Result.success("Circulation Service is running");
    }
}
