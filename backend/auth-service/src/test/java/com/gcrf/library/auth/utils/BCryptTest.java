package com.gcrf.library.auth.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码编码测试
 * 用于生成和验证BCrypt密码哈希
 */
public class BCryptTest {

    @Test
    public void testGeneratePasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("==================== BCrypt Password Hash ====================");
        System.out.println("Plain Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("==============================================================");

        // 验证生成的哈希
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification Result: " + matches);
    }

    @Test
    public void testVerifyExistingHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye7wGjMc/NWfMZdQTgEkHToWMkY3p6XrO";

        System.out.println("==================== BCrypt Verification ====================");
        System.out.println("Plain Password: " + password);
        System.out.println("Existing Hash: " + existingHash);

        boolean matches = encoder.matches(password, existingHash);
        System.out.println("Matches: " + matches);
        System.out.println("============================================================");
    }
}
