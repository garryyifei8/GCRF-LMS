package com.gcrf.library.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.utils.JwtUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RoleController, PermissionController, and UserRoleController.
 *
 * <p>Tests verify:
 * - GET /api/v1/roles returns the 10 seeded system roles
 * - GET /api/v1/permissions returns the 12 seeded permissions
 * - POST /api/v1/users/{id}/roles succeeds when caller has REGION_ADMIN role
 * - POST /api/v1/users/{id}/roles returns code 403 when caller lacks REGION_ADMIN role
 *
 * <p>NOTE: This test requires a live Redis connection (Redisson) for the refresh-token service
 * that is wired transitively through AuthService. If Redis is unavailable in the CI environment,
 * this test is @Disabled — it will be covered by the full-stack E2E suite in Task 15.
 * Remove @Disabled once TEST_REDIS_HOST is available in CI or a mock is provided.
 */
@Disabled("needs full test stack (Redis) — covered in Task 15 E2E")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RoleControllerIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.url", PG::getJdbcUrl);
        r.add("spring.flyway.user", PG::getUsername);
        r.add("spring.flyway.password", PG::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserMapper userMapper;

    private String tokenForAdmin() {
        User admin = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        return jwtUtil.generateToken(admin.getId().toString(), Map.of(
            "userId", admin.getId(),
            "username", "admin",
            "roles", List.of("REGION_ADMIN"),
            "scope", "REGION"
        ));
    }

    private String tokenForLibrarian(Long uid) {
        return jwtUtil.generateToken(uid.toString(), Map.of(
            "userId", uid,
            "username", "lib1",
            "roles", List.of("LIBRARIAN"),
            "scope", "SCHOOL"
        ));
    }

    @Test
    void getRoles_returns10SystemRoles() throws Exception {
        mvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + tokenForAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void getPermissions_returns12() throws Exception {
        mvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + tokenForAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(12));
    }

    @Test
    void assignRole_byAdmin_succeeds() throws Exception {
        // Seed admin user has id=1; assigning a redundant role is idempotent (no-op if exists)
        mvc.perform(post("/api/v1/users/1/roles")
                .header("Authorization", "Bearer " + tokenForAdmin())
                .contentType("application/json")
                .content("{\"roleCode\":\"LIBRARIAN\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void assignRole_byLibrarian_isDeniedBy403Code() throws Exception {
        // @RequireRole denies via AccessDeniedException → BusinessException → HTTP 200 + code 403
        mvc.perform(post("/api/v1/users/1/roles")
                .header("Authorization", "Bearer " + tokenForLibrarian(99L))
                .contentType("application/json")
                .content("{\"roleCode\":\"LIBRARIAN\"}"))
            .andExpect(status().isOk())  // project convention: BusinessException → HTTP 200
            .andExpect(jsonPath("$.code").value(403));
    }
}
