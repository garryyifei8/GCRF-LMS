package com.gcrf.gateway.integration;

import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway-Auth Integration Test
 * <p>
 * Tests the integration between Gateway service's AuthenticationFilter and JWT token validation.
 * Validates complete authentication flow including:
 * <ul>
 *   <li>Whitelist path access without authentication</li>
 *   <li>Protected path access with valid JWT tokens</li>
 *   <li>Error handling for invalid, expired, or missing tokens</li>
 *   <li>User context propagation via headers</li>
 * </ul>
 *
 * @author GCRF Team
 * @date 2025-11-01
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
@DisplayName("Gateway-Auth Integration Tests")
class GatewayAuthIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret:GCRF_Library_System_Secret_2024_Secure}")
    private String jwtSecret;

    /**
     * Helper: Generate valid JWT token with user claims
     */
    private String generateValidToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", "ADMIN");
        return jwtUtil.generateToken(username, claims);
    }

    /**
     * Helper: Generate expired JWT token for testing token expiration
     */
    private String generateExpiredToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", "ADMIN");

        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 3600000); // 1 hour ago

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(new Date(now.getTime() - 7200000)) // 2 hours ago
                .expiration(expiredDate)
                .signWith(key)
                .compact();
    }

    /**
     * Test 1: Whitelist path accessible without token
     * Expected: 5xx (connection error to dummy backend), NOT 401
     */
    @Test
    @DisplayName("Test 1: Whitelist path /api/v1/auth/login accessible without token")
    void testWhitelistPathLoginAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx (can't connect to dummy backend), NOT 401
    }

    /**
     * Test 2: Actuator health accessible without token
     * Expected: 200 OK
     */
    @Test
    @DisplayName("Test 2: Actuator health endpoint accessible without token")
    void testActuatorHealthAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isNotEmpty();
    }

    /**
     * Test 3: Protected path without token returns 401
     * Expected: 401 Unauthorized
     */
    @Test
    @DisplayName("Test 3: Protected path /api/v1/books without token returns 401")
    void testProtectedPathWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/books")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").exists();
    }

    /**
     * Test 4: Protected path with invalid token returns 401
     * Expected: 401 Unauthorized
     */
    @Test
    @DisplayName("Test 4: Protected path with invalid JWT token returns 401")
    void testProtectedPathWithInvalidTokenReturns401() {
        webTestClient.get()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token_xyz")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").exists();
    }

    /**
     * Test 5: Protected path with valid token passes authentication
     * Expected: 5xx (connection error to dummy backend), NOT 401
     */
    @Test
    @DisplayName("Test 5: Protected path with valid token passes authentication")
    void testProtectedPathWithValidTokenPasses() {
        String token = generateValidToken(100L, "testuser");

        webTestClient.get()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx (connection error), authentication passed
    }

    /**
     * Test 6: Valid token adds user context headers to downstream request
     * Note: Since we don't have downstream services, we test that request passes (not 401)
     * The actual header forwarding is validated in unit tests
     */
    @Test
    @DisplayName("Test 6: Valid token allows request to pass (headers added internally)")
    void testValidTokenAddsUserContextHeaders() {
        Long userId = 200L;
        String username = "admin";
        String token = generateValidToken(userId, username);

        webTestClient.get()
                .uri("/api/v1/readers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx (connection error), authentication passed

        // Note: Actual header verification done in AuthenticationFilterTest.testValidTokenAddsUserInfoToHeaders()
    }

    /**
     * Test 7: Malformed Authorization header returns 401
     * Expected: 401 Unauthorized
     */
    @Test
    @DisplayName("Test 7: Malformed Authorization header (no Bearer prefix) returns 401")
    void testMalformedAuthorizationHeaderReturns401() {
        webTestClient.get()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "InvalidPrefix token123")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").exists();
    }

    /**
     * Test 8: Missing Bearer prefix returns 401
     * Expected: 401 Unauthorized
     */
    @Test
    @DisplayName("Test 8: Token without Bearer prefix returns 401")
    void testTokenWithoutBearerPrefixReturns401() {
        String token = generateValidToken(300L, "user");

        webTestClient.get()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, token) // Missing "Bearer " prefix
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").exists();
    }

    /**
     * Test 9: Expired token returns 401
     * Expected: 401 Unauthorized
     */
    @Test
    @DisplayName("Test 9: Expired JWT token returns 401")
    void testExpiredTokenReturns401() {
        String expiredToken = generateExpiredToken(400L, "expireduser");

        webTestClient.get()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").exists();
    }

    /**
     * Test 10: Different HTTP methods work with valid token
     * Expected: POST, PUT, DELETE should also pass authentication (5xx, not 401)
     */
    @Test
    @DisplayName("Test 10a: POST method works with valid token")
    void testPostMethodWithValidToken() {
        String token = generateValidToken(500L, "postuser");

        webTestClient.post()
                .uri("/api/v1/books")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx, authentication passed
    }

    @Test
    @DisplayName("Test 10b: PUT method works with valid token")
    void testPutMethodWithValidToken() {
        String token = generateValidToken(600L, "putuser");

        webTestClient.put()
                .uri("/api/v1/books/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx, authentication passed
    }

    @Test
    @DisplayName("Test 10c: DELETE method works with valid token")
    void testDeleteMethodWithValidToken() {
        String token = generateValidToken(700L, "deleteuser");

        webTestClient.delete()
                .uri("/api/v1/books/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx, authentication passed
    }

    /**
     * Bonus Test: Multiple whitelist paths are accessible
     */
    @Test
    @DisplayName("Bonus Test: Register endpoint (whitelist) accessible without token")
    void testRegisterEndpointAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .exchange()
                .expectStatus().is5xxServerError(); // Expected 5xx (connection error), NOT 401
    }
}
