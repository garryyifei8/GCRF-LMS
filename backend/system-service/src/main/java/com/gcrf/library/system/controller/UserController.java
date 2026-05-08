package com.gcrf.library.system.controller;

import com.gcrf.library.system.dto.response.UserPageVO;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.client.UserManagementClient;
import com.gcrf.library.system.dto.request.UserCreateRequest;
import com.gcrf.library.system.dto.request.UserPasswordResetRequest;
import com.gcrf.library.system.dto.request.UserStatusRequest;
import com.gcrf.library.system.dto.request.UserUpdateRequest;
import com.gcrf.library.system.dto.response.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户管理控制器（代理至auth-service）
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
@Tag(name = "系统用户管理", description = "系统管理员用户CRUD操作（代理至认证服务）")
public class UserController {

    private final UserManagementClient userManagementClient;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "支持按用户名、类型、状态筛选，返回PageResult格式")
    public Result<PageResult<UserVO>> getUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "搜索关键词（用户名）") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色/用户类型") @RequestParam(required = false) String role,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {

        log.info("查询用户列表: pageNum={}, pageSize={}, keyword={}, role={}, status={}",
                pageNum, pageSize, keyword, role, status);

        Result<UserPageVO> authResult = userManagementClient.getUserList(
                pageNum, pageSize, keyword, role, status);

        if (authResult == null || authResult.getData() == null) {
            return Result.success(PageResult.emptyRecords(pageNum, pageSize));
        }

        UserPageVO page = authResult.getData();
        List<UserVO> adapted = page.getRecords().stream()
                .map(this::adaptUserVO)
                .collect(Collectors.toList());

        PageResult<UserVO> pageResult = PageResult.ofRecords(
                page.getTotal(),
                pageNum,
                pageSize,
                adapted
        );

        return Result.success(pageResult);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Result<UserVO> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("获取用户详情: id={}", id);
        Result<UserVO> authResult = userManagementClient.getUserById(id);
        if (authResult != null && authResult.getData() != null) {
            return Result.success(adaptUserVO(authResult.getData()));
        }
        return authResult;
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户账号")
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("创建用户: username={}", request.getUsername());
        Result<UserVO> authResult = userManagementClient.createUser(request);
        if (authResult != null && authResult.getData() != null) {
            return Result.success(adaptUserVO(authResult.getData()));
        }
        return authResult;
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户基本信息")
    public Result<UserVO> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("更新用户: id={}", id);
        Result<UserVO> authResult = userManagementClient.updateUser(id, request);
        if (authResult != null && authResult.getData() != null) {
            return Result.success(adaptUserVO(authResult.getData()));
        }
        return authResult;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "软删除用户账号")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("删除用户: id={}", id);
        return userManagementClient.deleteUser(id);
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password/reset")
    @Operation(summary = "重置用户密码", description = "重置用户登录密码")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody(required = false) UserPasswordResetRequest request) {
        log.info("重置用户密码: id={}", id);
        if (request == null) {
            request = new UserPasswordResetRequest();
        }
        return userManagementClient.resetPassword(id, request);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户账号")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        log.info("更新用户状态: id={}, status={}", id, request.getStatus());
        return userManagementClient.updateUserStatus(id, request);
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除用户", description = "批量软删除用户账号")
    public Result<Void> batchDeleteUsers(
            @Parameter(description = "用户ID列表，逗号分隔") @RequestParam String ids) {
        log.info("批量删除用户: ids={}", ids);
        return userManagementClient.batchDeleteUsers(ids);
    }

    /**
     * 适配UserVO：填充realName兜底逻辑
     */
    private UserVO adaptUserVO(UserVO vo) {
        if (vo == null) {
            return null;
        }
        // realName当前由auth-service填充；若为空则回退到username
        if (vo.getRealName() == null || vo.getRealName().isBlank()) {
            vo.setRealName(vo.getUsername());
        }
        return vo;
    }
}
