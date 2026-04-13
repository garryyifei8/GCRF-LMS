package com.gcrf.library.reader.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.client.CirculationServiceClient;
import com.gcrf.library.reader.client.dto.BorrowHistoryDTO;
import com.gcrf.library.reader.dto.*;
import com.gcrf.library.reader.dto.request.BorrowHistoryQueryRequest;
import com.gcrf.library.reader.dto.request.FaceRecognitionRequest;
import com.gcrf.library.reader.dto.response.FaceRecognitionVO;
import com.gcrf.library.reader.dto.response.ReaderDetailVO;
import com.gcrf.library.reader.dto.response.ReaderVO;
import com.gcrf.library.reader.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
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
    private final CirculationServiceClient circulationServiceClient;

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
     * 获取读者借阅历史
     */
    @Operation(summary = "获取读者借阅历史", description = "通过Feign调用流通服务获取读者借阅历史记录")
    @Parameter(name = "id", description = "读者ID", required = true)
    @GetMapping("/{id}/borrow-history")
    public Result<PageResult<BorrowHistoryDTO>> getBorrowHistory(
            @PathVariable Long id,
            @Valid BorrowHistoryQueryRequest request) {
        log.info("获取读者借阅历史: id={}, status={}", id, request.getStatus());

        // 先验证读者是否存在
        readerService.getReaderById(id);

        // 调用流通服务获取借阅历史
        Result<PageResult<BorrowHistoryDTO>> result = circulationServiceClient.getBorrowsByReaderId(
                id, request.getStatus(), request.getPageNum(), request.getPageSize()
        );

        return result;
    }

    /**
     * 人脸识别操作（占位接口）
     * 支持操作类型：REGISTER-注册, UPDATE-更新, DELETE-删除
     * 注意：当前为Mock实现，待Vision Service完成后集成
     */
    @Operation(summary = "人脸识别操作", description = "人脸注册/更新/删除接口（占位实现，待Vision Service完成）")
    @Parameter(name = "id", description = "读者ID", required = true)
    @PostMapping("/{id}/face")
    public Result<FaceRecognitionVO> faceOperation(
            @PathVariable Long id,
            @Valid @RequestBody FaceRecognitionRequest request) {
        log.info("人脸识别操作: readerId={}, operation={}", id, request.getOperation());

        // 先验证读者是否存在
        readerService.getReaderById(id);

        // TODO: 待Vision Service实现后，调用Vision Service进行人脸识别
        // 当前返回Mock数据
        FaceRecognitionVO result = switch (request.getOperation().toUpperCase()) {
            case "REGISTER" -> FaceRecognitionVO.mockRegisterSuccess(id);
            case "UPDATE" -> FaceRecognitionVO.mockUpdateSuccess(id);
            case "DELETE" -> FaceRecognitionVO.mockDeleteSuccess(id);
            default -> {
                log.warn("未知的人脸识别操作类型: {}", request.getOperation());
                yield FaceRecognitionVO.builder()
                        .readerId(id)
                        .status("ERROR")
                        .message("不支持的操作类型: " + request.getOperation())
                        .build();
            }
        };

        return Result.success(result);
    }

    /**
     * 获取读者人脸注册状态（占位接口）
     */
    @Operation(summary = "获取人脸注册状态", description = "查询读者是否已注册人脸（占位实现，待Vision Service完成）")
    @Parameter(name = "id", description = "读者ID", required = true)
    @GetMapping("/{id}/face")
    public Result<FaceRecognitionVO> getFaceStatus(@PathVariable Long id) {
        log.info("查询人脸注册状态: readerId={}", id);

        // 先验证读者是否存在
        readerService.getReaderById(id);

        // TODO: 待Vision Service实现后，查询真实的人脸注册状态
        // 当前返回Mock数据（假设未注册）
        FaceRecognitionVO result = FaceRecognitionVO.mockQueryResult(id, false);

        return Result.success(result);
    }

    /**
     * 批量删除读者
     */
    @Operation(summary = "批量删除读者", description = "根据ID列表批量删除读者（逻辑删除）")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteReaders(@RequestParam String ids) {
        log.info("批量删除读者: ids={}", ids);
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            readerService.deleteReader(Long.parseLong(id.trim()));
        }
        return Result.success();
    }

    /**
     * 办理/更新借阅证
     */
    @Operation(summary = "办理/更新借阅证", description = "为读者办理或更新借阅证")
    @PostMapping("/{id}/card")
    public Result<ReaderDetailVO> issueCard(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        log.info("办理/更新借阅证: id={}", id);
        ReaderDetailVO reader = readerService.activateCard(id);
        return Result.success(reader);
    }

    /**
     * 更新读者状态
     */
    @Operation(summary = "更新读者状态", description = "更新读者状态（active/suspended）")
    @PutMapping("/{id}/status")
    public Result<ReaderDetailVO> updateReaderStatus(@PathVariable Long id, @RequestBody Map<String, String> data) {
        log.info("更新读者状态: id={}, status={}", id, data.get("status"));
        String status = data.get("status");
        ReaderDetailVO reader;
        if ("suspended".equalsIgnoreCase(status)) {
            reader = readerService.suspendCard(id);
        } else if ("active".equalsIgnoreCase(status)) {
            reader = readerService.activateCard(id);
        } else {
            throw new com.gcrf.library.common.exception.BusinessException("不支持的状态: " + status);
        }
        return Result.success(reader);
    }

    /**
     * 根据借阅证号查询读者
     */
    @Operation(summary = "根据借阅证号查询读者", description = "根据借阅证号查询读者详细信息")
    @GetMapping("/card/{cardNumber}")
    public Result<ReaderDetailVO> getReaderByCardNumber(@PathVariable String cardNumber) {
        log.info("根据借阅证号查询读者: cardNumber={}", cardNumber);
        ReaderDetailVO reader = readerService.getReaderByReaderId(cardNumber);
        return Result.success(reader);
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
