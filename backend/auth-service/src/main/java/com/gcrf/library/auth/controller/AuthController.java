package com.gcrf.library.auth.controller;

import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.dto.RefreshRequest;
import com.gcrf.library.auth.dto.UserInfoResponse;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.auth.service.AuthService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.common.result.ResultCode;
import com.gcrf.library.common.security.context.SecurityContextHolder;
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
    private final UserMapper userMapper;

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
     * 用户注销 — 撤销 refresh token
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "撤销 refresh token，完成注销")
    public Result<Void> logout(@RequestBody(required = false) RefreshRequest req) {
        log.info("收到注销请求");
        String tk = req == null ? null : req.getRefreshToken();
        authService.logout(tk);
        return Result.success();
    }

    /**
     * 刷新令牌 — 消费 refresh token，旋转后返回新的富化响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用 refresh token 换取新的 access token + refresh token")
    public Result<LoginResponse> refreshToken(@RequestBody RefreshRequest req) {
        log.info("收到令牌刷新请求");
        LoginResponse response = authService.refreshToken(req.getRefreshToken());
        return Result.success(response);
    }

    /**
     * 获取当前登录用户的富化信息（依赖 JWT 过滤器填充 SecurityContextHolder）
     */
    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "通过 JWT 获取当前登录用户的完整信息")
    public Result<LoginResponse> me() {
        Long uid = SecurityContextHolder.currentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(authService.buildLoginResponseFromUser(user));
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
