package com.gcrf.library.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcrf.library.auth.dto.*;
import com.gcrf.library.auth.service.UserService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD操作接口")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户账号")
    public Result<UserInfoResponse> createUser(@Validated @RequestBody CreateUserRequest request) {
        log.info("收到创建用户请求: username={}", request.getUsername());
        UserInfoResponse response = userService.createUser(request);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    @Operation(summary = "更新用户信息", description = "更新用户的基本信息")
    public Result<UserInfoResponse> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Validated @RequestBody UpdateUserRequest request) {
        log.info("收到更新用户请求: userId={}", userId);
        UserInfoResponse response = userService.updateUser(userId, request);
        return Result.success(response);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{userId}/password")
    @Operation(summary = "修改密码", description = "修改用户登录密码")
    public Result<Void> changePassword(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Validated @RequestBody ChangePasswordRequest request) {
        log.info("收到修改密码请求: userId={}", userId);
        userService.changePassword(userId, request);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "软删除用户账号")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("收到删除用户请求: userId={}", userId);
        userService.deleteUser(userId);
        return Result.success();
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Result<UserInfoResponse> getUserById(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("收到获取用户详情请求: userId={}", userId);
        UserInfoResponse response = userService.getUserById(userId);
        return Result.success(response);
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户", description = "通过用户名查询用户信息")
    public Result<UserInfoResponse> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        log.info("收到根据用户名获取用户请求: username={}", username);
        UserInfoResponse response = userService.getUserByUsername(username);
        return Result.success(response);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "支持按用户名、类型、状态筛选")
    public Result<IPage<UserInfoResponse>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名(模糊查询)") @RequestParam(required = false) String username,
            @Parameter(description = "用户类型") @RequestParam(required = false) String userType,
            @Parameter(description = "账号状态") @RequestParam(required = false) String status) {
        log.info("收到分页查询用户列表请求: pageNum={}, pageSize={}", pageNum, pageSize);
        IPage<UserInfoResponse> page = userService.getUserList(pageNum, pageSize, username, userType, status);
        return Result.success(page);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查用户管理服务是否正常运行")
    public Result<String> health() {
        return Result.success("User Service is running");
    }
}
