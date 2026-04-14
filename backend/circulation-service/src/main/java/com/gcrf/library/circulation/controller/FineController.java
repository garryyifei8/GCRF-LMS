package com.gcrf.library.circulation.controller;

import com.gcrf.library.circulation.dto.request.FinePaymentRequest;
import com.gcrf.library.circulation.dto.response.FineVO;
import com.gcrf.library.circulation.service.FineService;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 罚金管理控制器
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Slf4j
@Tag(name = "罚金管理", description = "逾期罚金计算、支付、查询相关接口")
@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    /**
     * 查询逾期记录（带罚金）
     */
    @Operation(summary = "查询逾期记录", description = "分页查询逾期借阅记录及罚金信息")
    @GetMapping("/overdue")
    public Result<PageResult<FineVO>> queryOverdueRecords(
            @Parameter(description = "读者ID") @RequestParam(required = false) Long readerId,
            @Parameter(description = "是否已支付") @RequestParam(required = false) Boolean paid,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        log.info("查询逾期记录: readerId={}, paid={}, pageNum={}, pageSize={}", 
                 readerId, paid, pageNum, pageSize);
        
        PageResult<FineVO> result = fineService.queryOverdueRecords(readerId, paid, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 计算指定借阅记录的罚金
     */
    @Operation(summary = "计算罚金", description = "根据借阅记录ID计算逾期罚金")
    @PostMapping("/calculate/{borrowId}")
    public Result<Map<String, Object>> calculateFine(
            @Parameter(description = "借阅记录ID", required = true) @PathVariable Long borrowId) {
        
        log.info("计算罚金: borrowId={}", borrowId);
        
        Map<String, Object> result = fineService.calculateFine(borrowId);
        return Result.success(result);
    }

    /**
     * 支付罚金
     */
    @Operation(summary = "支付罚金", description = "支付指定借阅记录的逾期罚金")
    @PostMapping("/pay")
    public Result<FineVO> payFine(@Valid @RequestBody FinePaymentRequest request) {
        
        log.info("支付罚金: borrowId={}, paymentMethod={}", 
                 request.getBorrowId(), request.getPaymentMethod());
        
        FineVO result = fineService.payFine(request);
        return Result.success(result);
    }

    /**
     * 查询罚金记录
     */
    @Operation(summary = "查询罚金记录", description = "分页查询所有罚金记录（包括已支付和未支付）")
    @GetMapping
    public Result<PageResult<FineVO>> queryFines(
            @Parameter(description = "读者ID") @RequestParam(required = false) Long readerId,
            @Parameter(description = "是否已支付") @RequestParam(required = false) Boolean paid,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") Integer pageSize) {
        
        log.info("查询罚金记录: readerId={}, paid={}, pageNum={}, pageSize={}", 
                 readerId, paid, pageNum, pageSize);
        
        PageResult<FineVO> result = fineService.queryFines(readerId, paid, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 批量归还（带罚金处理）
     */
    @Operation(summary = "批量归还", description = "批量归还图书并计算罚金")
    @PostMapping("/batch-return")
    public Result<Map<String, Object>> batchReturn(
            @Parameter(description = "借阅记录ID列表", required = true) 
            @RequestBody List<Long> borrowIds) {
        
        log.info("批量归还: borrowIds={}", borrowIds);
        
        Map<String, Object> result = fineService.batchReturn(borrowIds);
        return Result.success(result);
    }

    /**
     * 获取罚金统计信息
     */
    @Operation(summary = "罚金统计", description = "获取罚金统计数据（总金额、已支付、未支付等）")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFineStatistics(
            @Parameter(description = "读者ID（可选）") @RequestParam(required = false) Long readerId) {
        
        log.info("查询罚金统计: readerId={}", readerId);
        
        Map<String, Object> result = fineService.getFineStatistics(readerId);
        return Result.success(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查罚金服务是否正常运行")
    public Result<String> health() {
        return Result.success("Fine Service is running");
    }
}
