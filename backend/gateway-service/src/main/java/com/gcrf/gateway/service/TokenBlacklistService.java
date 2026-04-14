package com.gcrf.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 *
 * 用于检查JWT Token是否已被注销（加入黑名单）
 * 与auth-service共享同一个Redis存储
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedissonClient redissonClient;

    /**
     * Token黑名单Redis键前缀
     * 注意：必须与auth-service的前缀保持一致
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    /**
     * Token默认过期时间（秒）
     */
    @Value("${jwt.expiration:7200000}")
    private long tokenExpirationMs;

    /**
     * 检查Token是否在黑名单中
     *
     * @param token JWT Token
     * @return true表示在黑名单中（已注销），false表示未注销
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
            boolean blacklisted = bucket.isExists();

            if (blacklisted) {
                log.debug("Token已在黑名单中: {}...", token.substring(0, Math.min(20, token.length())));
            }

            return blacklisted;
        } catch (Exception e) {
            log.error("检查Token黑名单失败: {}", e.getMessage());
            // 黑名单检查失败时，出于安全考虑，默认认为Token无效
            // 这可能导致用户需要重新登录，但比允许已注销的Token通过更安全
            return true;
        }
    }

    /**
     * 将Token加入黑名单
     * 注意：通常由auth-service调用，此方法供测试或特殊场景使用
     *
     * @param token JWT Token
     * @param reason 加入黑名单的原因
     */
    public void addToBlacklist(String token, String reason) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            RBucket<String> bucket = redissonClient.getBucket(blacklistKey);

            // 设置过期时间与Token过期时间一致
            long expirySeconds = tokenExpirationMs / 1000;
            bucket.set(reason != null ? reason : "blacklisted", expirySeconds, TimeUnit.SECONDS);

            log.info("Token已加入黑名单: reason={}, expirySeconds={}",
                    reason, expirySeconds);
        } catch (Exception e) {
            log.error("将Token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 从黑名单中移除Token
     * 注意：通常不需要调用，Token会自动过期
     *
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
            bucket.delete();

            log.info("Token已从黑名单中移除");
        } catch (Exception e) {
            log.error("从黑名单移除Token失败: {}", e.getMessage());
        }
    }
}
