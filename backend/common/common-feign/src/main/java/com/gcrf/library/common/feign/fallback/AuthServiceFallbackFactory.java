package com.gcrf.library.common.feign.fallback;

import com.gcrf.library.common.feign.client.AuthServiceClient;
import com.gcrf.library.common.feign.dto.UserDTO;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 认证服务Fallback工厂
 * 提供认证服务调用失败时的降级处理
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Component
public class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        log.error("认证服务调用失败，触发熔断降级: {}", cause.getMessage());

        return new AuthServiceClient() {

            @Override
            public Result<Boolean> validateToken(String authorization) {
                log.error("AuthService.validateToken降级: cause={}", cause.getMessage());
                // 降级策略：令牌验证失败，返回无效
                return Result.success(false);
            }

            @Override
            public Result<Long> getCurrentUserId(String authorization) {
                log.error("AuthService.getCurrentUserId降级: cause={}", cause.getMessage());
                return Result.error(503, "认证服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<UserDTO> getUserInfo(String authorization) {
                log.error("AuthService.getUserInfo降级: cause={}", cause.getMessage());
                return Result.error(503, "认证服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<UserDTO> getUserById(Long userId) {
                log.error("AuthService.getUserById降级: userId={}, cause={}", userId, cause.getMessage());
                return Result.error(503, "认证服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<UserDTO>> getUsersByIds(String userIds) {
                log.error("AuthService.getUsersByIds降级: userIds={}, cause={}", userIds, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Boolean> hasPermission(Long userId, String permission) {
                log.error("AuthService.hasPermission降级: userId={}, permission={}, cause={}",
                        userId, permission, cause.getMessage());
                // 降级策略：返回无权限，确保安全
                return Result.success(false);
            }

            @Override
            public Result<Boolean> hasRole(Long userId, String role) {
                log.error("AuthService.hasRole降级: userId={}, role={}, cause={}",
                        userId, role, cause.getMessage());
                // 降级策略：返回无角色，确保安全
                return Result.success(false);
            }

            @Override
            public Result<List<String>> getUserPermissions(Long userId) {
                log.error("AuthService.getUserPermissions降级: userId={}, cause={}", userId, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<String>> getUserRoles(Long userId) {
                log.error("AuthService.getUserRoles降级: userId={}, cause={}", userId, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<String> health() {
                log.error("AuthService.health降级: cause={}", cause.getMessage());
                return Result.error(503, "认证服务不可用");
            }
        };
    }
}
