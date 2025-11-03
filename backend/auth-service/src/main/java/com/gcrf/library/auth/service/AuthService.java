package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.dto.LoginRequest;
import com.gcrf.library.auth.dto.LoginResponse;
import com.gcrf.library.auth.dto.UserInfoResponse;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import com.gcrf.library.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final long TOKEN_EXPIRY_SECONDS = 7200L; // 2小时

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        // 查询用户（只查询未删除的用户）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername())
                    .apply("deleted_at IS NULL");
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_CREDENTIALS_ERROR);
        }

        // 检查账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成JWT令牌
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "userType", user.getUserType()
        );
        String token = jwtUtil.generateToken(user.getId().toString(), claims);
        Long expiresIn = 7200L; // 2小时，与JWT配置保持一致

        log.info("用户登录成功: username={}, userId={}", user.getUsername(), user.getId());

        return new LoginResponse(
                token,
                expiresIn,
                user.getId(),
                user.getUsername(),
                user.getUserType()
        );
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
     * 用户注销
     */
    public void logout(String token) {
        log.info("用户注销请求");

        // 验证令牌有效性
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 将令牌加入黑名单
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
        bucket.set("logged_out", TOKEN_EXPIRY_SECONDS, TimeUnit.SECONDS);

        Long userId = jwtUtil.getUserId(token);
        log.info("用户注销成功: userId={}", userId);
    }

    /**
     * 刷新令牌
     */
    public LoginResponse refreshToken(String oldToken) {
        log.info("令牌刷新请求");

        // 验证旧令牌
        if (!validateToken(oldToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 获取用户ID并查询用户信息（只查询未删除的用户）
        Long userId = jwtUtil.getUserId(oldToken);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId)
                    .isNull(User::getDeletedAt);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 将旧令牌加入黑名单
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + oldToken;
        RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
        bucket.set("refreshed", TOKEN_EXPIRY_SECONDS, TimeUnit.SECONDS);

        // 确保新令牌的时间戳与旧令牌不同（JWT iat使用秒级精度,需要至少1秒延迟）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 生成新令牌
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "userType", user.getUserType()
        );
        String newToken = jwtUtil.generateToken(user.getId().toString(), claims);
        Long expiresIn = 7200L;

        log.info("令牌刷新成功: userId={}", user.getId());

        return new LoginResponse(
                newToken,
                expiresIn,
                user.getId(),
                user.getUsername(),
                user.getUserType()
        );
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
