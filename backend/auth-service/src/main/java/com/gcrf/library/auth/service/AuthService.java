package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.dto.UserInfoResponse;
import com.gcrf.library.auth.entity.Role;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证服务
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedissonClient redissonClient;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    /**
     * 用户登录 — 返回含 roles/tenant/scope/permissions 的富化响应
     */
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        User user = findActiveUser(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_CREDENTIALS_ERROR);
        }
        log.info("用户登录成功: username={}, userId={}", user.getUsername(), user.getId());
        return buildLoginResponse(user);
    }

    /**
     * 构建富化登录响应（含 roles/tenant/scope/permissions）
     * 供 login() 和 refreshToken() 共用。
     */
    public LoginResponse buildLoginResponse(User user) {
        List<Role> roles = roleService.rolesOfUser(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        String maxScope = roles.stream()
                .map(Role::getScopeDefault)
                .max(Comparator.comparingInt(s -> Scope.valueOf(s).ordinal()))
                .orElse("SELF");

        Set<String> perms = permissionService.codesForUser(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        if (user.getTenantSchema() != null) {
            claims.put("tenant", user.getTenantSchema());
            claims.put("tenantId", user.getSchoolId());
        }
        claims.put("roles", roleCodes);
        claims.put("scope", maxScope);

        String accessToken = jwtUtil.generateToken(user.getId().toString(), claims);
        String refreshToken = refreshTokenService.issue(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(1800L)
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .roles(roleCodes)
                .tenant(user.getTenantSchema())
                .tenantId(user.getSchoolId())
                .scope(maxScope)
                .permissions(perms)
                .build();
    }

    /**
     * 查询并校验活跃用户（已删除或不存在均抛异常）
     */
    private User findActiveUser(String username) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getUsername, username).apply("deleted_at IS NULL");
        User u = userMapper.selectOne(w);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (!"ACTIVE".equals(u.getStatus())) throw new BusinessException(ResultCode.USER_DISABLED);
        return u;
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            // 检查令牌是否在黑名单中
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
            if (bucket.isExists()) {
                log.warn("令牌已被注销: {}", token.substring(0, Math.min(20, token.length())));
                return false;
            }

            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.error("令牌验证失败", e);
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return jwtUtil.getUserId(token);
    }

    /**
     * 用户注销 — 撤销 refresh token（不再操作 access token 黑名单）
     */
    public void logout(String refreshToken) {
        log.info("用户注销请求");
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
        log.info("用户注销成功");
    }

    /**
     * 刷新令牌 — 消费旧 refresh token（旋转），返回新的富化登录响应
     */
    public LoginResponse refreshToken(String refreshToken) {
        log.info("令牌刷新请求");
        Long userId = refreshTokenService.consume(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        log.info("令牌刷新成功: userId={}", userId);
        return buildLoginResponse(user);
    }

    /**
     * 通过 User 构建富化登录响应（供 /me 等端点复用）
     */
    public LoginResponse buildLoginResponseFromUser(User user) {
        return buildLoginResponse(user);
    }

    /**
     * 获取用户详细信息
     */
    public UserInfoResponse getUserInfo(Long userId) {
        log.info("获取用户信息请求: userId={}", userId);

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return new UserInfoResponse(
                user.getId(),
                user.getUserId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getUserType(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getLastLoginIp(),
                user.getCreatedAt()
        );
    }
}
