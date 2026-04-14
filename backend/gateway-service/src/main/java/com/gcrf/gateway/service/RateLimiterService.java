package com.gcrf.gateway.service;

import com.gcrf.gateway.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流服务
 *
 * 使用Redis实现分布式限流，支持：
 * - 按IP限流
 * - 按用户ID限流
 * - 路径特定限流规则
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedissonClient redissonClient;
    private final RateLimitProperties rateLimitProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 缓存已初始化的限流器，避免重复初始化
     */
    private final Map<String, Boolean> initializedLimiters = new ConcurrentHashMap<>();

    /**
     * 检查是否允许请求通过
     *
     * @param path 请求路径
     * @param clientIp 客户端IP
     * @param userId 用户ID（可为null表示匿名）
     * @return true表示允许，false表示被限流
     */
    public boolean isAllowed(String path, String clientIp, Long userId) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        // 检查路径特定限流规则
        for (Map.Entry<String, RateLimitProperties.PathRateLimit> entry :
                rateLimitProperties.getPaths().entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                RateLimitProperties.PathRateLimit pathLimit = entry.getValue();
                if (pathLimit.isEnabled()) {
                    return checkPathRateLimit(path, clientIp, userId, entry.getKey(), pathLimit);
                }
            }
        }

        // 使用默认限流规则
        return checkDefaultRateLimit(path, clientIp, userId);
    }

    /**
     * 检查路径特定限流规则
     */
    private boolean checkPathRateLimit(String path, String clientIp, Long userId,
                                       String pathPattern, RateLimitProperties.PathRateLimit pathLimit) {
        String rateLimitKey = buildRateLimitKey(pathPattern, clientIp, userId, pathLimit.getType());
        return tryAcquire(rateLimitKey, pathLimit.getRequestsPerMinute());
    }

    /**
     * 检查默认限流规则
     */
    private boolean checkDefaultRateLimit(String path, String clientIp, Long userId) {
        RateLimitProperties.RateLimitType type;
        int limit;

        if (userId != null) {
            // 认证用户按USER限流
            type = RateLimitProperties.RateLimitType.USER;
            limit = rateLimitProperties.getAuthenticatedRequestsPerMinute();
        } else {
            // 匿名用户按IP限流
            type = RateLimitProperties.RateLimitType.IP;
            limit = rateLimitProperties.getAnonymousRequestsPerMinute();
        }

        String rateLimitKey = buildRateLimitKey("default", clientIp, userId, type);
        return tryAcquire(rateLimitKey, limit);
    }

    /**
     * 构建限流键
     */
    private String buildRateLimitKey(String pathPattern, String clientIp, Long userId,
                                     RateLimitProperties.RateLimitType type) {
        String prefix = rateLimitProperties.getRedisKeyPrefix();
        String sanitizedPath = pathPattern.replace("/", "_").replace("*", "x");

        return switch (type) {
            case IP -> String.format("%s%s:ip:%s", prefix, sanitizedPath, clientIp);
            case USER -> String.format("%s%s:user:%d", prefix, sanitizedPath, userId != null ? userId : 0);
            case GLOBAL -> String.format("%s%s:global", prefix, sanitizedPath);
        };
    }

    /**
     * 尝试获取令牌
     */
    private boolean tryAcquire(String key, int permitsPerMinute) {
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

            // 初始化限流器（如果尚未初始化）
            if (!initializedLimiters.containsKey(key)) {
                // 使用trySetRate避免重复设置
                boolean setResult = rateLimiter.trySetRate(
                        RateType.OVERALL,
                        permitsPerMinute,
                        1,
                        RateIntervalUnit.MINUTES
                );
                if (setResult) {
                    // 设置过期时间，防止Redis内存泄漏
                    rateLimiter.expire(java.time.Duration.ofMinutes(5));
                }
                initializedLimiters.put(key, true);
            }

            boolean acquired = rateLimiter.tryAcquire(1);
            if (!acquired) {
                log.warn("请求被限流: key={}", key);
            }
            return acquired;

        } catch (Exception e) {
            log.error("限流检查失败，默认放行: key={}, error={}", key, e.getMessage());
            // 限流服务异常时默认放行，避免影响正常业务
            return true;
        }
    }

    /**
     * 获取剩余可用请求数
     *
     * @param path 请求路径
     * @param clientIp 客户端IP
     * @param userId 用户ID
     * @return 剩余可用请求数
     */
    public long getRemainingRequests(String path, String clientIp, Long userId) {
        try {
            RateLimitProperties.RateLimitType type = userId != null ?
                    RateLimitProperties.RateLimitType.USER :
                    RateLimitProperties.RateLimitType.IP;
            String rateLimitKey = buildRateLimitKey("default", clientIp, userId, type);
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);
            return rateLimiter.availablePermits();
        } catch (Exception e) {
            log.error("获取剩余请求数失败: error={}", e.getMessage());
            return -1;
        }
    }
}
