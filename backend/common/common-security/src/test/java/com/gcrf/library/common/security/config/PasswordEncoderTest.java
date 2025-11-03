package com.gcrf.library.common.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncoder单元测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void testEncodePassword() {
        // Arrange
        String rawPassword = "password123";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
    }

    @Test
    void testEncodePasswordDifferentEachTime() {
        // Arrange
        String rawPassword = "password123";

        // Act
        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // Assert
        assertNotEquals(encodedPassword1, encodedPassword2);
        // But both should match the raw password
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword1));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword2));
    }

    @Test
    void testMatchesCorrectPassword() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testMatchesIncorrectPassword() {
        // Arrange
        String rawPassword = "password123";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testMatchesEmptyPassword() {
        // Arrange
        String rawPassword = "";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testEncodeSpecialCharacters() {
        // Arrange
        String rawPassword = "P@ssw0rd!#$%^&*()";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(matches);
    }

    @Test
    void testEncodeLongPassword() {
        // Arrange
        String rawPassword = "a".repeat(100);

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(matches);
    }

    @Test
    void testEncodeChineseCharacters() {
        // Arrange
        String rawPassword = "密码123";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(matches);
    }

    @Test
    void testMatchesCaseSensitive() {
        // Arrange
        String rawPassword = "Password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matchesLowerCase = passwordEncoder.matches("password123", encodedPassword);
        boolean matchesUpperCase = passwordEncoder.matches("PASSWORD123", encodedPassword);
        boolean matchesCorrect = passwordEncoder.matches("Password123", encodedPassword);

        // Assert
        assertFalse(matchesLowerCase);
        assertFalse(matchesUpperCase);
        assertTrue(matchesCorrect);
    }

    @Test
    void testMatchesWithWhitespace() {
        // Arrange
        String rawPassword = "password 123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean matchesWithSpace = passwordEncoder.matches("password 123", encodedPassword);
        boolean matchesWithoutSpace = passwordEncoder.matches("password123", encodedPassword);

        // Assert
        assertTrue(matchesWithSpace);
        assertFalse(matchesWithoutSpace);
    }

    @Test
    void testEncodeNullPassword() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            passwordEncoder.encode(null)
        );
    }

    @Test
    void testMatchesNullRawPassword() {
        // Arrange
        String encodedPassword = passwordEncoder.encode("password123");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            passwordEncoder.matches(null, encodedPassword)
        );
    }

    @Test
    void testMatchesNullEncodedPassword() {
        // Act
        boolean matches = passwordEncoder.matches("password123", null);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testBCryptEncodedLength() {
        // Arrange
        String rawPassword = "test";

        // Act
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Assert
        // BCrypt always produces 60 character strings
        assertEquals(60, encodedPassword.length());
    }

    @Test
    void testMultipleEncodingsIndependent() {
        // Arrange
        String password1 = "password1";
        String password2 = "password2";

        // Act
        String encoded1 = passwordEncoder.encode(password1);
        String encoded2 = passwordEncoder.encode(password2);

        // Assert
        assertTrue(passwordEncoder.matches(password1, encoded1));
        assertTrue(passwordEncoder.matches(password2, encoded2));
        assertFalse(passwordEncoder.matches(password1, encoded2));
        assertFalse(passwordEncoder.matches(password2, encoded1));
    }

    @Test
    void testCommonPasswordPatterns() {
        // Arrange
        String[] passwords = {
            "123456",
            "password",
            "admin123",
            "user@2024",
            "Test123!@#"
        };

        // Act & Assert
        for (String password : passwords) {
            String encoded = passwordEncoder.encode(password);
            assertTrue(passwordEncoder.matches(password, encoded));
            assertNotEquals(password, encoded);
        }
    }
}
