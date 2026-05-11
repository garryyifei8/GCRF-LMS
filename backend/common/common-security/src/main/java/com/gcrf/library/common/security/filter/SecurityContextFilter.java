package com.gcrf.library.common.security.filter;

import com.gcrf.library.common.security.context.Scope;
import com.gcrf.library.common.security.context.SecurityContext;
import com.gcrf.library.common.security.context.SecurityContextHolder;
import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从 Authorization: Bearer 头解析 JWT，将富化的 claims 写入 ThreadLocal SecurityContext。
 * 请求结束后（finally 块）自动 clear，防止内存泄漏。
 *
 * <p>运行顺序：{@code Ordered.HIGHEST_PRECEDENCE + 50}，早于 TenantContextFilter
 * ({@code HIGHEST_PRECEDENCE + 100})，确保后者能读到 SecurityContext。
 *
 * @author GCRF Team
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityContextFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = jwtUtil.parseToken(token);
                    SecurityContextHolder.set(toContext(claims));
                } catch (Exception ex) {
                    // Don't fail the request here; downstream auth filters / @RequireRole will deny.
                    log.debug("SecurityContextFilter: invalid token, ignoring. {}", ex.getMessage());
                }
            }
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private static SecurityContext toContext(Claims c) {
        Long userId = optLong(c, "userId");
        Long tenantId = optLong(c, "tenantId");
        Object rolesObj = c.get("roles");
        @SuppressWarnings("unchecked")
        List<String> roles = rolesObj instanceof List ? (List<String>) rolesObj : List.of();
        Scope scope;
        try {
            String s = c.get("scope", String.class);
            scope = s == null ? Scope.SELF : Scope.valueOf(s);
        } catch (IllegalArgumentException e) {
            scope = Scope.SELF;
        }
        return SecurityContext.builder()
            .userId(userId)
            .username(c.get("username", String.class))
            .tenant(c.get("tenant", String.class))
            .tenantId(tenantId)
            .roles(roles)
            .scope(scope)
            .orgPath(c.get("orgPath", String.class))
            .build();
    }

    private static Long optLong(Claims c, String key) {
        Object v = c.get(key);
        return v == null ? null : ((Number) v).longValue();
    }
}
