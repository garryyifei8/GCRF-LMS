package com.gcrf.library.circulation.controller;

import com.gcrf.library.circulation.dto.ReserveRequest;
import com.gcrf.library.circulation.dto.response.ReserveDetailVO;
import com.gcrf.library.circulation.dto.response.ReserveVO;
import com.gcrf.library.circulation.service.ReserveService;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "预约管理", description = "图书预约、取书、取消预约相关接口")
@RestController
@RequestMapping("/api/v1/reserves")
@RequiredArgsConstructor
public class ReserveController {

    private final ReserveService reserveService;

    /**
     * 分页查询预约记录
     */
    @Operation(summary = "分页查询预约记录", description = "根据条件分页查询预约记录")
    @GetMapping
    public Result<PageResult<ReserveVO>> queryReserves(
            @Parameter(description = "读者ID") @RequestParam(required = false) Long readerId,
            @Parameter(description = "预约状态 (RESERVED, PICKED_UP, CANCELLED, EXPIRED)") @RequestParam(required = false) String status,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("分页查询预约记录请求: readerId={}, status={}, pageNum={}, pageSize={}",
                 readerId, status, pageNum, pageSize);
        PageResult<ReserveVO> result = reserveService.queryReserves(readerId, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 根据ID获取预约详情
     */
    @Operation(summary = "根据ID获取预约详情", description = "根据预约记录ID查询详细信息")
    @Parameter(name = "id", description = "预约记录ID", required = true)
    @GetMapping("/{id}")
    public Result<ReserveDetailVO> getReserveById(@PathVariable Long id) {
        log.info("查询预约详情: id={}", id);
        ReserveDetailVO result = reserveService.getReserveById(id);
        return Result.success(result);
    }

    /**
     * 预约图书
     */
    @Operation(summary = "预约图书", description = "创建图书预约记录")
    @PostMapping("/reserve")
    public Result<ReserveDetailVO> reserveBook(@Valid @RequestBody ReserveRequest request) {
        log.info("预约图书请求: bookId={}, readerId={}", request.getBookId(), request.getReaderId());
        ReserveDetailVO result = reserveService.reserveBook(request);
        return Result.success(result);
    }

    /**
     * 取书（完成预约）
     */
    @Operation(summary = "取书", description = "完成预约，读者取走图书")
    @Parameter(name = "id", description = "预约记录ID", required = true)
    @PostMapping("/{id}/pickup")
    public Result<ReserveDetailVO> pickupReserve(@PathVariable Long id) {
        log.info("取书请求: id={}", id);
        ReserveDetailVO result = reserveService.pickupReserve(id);
        return Result.success(result);
    }

    /**
     * 取消预约
     */
    @Operation(summary = "取消预约", description = "取消图书预约")
    @Parameter(name = "id", description = "预约记录ID", required = true)
    @PostMapping("/{id}/cancel")
    public Result<ReserveDetailVO> cancelReserve(@PathVariable Long id) {
        log.info("取消预约请求: id={}", id);
        ReserveDetailVO result = reserveService.cancelReserve(id);
        return Result.success(result);
    }

    /**
     * 批量过期处理（定时任务调用）
     */
    @Operation(summary = "批量过期处理", description = "将已超过过期日期的预约记录状态更新为EXPIRED")
    @PostMapping("/expire-reserves")
    public Result<Void> expireReserves() {
        log.info("批量过期处理预约记录");
        reserveService.expireReserves();
        return Result.success();
    }

    /**
     * 发送预约通知
     */
    @Operation(summary = "发送预约通知", description = "标记预约记录已发送通知，更新通知计数和时间")
    @Parameter(name = "id", description = "预约记录ID", required = true)
    @PostMapping("/{id}/notify")
    public Result<ReserveDetailVO> notifyReserve(@PathVariable Long id) {
        log.info("发送预约通知: id={}", id);
        ReserveDetailVO result = reserveService.notifyReserve(id);
        return Result.success(result);
    }

    /**
     * 获取待通知预约记录
     */
    @Operation(summary = "获取待通知预约记录", description = "查询需要发送通知的预约记录")
    @GetMapping("/pending-notifications")
    public Result<List<ReserveVO>> getPendingNotifications() {
        log.info("查询待通知预约记录");
        List<ReserveVO> result = reserveService.getPendingNotifications();
        return Result.success(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查预约服务是否正常运行")
    public Result<String> health() {
        return Result.success("Circulation Service (Reserve) is running");
    }
}
