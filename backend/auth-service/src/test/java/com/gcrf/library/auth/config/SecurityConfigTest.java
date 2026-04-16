package com.gcrf.library.auth.config;

import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig配置测试
 *
 * 测试覆盖:
 * - 密码编码器配置 (1个测试)
 * - 公开端点访问控制 (6个测试)
 * - 受保护端点访问控制 (3个测试)
 * - CSRF禁用验证 (1个测试)
 * - Gateway路径访问控制 (2个测试)
 *
 * @author GCRF Team
 * @date 2025-10-13
 * @updated 2025-10-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ==================== 密码编码器配置测试 (1个) ====================

    @Test
    void testPasswordEncoderBean() {
        // Assert
        assertNotNull(passwordEncoder);

        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    // ==================== 公开端点访问控制测试 (6个) ====================

    @Test
    void testPublicEndpoints_AuthHealthAllowed() throws Exception {
        // Test that /api/v1/auth/health endpoint is publicly accessible
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testPublicEndpoints_UsersHealthAllowed() throws Exception {
        // Test that /api/v1/users/health endpoint is publicly accessible
        mockMvc.perform(get("/api/v1/users/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testPublicEndpoints_ActuatorAllowed() throws Exception {
        // Test that actuator endpoints are publicly accessible
        // Expecting 404 or 5xx since health endpoint may not be fully configured in test profile
        // Main goal: verify it's NOT 401 (Unauthorized)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void testPublicEndpoints_SwaggerUIAllowed() throws Exception {
        // Test that Swagger UI endpoints are publicly accessible
        // May return 302 (redirect) or 200 if configured, but should NOT return 401
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void testPublicEndpoints_ApiDocsAllowed() throws Exception {
        // Test that API docs endpoints are publicly accessible
        // May return 200 if configured, but should NOT be 401
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void testPublicEndpoints_Knife4jAllowed() throws Exception {
        // Test that Knife4j endpoints are publicly accessible
        // May return 200 if configured, but should NOT be 401
        mockMvc.perform(get("/doc.html"))
                .andExpect(status().is(not(401)));
    }

    // ==================== 受保护端点访问控制测试 (3个) ====================

    @Test
    void testProtectedEndpoints_RequireAuthentication() throws Exception {
        // Test that protected endpoints require authentication
        mockMvc.perform(get("/api/v1/auth/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoints_WithInvalidToken() throws Exception {
        // Test that invalid token is rejected
        mockMvc.perform(get("/api/v1/auth/info")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoints_WithoutBearerPrefix() throws Exception {
        // Test that token without Bearer prefix is rejected
        mockMvc.perform(get("/api/v1/auth/info")
                        .header("Authorization", "some-token"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CSRF禁用验证测试 (1个) ====================

    @Test
    void testCsrfDisabled_PostRequestAllowed() throws Exception {
        // Test that POST requests work without CSRF token (JWT-based, stateless)
        // This should fail with 401 (no auth) NOT 403 (CSRF missing)
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized()); // 401, not 403 CSRF error
    }

    // ==================== Gateway路径访问控制测试 (2个) ====================

    @Test
    void testGatewayPaths_AuthHealthAllowed() throws Exception {
        // Test Gateway stripped paths (without /api/v1 prefix)
        // These paths are configured as permitAll, so should NOT return 401
        // May return 404/500 if no controller handles them, but NOT 401
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void testGatewayPaths_UsersHealthAllowed() throws Exception {
        // Test Gateway stripped paths (without /api/v1 prefix)
        // These paths are configured as permitAll, so should NOT return 401
        // May return 404/500 if no controller handles them, but NOT 401
        mockMvc.perform(get("/users/health"))
                .andExpect(status().is(not(401)));
    }
}
