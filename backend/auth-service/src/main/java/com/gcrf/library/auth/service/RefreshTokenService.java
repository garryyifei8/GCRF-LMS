package com.gcrf.library.auth.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Refresh Token 服务 — Redis 存储 + 旋转防重放
 *
 * <p>Key 格式: {@code refresh:<token-uuid>} → userId (Long)
 * TTL: 30 天
 *
 * @author GCRF Team
 * @date 2026-05-11
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final Duration TTL = Duration.ofDays(30);

    private final RedissonClient redisson;

    /**
     * 签发新的 refresh token，写入 Redis，返回 UUID 字符串。
     */
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        bucket.set(userId, TTL);
        return token;
    }

    /**
     * 消费 refresh token（旋转：读取后立即删除）。
     *
     * @param token 客户端传入的 refresh token 字符串
     * @return 关联的 userId
     * @throws BusinessException 若 token 不存在或已过期
     */
    public Long consume(String token) {
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        Long userId = bucket.getAndDelete();   // atomic — Redisson 3.17+
        if (userId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "refresh token 无效或已过期");
        }
        return userId;
    }

    /**
     * 撤销 refresh token（用于 logout）。
     */
    public void revoke(String token) {
        RBucket<Long> bucket = redisson.getBucket(KEY_PREFIX + token);
        bucket.delete();
    }
}
