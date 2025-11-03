package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.dto.RefreshTokenRequest;
import com.gcrf.library.auth.dto.UserInfoResponse;
import com.gcrf.library.auth.service.AuthService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、令牌管理等接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录系统")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.getUsername());
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证JWT令牌的有效性")
    public ResponseEntity<Result<Boolean>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未提供认证令牌"));
        }
        String token = authorization.replace("Bearer ", "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Result.success(valid));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    @Operation(summary = "获取当前用户", description = "根据令牌获取当前登录用户信息")
    public ResponseEntity<Result<Long>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未提供认证令牌"));
        }
        String token = authorization.replace("Bearer ", "");
        Long userId = authService.getUserIdFromToken(token);
        return ResponseEntity.ok(Result.success(userId));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "注销当前登录用户，令牌将被加入黑名单")
    public ResponseEntity<Result<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未提供认证令牌"));
        }
        log.info("收到注销请求");
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok(Result.success());
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用现有令牌刷新获取新令牌")
    public Result<LoginResponse> refreshToken(@Validated @RequestBody RefreshTokenRequest request) {
        log.info("收到令牌刷新请求");
        LoginResponse response = authService.refreshToken(request.getToken());
        return Result.success(response);
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户详细信息", description = "根据令牌获取当前用户的详细信息")
    public ResponseEntity<Result<UserInfoResponse>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未提供认证令牌"));
        }
        log.info("收到获取用户信息请求");
        String token = authorization.replace("Bearer ", "");
        Long userId = authService.getUserIdFromToken(token);
        UserInfoResponse userInfo = authService.getUserInfo(userId);
        return ResponseEntity.ok(Result.success(userInfo));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查认证服务是否正常运行")
    public Result<String> health() {
        return Result.success("Auth Service is running");
    }
}
