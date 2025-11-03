package com.gcrf.library.auth.config;

import com.gcrf.library.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JWT认证过滤器测试
 *
 * 测试覆盖:
 * - Token提取和验证 (2个测试)
 * - 无效Token处理 (3个测试)
 * - 缺失Authorization头 (2个测试)
 * - SecurityContext设置 (1个测试)
 * - 过滤器链传递 (2个测试)
 *
 * @author GCRF Team
 * @since 2025-10-30
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    // ==================== Token提取和验证测试 (2个) ====================

    @Test
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String username = "testuser";
        Long userId = 123L;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsername(validToken)).thenReturn(username);
        when(jwtUtil.getUserId(validToken)).thenReturn(userId);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(username);
        assertThat(authentication.getAuthorities())
                .hasSize(1)
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).getUsername(validToken);
        verify(jwtUtil).getUserId(validToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ValidToken_SetsAuthenticationDetails() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String username = "adminuser";
        Long userId = 456L;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsername(validToken)).thenReturn(username);
        when(jwtUtil.getUserId(validToken)).thenReturn(userId);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getDetails()).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getCredentials()).isNull();

        verify(filterChain).doFilter(request, response);
    }

    // ==================== 无效Token处理测试 (3个) ====================

    @Test
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        String authHeader = "Bearer " + invalidToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).getUsername(anyString());
        verify(jwtUtil, never()).getUserId(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_MalformedToken() throws ServletException, IOException {
        // Arrange
        String malformedToken = "malformed-token";
        String authHeader = "Bearer " + malformedToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(malformedToken)).thenThrow(new RuntimeException("Malformed JWT"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(malformedToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ExpiredToken() throws ServletException, IOException {
        // Arrange
        String expiredToken = "expired.jwt.token";
        String authHeader = "Bearer " + expiredToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(expiredToken)).thenThrow(new RuntimeException("Token expired"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil).validateToken(expiredToken);
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 缺失Authorization头测试 (2个) ====================

    @Test
    void testDoFilterInternal_NoAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_InvalidAuthorizationScheme() throws ServletException, IOException {
        // Arrange - 使用错误的认证方案 (Basic instead of Bearer)
        String authHeader = "Basic dXNlcjpwYXNz";

        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== SecurityContext设置测试 (1个) ====================

    @Test
    void testDoFilterInternal_SecurityContextSet() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;
        String username = "securityuser";
        Long userId = 789L;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsername(validToken)).thenReturn(username);
        when(jwtUtil.getUserId(validToken)).thenReturn(userId);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(username);
        assertThat(authentication.getAuthorities())
                .extracting(auth -> ((SimpleGrantedAuthority) auth).getAuthority())
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    // ==================== 过滤器链传递测试 (2个) ====================

    @Test
    void testDoFilterInternal_FilterChainContinues_WithValidToken() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String authHeader = "Bearer " + validToken;

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsername(validToken)).thenReturn("user");
        when(jwtUtil.getUserId(validToken)).thenReturn(1L);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_FilterChainContinues_WithoutToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ==================== 边界情况测试 (2个) ====================

    @Test
    void testDoFilterInternal_EmptyBearerToken() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer ";

        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WhitespaceToken() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer    ";

        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
