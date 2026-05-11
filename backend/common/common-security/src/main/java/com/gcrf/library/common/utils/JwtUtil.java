package com.gcrf.library.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author 张三
 * @date 2025-10-11
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥（至少64字节/512位，满足HS512要求）
     * 生产环境必须通过 JWT_SECRET 环境变量或 jwt.secret 配置覆盖此默认值
     */
    @Value("${jwt.secret:gcrf-library-iam-default-development-secret-key-do-not-use-in-production-2026}")
    private String secret;

    /**
     * JWT过期时间（毫秒），默认30分钟
     */
    @Value("${jwt.expiration:1800000}")
    private Long expiration;

    /**
     * HS512算法要求的最小密钥长度（字节）
     */
    private static final int MIN_SECRET_BYTES = 64;

    private static final String DEV_DEFAULT_SECRET_PREFIX = "gcrf-library-iam-default";

    @PostConstruct
    void validateSecretOnStartup() {
        getSignKey();  // throws IllegalStateException if secret < 64 bytes
        if (secret.startsWith(DEV_DEFAULT_SECRET_PREFIX)) {
            log.warn("SECURITY WARNING: Using default development JWT secret. " +
                     "Set `jwt.secret` (or JWT_SECRET env) in production!");
        }
    }

    /**
     * 生成JWT Token
     *
     * @param subject 主题（通常是用户ID或用户名）
     * @param claims 自定义声明
     * @return JWT Token
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignKey())
                .compact();
    }

    /**
     * 生成JWT Token
     *
     * @param subject 主题
     * @return JWT Token
     */
    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT Token解析失败: {}", e.getMessage());
            throw new RuntimeException("JWT Token解析失败", e);
        }
    }

    /**
     * 从Token中获取主题
     *
     * @param token JWT Token
     * @return 主题
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("JWT Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Token是否过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新Token
     *
     * @param token 旧Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        String subject = claims.getSubject();
        return generateToken(subject, claims);
    }

    /**
     * 获取签名密钥
     * 若密钥长度不足 64 字节（512 bits），则 fail-fast 抛出 IllegalStateException，
     * 避免 HS512 产生 WeakKeyException 或静默降级。
     *
     * @return SecretKey
     * @throws IllegalStateException 当 jwt.secret 不足 64 字节时
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (512 bits) for HS512, got "
                + keyBytes.length + " bytes. Configure spring property `jwt.secret` with a longer value.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
