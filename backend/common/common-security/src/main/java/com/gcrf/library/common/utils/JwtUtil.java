package com.gcrf.library.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
     * JWT密钥
     */
    @Value("${jwt.secret:gcrf-library-management-system-jwt-secret-key-2025}")
    private String secret;

    /**
     * JWT过期时间（毫秒），默认2小时
     */
    @Value("${jwt.expiration:7200000}")
    private Long expiration;

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
     *
     * @return SecretKey
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
