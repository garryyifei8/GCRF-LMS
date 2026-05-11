package com.gcrf.library.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set default values for @Value fields
        ReflectionTestUtils.setField(jwtUtil, "secret", "gcrf-library-management-system-jwt-secret-key-for-testing-purposes-2025");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 7200000L); // 2 hours
    }

    @Test
    void testGenerateTokenWithSubjectOnly() {
        // Arrange
        String subject = "user123";

        // Act
        String token = jwtUtil.generateToken(subject);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGenerateTokenWithClaims() {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "testuser");
        claims.put("userType", "ADMIN");

        // Act
        String token = jwtUtil.generateToken(subject, claims);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testParseToken() {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "testuser");
        String token = jwtUtil.generateToken(subject, claims);

        // Act
        Claims parsedClaims = jwtUtil.parseToken(token);

        // Assert
        assertNotNull(parsedClaims);
        assertEquals(subject, parsedClaims.getSubject());
        assertEquals(1L, parsedClaims.get("userId", Long.class));
        assertEquals("testuser", parsedClaims.get("username", String.class));
    }

    @Test
    void testGetSubject() {
        // Arrange
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Act
        String extractedSubject = jwtUtil.getSubject(token);

        // Assert
        assertEquals(subject, extractedSubject);
    }

    @Test
    void testGetUserId() {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        String token = jwtUtil.generateToken(subject, claims);

        // Act
        Long userId = jwtUtil.getUserId(token);

        // Assert
        assertEquals(1L, userId);
    }

    @Test
    void testGetUsername() {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", "testuser");
        String token = jwtUtil.generateToken(subject, claims);

        // Act
        String username = jwtUtil.getUsername(token);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void testValidateTokenValid() {
        // Arrange
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenInvalid() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateTokenExpired() {
        // Arrange
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Already expired
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Reset expiration for validation
        ReflectionTestUtils.setField(jwtUtil, "expiration", 7200000L);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsTokenExpiredFalse() {
        // Arrange
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    void testIsTokenExpiredTrue() {
        // Arrange
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Already expired
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Reset expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", 7200000L);

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    void testRefreshToken() throws InterruptedException {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "testuser");
        String oldToken = jwtUtil.generateToken(subject, claims);

        // Wait a moment to ensure timestamp difference (at least 1 second for different iat)
        Thread.sleep(1000);

        // Act
        String newToken = jwtUtil.refreshToken(oldToken);

        // Assert
        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);

        // Verify the subject and claims are preserved
        assertEquals(subject, jwtUtil.getSubject(newToken));
        assertEquals(1L, jwtUtil.getUserId(newToken));
        assertEquals("testuser", jwtUtil.getUsername(newToken));
    }

    @Test
    void testParseTokenWithTamperedSignature() {
        // Arrange
        String subject = "user123";
        String token = jwtUtil.generateToken(subject);

        // Tamper with the signature (last part)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tamperedsignature";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> jwtUtil.parseToken(tamperedToken));
    }

    @Test
    void testGenerateTokenWithMultipleClaims() {
        // Arrange
        String subject = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 123L);
        claims.put("username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("role", "ADMIN");
        claims.put("department", "IT");

        // Act
        String token = jwtUtil.generateToken(subject, claims);
        Claims parsedClaims = jwtUtil.parseToken(token);

        // Assert
        assertEquals(123L, parsedClaims.get("userId", Long.class));
        assertEquals("testuser", parsedClaims.get("username", String.class));
        assertEquals("test@example.com", parsedClaims.get("email", String.class));
        assertEquals("ADMIN", parsedClaims.get("role", String.class));
        assertEquals("IT", parsedClaims.get("department", String.class));
    }

    @Test
    void testTokenWithEmptySubject() {
        // Arrange
        String subject = "";

        // Act
        String token = jwtUtil.generateToken(subject);
        String extractedSubject = jwtUtil.getSubject(token);

        // Assert
        // Empty subject results in null when extracted
        assertNull(extractedSubject);
    }

    @Test
    void testTokenWithSpecialCharactersInSubject() {
        // Arrange
        String subject = "user@example.com#123$456";

        // Act
        String token = jwtUtil.generateToken(subject);
        String extractedSubject = jwtUtil.getSubject(token);

        // Assert
        assertEquals(subject, extractedSubject);
    }

    @Test
    void testValidateTokenWithMalformedToken() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token.format";

        // Act
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsTokenExpiredWithInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token";

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(invalidToken);

        // Assert
        assertTrue(isExpired); // Invalid tokens are considered expired
    }

    @Test
    void testKeyStrength_atLeast512Bits() {
        // 当 secret 长度 < 64 字节（512 bits）时应抛错或自动升级
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "too-short-secret-key");  // ~20 chars = 160 bits
        ReflectionTestUtils.setField(util, "expiration", 7200000L);
        assertThrows(IllegalStateException.class,
            () -> util.generateToken("user", Map.of()),
            "Expected IllegalStateException when secret < 64 bytes");
    }

    @Test
    void testGenerateRichToken_carriesAllClaims() {
        Map<String, Object> claims = Map.of(
            "userId", 42L,
            "username", "alice",
            "tenant", "school_000001",
            "tenantId", 1L,
            "roles", java.util.List.of("LIBRARIAN", "TEACHER"),
            "scope", "SCHOOL"
        );
        String token = jwtUtil.generateToken("42", claims);

        Claims parsed = jwtUtil.parseToken(token);
        assertEquals("42", parsed.getSubject());
        assertEquals(42, ((Number) parsed.get("userId")).longValue());
        assertEquals("school_000001", parsed.get("tenant", String.class));
        assertEquals("SCHOOL", parsed.get("scope", String.class));
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = parsed.get("roles", java.util.List.class);
        assertTrue(roles.contains("LIBRARIAN"));
    }
}
