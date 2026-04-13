package com.gcrf.library.common.feign.client;

import com.gcrf.library.common.feign.constant.FeignConstants;
import com.gcrf.library.common.feign.dto.UserDTO;
import com.gcrf.library.common.feign.fallback.AuthServiceFallbackFactory;
import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证服务Feign客户端
 * 提供用户认证、授权验证等接口
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@FeignClient(
        name = FeignConstants.AUTH_SERVICE,
        path = FeignConstants.AUTH_API_PREFIX,
        fallbackFactory = AuthServiceFallbackFactory.class
)
public interface AuthServiceClient {

    /**
     * 验证令牌有效性
     *
     * @param authorization 授权头（Bearer token）
     * @return 令牌是否有效
     */
    @GetMapping("/validate")
    Result<Boolean> validateToken(@RequestHeader(FeignConstants.HEADER_AUTHORIZATION) String authorization);

    /**
     * 获取当前用户ID
     *
     * @param authorization 授权头（Bearer token）
     * @return 用户ID
     */
    @GetMapping("/current-user")
    Result<Long> getCurrentUserId(@RequestHeader(FeignConstants.HEADER_AUTHORIZATION) String authorization);

    /**
     * 获取用户详细信息
     *
     * @param authorization 授权头（Bearer token）
     * @return 用户详细信息
     */
    @GetMapping("/info")
    Result<UserDTO> getUserInfo(@RequestHeader(FeignConstants.HEADER_AUTHORIZATION) String authorization);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID列表（逗号分隔）
     * @return 用户信息列表
     */
    @GetMapping("/users/batch")
    Result<List<UserDTO>> getUsersByIds(@RequestParam("ids") String userIds);

    /**
     * 检查用户是否有指定权限
     *
     * @param userId     用户ID
     * @param permission 权限标识
     * @return 是否有权限
     */
    @GetMapping("/users/{userId}/has-permission")
    Result<Boolean> hasPermission(
            @PathVariable("userId") Long userId,
            @RequestParam("permission") String permission
    );

    /**
     * 检查用户是否有指定角色
     *
     * @param userId 用户ID
     * @param role   角色标识
     * @return 是否有角色
     */
    @GetMapping("/users/{userId}/has-role")
    Result<Boolean> hasRole(
            @PathVariable("userId") Long userId,
            @RequestParam("role") String role
    );

    /**
     * 获取用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @GetMapping("/users/{userId}/permissions")
    Result<List<String>> getUserPermissions(@PathVariable("userId") Long userId);

    /**
     * 获取用户的所有角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/users/{userId}/roles")
    Result<List<String>> getUserRoles(@PathVariable("userId") Long userId);

    /**
     * 健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    Result<String> health();
}
