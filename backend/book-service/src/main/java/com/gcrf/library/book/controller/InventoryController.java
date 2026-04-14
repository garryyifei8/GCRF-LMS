package com.gcrf.library.book.controller;

import com.gcrf.library.book.dto.request.*;
import com.gcrf.library.book.dto.response.*;
import com.gcrf.library.book.service.InventoryService;
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
 * 库存管理控制器
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "库存管理", description = "库存的查询、调整、盘点任务等接口")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== 库存管理 ====================

    /**
     * 分页查询库存
     */
    @GetMapping
    @Operation(summary = "分页查询库存", description = "支持按位置、书架号筛选，可仅显示预警库存")
    public Result<PageResult<InventoryDetailVO>> queryInventories(@Valid InventoryQueryRequest request) {
        log.info("分页查询库存请求: {}", request);
        PageResult<InventoryDetailVO> result = inventoryService.queryInventories(request);
        return Result.success(result);
    }

    /**
     * 根据ID查询库存详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询库存详情", description = "根据库存ID查询详细信息")
    public Result<InventoryDetailVO> getInventory(
            @Parameter(description = "库存ID") @PathVariable Long id) {
        log.info("查询库存详情: id={}", id);
        InventoryDetailVO inventory = inventoryService.getInventoryById(id);
        return Result.success(inventory);
    }

    /**
     * 库存调整
     */
    @PostMapping("/adjust")
    @Operation(summary = "库存调整", description = "调整图书库存，支持增加、减少、设置三种方式")
    public Result<InventoryDetailVO> adjustInventory(@Valid @RequestBody InventoryAdjustRequest request) {
        log.info("库存调整请求: bookId={}, adjustType={}, quantity={}",
                request.getBookId(), request.getAdjustType(), request.getQuantity());
        InventoryDetailVO result = inventoryService.adjustInventory(request);
        return Result.success(result);
    }

    /**
     * 查询库存预警列表
     */
    @GetMapping("/alerts")
    @Operation(summary = "查询库存预警列表", description = "查询可借数量低于预警阈值的库存")
    public Result<PageResult<InventoryAlertVO>> queryAlertInventories(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询库存预警列表: pageNum={}, pageSize={}", pageNum, pageSize);
        PageResult<InventoryAlertVO> result = inventoryService.queryAlertInventories(pageNum, pageSize);
        return Result.success(result);
    }

    // ==================== 盘点任务管理 ====================

    /**
     * 创建盘点任务
     */
    @PostMapping("/tasks")
    @Operation(summary = "创建盘点任务", description = "创建新的盘点任务")
    public Result<InventoryTaskVO> createTask(@Valid @RequestBody InventoryTaskCreateRequest request) {
        log.info("创建盘点任务请求: taskName={}, taskType={}, scope={}",
                request.getTaskName(), request.getTaskType(), request.getScope());
        InventoryTaskVO result = inventoryService.createTask(request);
        return Result.success(result);
    }

    /**
     * 分页查询盘点任务
     */
    @GetMapping("/tasks")
    @Operation(summary = "分页查询盘点任务", description = "支持按任务类型、状态、操作人等筛选")
    public Result<PageResult<InventoryTaskVO>> queryTasks(@Valid InventoryTaskQueryRequest request) {
        log.info("分页查询盘点任务请求: {}", request);
        PageResult<InventoryTaskVO> result = inventoryService.queryTasks(request);
        return Result.success(result);
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/tasks/{id}")
    @Operation(summary = "查询任务详情", description = "根据任务ID查询详细信息")
    public Result<InventoryTaskVO> getTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        log.info("查询任务详情: id={}", id);
        InventoryTaskVO task = inventoryService.getTaskById(id);
        return Result.success(task);
    }

    /**
     * 更新盘点任务
     */
    @PutMapping("/tasks/{id}")
    @Operation(summary = "更新盘点任务", description = "修改盘点任务信息（仅待执行状态可修改）")
    public Result<InventoryTaskVO> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody InventoryTaskUpdateRequest request) {
        log.info("更新盘点任务请求: id={}", id);
        InventoryTaskVO result = inventoryService.updateTask(id, request);
        return Result.success(result);
    }

    /**
     * 开始盘点
     */
    @PostMapping("/tasks/{id}/start")
    @Operation(summary = "开始盘点", description = "将任务状态从待执行变为进行中")
    public Result<InventoryTaskVO> startTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        log.info("开始盘点请求: id={}", id);
        InventoryTaskVO result = inventoryService.startTask(id);
        return Result.success(result);
    }

    /**
     * 完成盘点
     */
    @PostMapping("/tasks/{id}/complete")
    @Operation(summary = "完成盘点", description = "将任务状态从进行中变为已完成，并统计盘点结果")
    public Result<InventoryTaskVO> completeTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        log.info("完成盘点请求: id={}", id);
        InventoryTaskVO result = inventoryService.completeTask(id);
        return Result.success(result);
    }

    /**
     * 取消盘点任务
     */
    @PostMapping("/tasks/{id}/cancel")
    @Operation(summary = "取消盘点任务", description = "取消待执行或进行中的任务")
    public Result<InventoryTaskVO> cancelTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        log.info("取消盘点任务请求: id={}", id);
        InventoryTaskVO result = inventoryService.cancelTask(id);
        return Result.success(result);
    }

    // ==================== 盘点明细管理 ====================

    /**
     * 分页查询盘点明细
     */
    @GetMapping("/tasks/{id}/items")
    @Operation(summary = "分页查询盘点明细", description = "查询指定任务的盘点明细列表")
    public Result<PageResult<InventoryTaskItemVO>> queryTaskItems(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid InventoryTaskItemQueryRequest request) {
        log.info("分页查询盘点明细请求: taskId={}, request={}", id, request);
        PageResult<InventoryTaskItemVO> result = inventoryService.queryTaskItems(id, request);
        return Result.success(result);
    }

    /**
     * 录入盘点结果
     */
    @PostMapping("/tasks/{id}/items")
    @Operation(summary = "录入盘点结果", description = "录入单个或批量盘点结果")
    public Result<List<InventoryTaskItemVO>> recordTaskItems(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody InventoryTaskItemBatchRequest request) {
        log.info("录入盘点结果请求: taskId={}, itemCount={}", id, request.getItems().size());
        List<InventoryTaskItemVO> results = inventoryService.batchRecordTaskItems(id, request);
        return Result.success(results);
    }

    /**
     * 录入单个盘点结果
     */
    @PostMapping("/tasks/{taskId}/items/{bookId}")
    @Operation(summary = "录入单个盘点结果", description = "录入单个图书的盘点结果")
    public Result<InventoryTaskItemVO> recordSingleTaskItem(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "图书ID") @PathVariable Long bookId,
            @Valid @RequestBody InventoryTaskItemRequest request) {
        log.info("录入单个盘点结果请求: taskId={}, bookId={}, actualQuantity={}",
                taskId, bookId, request.getActualQuantity());
        // 确保请求中的bookId与路径参数一致
        request.setBookId(bookId);
        InventoryTaskItemVO result = inventoryService.recordTaskItem(taskId, request);
        return Result.success(result);
    }

    /**
     * 获取任务的差异明细
     */
    @GetMapping("/tasks/{id}/discrepancies")
    @Operation(summary = "获取差异明细", description = "获取指定任务中有差异的盘点明细")
    public Result<List<InventoryTaskItemVO>> getDiscrepancyItems(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        log.info("获取差异明细请求: taskId={}", id);
        List<InventoryTaskItemVO> results = inventoryService.getDiscrepancyItems(id);
        return Result.success(results);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查库存服务是否正常运行")
    public Result<String> health() {
        return Result.success("Inventory Service is running");
    }
}
