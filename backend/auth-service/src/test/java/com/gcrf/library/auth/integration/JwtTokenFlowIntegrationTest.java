package com.gcrf.library.auth.integration;

import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT Token Flow Integration Test
 *
 * Tests the complete JWT token lifecycle from generation to validation across services.
 *
 * Test Coverage (7 tests - comprehensive JWT lifecycle):
 * 1. Token generation creates valid token
 * 2. Token contains correct claims (userId, username, userType)
 * 3. Token validation succeeds with correct secret
 * 4. Token validation fails with wrong secret
 * 5. Expired token is rejected
 * 6. Token expiration is enforced
 * 7. Token signature validation
 *
 * @author GCRF Team
 * @since 2025-11-01
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "jwt.secret=gcrf-library-management-system-jwt-secret-key-2025",
        "jwt.expiration=7200000"
})
class JwtTokenFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    private Map<String, Object> testClaims;

    @BeforeEach
    void setUp() {
        testClaims = createTestClaims(12345L, "testuser", "ADMIN");
    }

    // ==================== Test 1: Token Generation Creates Valid Token ====================

    @Test
    void testTokenGenerationCreatesValidToken() {
        // Arrange
        String subject = "testuser";

        // Act
        String token = jwtUtil.generateToken(subject, testClaims);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    // ==================== Test 2: Token Contains Correct Claims ====================

    @Test
    void testTokenContainsCorrectClaims() {
        // Arrange
        String subject = "testuser";
        Long expectedUserId = 12345L;
        String expectedUsername = "testuser";

        // Act
        String token = jwtUtil.generateToken(subject, testClaims);

        // Assert - Extract and verify claims
        assertThat(jwtUtil.getSubject(token)).isEqualTo(subject);
        assertThat(jwtUtil.getUserId(token)).isEqualTo(expectedUserId);
        assertThat(jwtUtil.getUsername(token)).isEqualTo(expectedUsername);

        // Verify all claims are present
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.get("userId", Long.class)).isEqualTo(expectedUserId);
        assertThat(claims.get("username", String.class)).isEqualTo(expectedUsername);
        assertThat(claims.get("userType", String.class)).isEqualTo("ADMIN");
    }

    // ==================== Test 3: Token Validation Succeeds With Correct Secret ====================

    @Test
    void testTokenValidationSucceedsWithCorrectSecret() {
        // Arrange
        String subject = "validuser";
        String token = jwtUtil.generateToken(subject, testClaims);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    // ==================== Test 4: Token Validation Fails With Wrong Secret ====================

    @Test
    void testTokenValidationFailsWithWrongSecret() {
        // Arrange - Generate token with our JwtUtil
        String subject = "testuser";
        String token = jwtUtil.generateToken(subject, testClaims);

        // Act - Try to validate with wrong secret
        String wrongSecret = "wrong-secret-key-that-should-fail-validation-12345";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

        // Assert - Parsing with wrong secret should throw exception
        assertThatThrownBy(() -> {
            Jwts.parser()
                    .verifyWith(wrongKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("signature");
    }

    // ==================== Test 5: Expired Token Is Rejected ====================

    @Test
    void testExpiredTokenIsRejected() {
        // Arrange - Create expired token manually using JJWT
        String secret = "gcrf-library-management-system-jwt-secret-key-2025";
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 10000); // 10 seconds ago

        String expiredToken = Jwts.builder()
                .subject("testuser")
                .claims(testClaims)
                .issuedAt(new Date(now.getTime() - 20000)) // Issued 20 seconds ago
                .expiration(expiredDate) // Expired 10 seconds ago
                .signWith(secretKey)
                .compact();

        // Act
        boolean isValid = jwtUtil.validateToken(expiredToken);
        boolean isExpired = jwtUtil.isTokenExpired(expiredToken);

        // Assert
        assertThat(isValid).isFalse(); // Expired tokens are not valid
        assertThat(isExpired).isTrue(); // Token should be marked as expired
    }

    // ==================== Test 6: Token Expiration Is Enforced ====================

    @Test
    void testTokenExpirationIsEnforced() {
        // Arrange - Create token with very short expiration (1 second)
        String secret = "gcrf-library-management-system-jwt-secret-key-2025";
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date shortExpiryDate = new Date(now.getTime() + 1000); // 1 second from now

        String shortLivedToken = Jwts.builder()
                .subject("testuser")
                .claims(testClaims)
                .issuedAt(now)
                .expiration(shortExpiryDate)
                .signWith(secretKey)
                .compact();

        // Act - Token should be valid initially
        boolean isValidInitially = jwtUtil.validateToken(shortLivedToken);
        boolean isExpiredInitially = jwtUtil.isTokenExpired(shortLivedToken);

        // Assert - Initially valid
        assertThat(isValidInitially).isTrue();
        assertThat(isExpiredInitially).isFalse();

        // Wait for token to expire
        try {
            Thread.sleep(1100); // Wait 1.1 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act - Check again after expiration
        boolean isValidAfterExpiry = jwtUtil.validateToken(shortLivedToken);
        boolean isExpiredAfterExpiry = jwtUtil.isTokenExpired(shortLivedToken);

        // Assert - Should be expired now
        assertThat(isValidAfterExpiry).isFalse();
        assertThat(isExpiredAfterExpiry).isTrue();
    }

    // ==================== Test 7: Token Signature Validation ====================

    @Test
    void testTokenSignatureValidation() {
        // Arrange - Generate valid token
        String subject = "testuser";
        String validToken = jwtUtil.generateToken(subject, testClaims);

        // Split token into parts
        String[] tokenParts = validToken.split("\\.");
        assertThat(tokenParts).hasSize(3);

        // Act - Tamper with the payload (change one character)
        String tamperedPayload = tokenParts[1].substring(0, tokenParts[1].length() - 1) + "X";
        String tamperedToken = tokenParts[0] + "." + tamperedPayload + "." + tokenParts[2];

        // Assert - Tampered token should fail validation
        assertThat(jwtUtil.validateToken(tamperedToken)).isFalse();

        // Parsing tampered token should throw exception
        assertThatThrownBy(() -> jwtUtil.parseToken(tamperedToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JWT Token解析失败");

        // Original token should still be valid
        assertThat(jwtUtil.validateToken(validToken)).isTrue();
    }

    // ==================== Helper Methods ====================

    /**
     * Create test claims for JWT token
     *
     * @param userId User ID
     * @param username Username
     * @param userType User type (e.g., ADMIN, USER, LIBRARIAN)
     * @return Map of claims
     */
    private Map<String, Object> createTestClaims(Long userId, String username, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        return claims;
    }
}
