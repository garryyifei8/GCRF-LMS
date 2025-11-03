package com.gcrf.library.reader.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.dto.*;
import com.gcrf.library.reader.dto.response.ReaderDetailVO;
import com.gcrf.library.reader.dto.response.ReaderVO;
import com.gcrf.library.reader.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 读者管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Slf4j
@Tag(name = "读者管理", description = "读者信息管理相关接口")
@RestController
@RequestMapping("/api/v1/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    /**
     * 分页查询读者列表
     */
    @Operation(summary = "分页查询读者列表", description = "根据条件分页查询读者列表")
    @GetMapping
    public Result<PageResult<ReaderVO>> queryReaders(@Valid ReaderQueryRequest request) {
        log.info("分页查询读者列表请求: {}", request);
        PageResult<ReaderVO> result = readerService.queryReaders(request);
        return Result.success(result);
    }

    /**
     * 根据ID获取读者详情
     */
    @Operation(summary = "根据ID获取读者信息", description = "根据读者ID查询读者详细信息")
    @Parameter(name = "id", description = "读者ID", required = true)
    @GetMapping("/{id}")
    public Result<ReaderDetailVO> getReaderById(@PathVariable Long id) {
        log.info("查询读者详情: id={}", id);
        ReaderDetailVO reader = readerService.getReaderById(id);
        return Result.success(reader);
    }

    /**
     * 根据读者证号获取读者详情
     */
    @Operation(summary = "根据读者证号获取读者信息", description = "根据读者证号查询读者详细信息")
    @Parameter(name = "readerId", description = "读者证号", required = true)
    @GetMapping("/readerId/{readerId}")
    public Result<ReaderDetailVO> getReaderByReaderId(@PathVariable String readerId) {
        log.info("根据读者证号查询: readerId={}", readerId);
        ReaderDetailVO reader = readerService.getReaderByReaderId(readerId);
        return Result.success(reader);
    }

    /**
     * 创建读者
     */
    @Operation(summary = "创建读者", description = "创建新的读者信息并生成借书卡")
    @PostMapping
    public Result<ReaderDetailVO> createReader(@Valid @RequestBody ReaderCreateRequest request) {
        log.info("创建读者: {}", request.getName());
        ReaderDetailVO result = readerService.createReader(request);
        return Result.success(result);
    }

    /**
     * 更新读者信息
     */
    @Operation(summary = "更新读者信息", description = "更新读者的基本信息")
    @PutMapping("/{id}")
    public Result<ReaderDetailVO> updateReader(@PathVariable Long id, @Valid @RequestBody ReaderUpdateRequest request) {
        log.info("更新读者: id={}", id);
        // 确保路径参数和请求体的ID一致
        request.setId(id);
        ReaderDetailVO result = readerService.updateReader(request);
        return Result.success(result);
    }

    /**
     * 删除读者
     */
    @Operation(summary = "删除读者", description = "删除读者信息（逻辑删除）")
    @Parameter(name = "id", description = "读者ID", required = true)
    @DeleteMapping("/{id}")
    public Result<Void> deleteReader(@PathVariable Long id) {
        log.info("删除读者: id={}", id);
        readerService.deleteReader(id);
        return Result.success();
    }

    /**
     * 激活借书卡
     */
    @Operation(summary = "激活借书卡", description = "激活读者的借书卡，使其可以借阅图书")
    @Parameter(name = "id", description = "读者ID", required = true)
    @PostMapping("/{id}/activate")
    public Result<ReaderDetailVO> activateCard(@PathVariable Long id) {
        log.info("激活借书卡: id={}", id);
        ReaderDetailVO result = readerService.activateCard(id);
        return Result.success(result);
    }

    /**
     * 挂失借书卡
     */
    @Operation(summary = "挂失借书卡", description = "挂失读者的借书卡，暂停借阅权限")
    @Parameter(name = "id", description = "读者ID", required = true)
    @PostMapping("/{id}/suspend")
    public Result<ReaderDetailVO> suspendCard(@PathVariable Long id) {
        log.info("挂失借书卡: id={}", id);
        ReaderDetailVO result = readerService.suspendCard(id);
        return Result.success(result);
    }

    /**
     * 注销借书卡
     */
    @Operation(summary = "注销借书卡", description = "注销读者的借书卡")
    @Parameter(name = "id", description = "读者ID", required = true)
    @PostMapping("/{id}/cancel")
    public Result<ReaderDetailVO> cancelCard(@PathVariable Long id) {
        log.info("注销借书卡: id={}", id);
        ReaderDetailVO result = readerService.cancelCard(id);
        return Result.success(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查读者服务是否正常运行")
    public Result<String> health() {
        return Result.success("Reader Service is running");
    }
}
