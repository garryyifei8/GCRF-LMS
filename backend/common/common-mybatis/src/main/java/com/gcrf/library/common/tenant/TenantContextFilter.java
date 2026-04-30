package com.gcrf.library.common.tenant;

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

/**
 * Servlet Filter — 从 Authorization: Bearer <jwt> 提取 tenant claim 并写入
 * TenantContext，请求结束后清理 ThreadLocal。
 */
@Slf4j
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtil.parseToken(auth.substring(7));
                    String tenant = claims.get("tenant", String.class);
                    if (tenant != null && !tenant.isBlank()) {
                        TenantContext.setTenant(tenant);
                    }
                } catch (Exception e) {
                    log.debug("tenant filter: invalid token, ignoring: {}", e.getMessage());
                }
            }
            onChainBeforeClear(req, res);
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }

    /** 测试钩子。生产代码空实现。 */
    protected void onChainBeforeClear(HttpServletRequest req, HttpServletResponse res) {
        // intentionally empty
    }
}
