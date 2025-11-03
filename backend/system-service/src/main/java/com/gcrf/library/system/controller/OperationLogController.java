package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.OperationLogQueryRequest;
import com.gcrf.library.system.dto.response.OperationLogVO;
import com.gcrf.library.system.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志查询")
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping
    @Operation(summary = "分页查询操作日志", description = "支持用户、操作类型、状态、时间等条件查询")
    public Result<PageResult<OperationLogVO>> queryLogs(@Valid OperationLogQueryRequest request) {
        log.info("分页查询操作日志, request: {}", request);
        PageResult<OperationLogVO> result = operationLogService.queryLogs(request);
        return Result.success(result);
    }
}
