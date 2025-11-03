package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.LoginLogQueryRequest;
import com.gcrf.library.system.dto.response.LoginLogVO;
import com.gcrf.library.system.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/login-logs")
@RequiredArgsConstructor
@Tag(name = "登录日志", description = "登录日志查询")
public class LoginLogController {

    private final LoginLogService loginLogService;

    /**
     * 分页查询登录日志
     */
    @GetMapping
    @Operation(summary = "分页查询登录日志", description = "支持用户、登录方式、状态、时间等条件查询")
    public Result<PageResult<LoginLogVO>> queryLogs(@Valid LoginLogQueryRequest request) {
        log.info("分页查询登录日志, request: {}", request);
        PageResult<LoginLogVO> result = loginLogService.queryLogs(request);
        return Result.success(result);
    }
}
