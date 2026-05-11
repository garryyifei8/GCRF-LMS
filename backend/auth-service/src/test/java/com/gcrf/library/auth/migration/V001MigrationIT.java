package com.gcrf.library.auth.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-test: verifies V001__iam_baseline.sql applies cleanly to a fresh PostgreSQL 15
 * container and that all seed data (10 roles, 12 permissions, admin user bound to
 * REGION_ADMIN with all 12 permissions) is present after migration.
 *
 * <p>Note: The original plan text says "11 permissions" but the SQL seed correctly
 * inserts 12 codes (book.read/write, circulation.read/write, reader.read/write,
 * system.read/write, analytics.read, org.read/write, opac.read). Assertions here
 * use the correct count of 12.
 *
 * @author GCRF Team
 * @since 2026-05-11
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class V001MigrationIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        // Ensure Flyway also uses the same container DB
        r.add("spring.flyway.url", PG::getJdbcUrl);
        r.add("spring.flyway.user", PG::getUsername);
        r.add("spring.flyway.password", PG::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedDataIsPresent() {
        // 10 system roles seeded
        Integer roleCount = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_role WHERE is_system = true", Integer.class);
        assertThat(roleCount).isEqualTo(10);

        // 12 permissions seeded (plan text says 11 but SQL has 12; using correct count)
        Integer permCount = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_permission", Integer.class);
        assertThat(permCount).isEqualTo(12);

        // admin user bound to REGION_ADMIN
        Integer adminBound = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_user_role ur " +
            "JOIN gcrf_region.users u ON u.id = ur.user_id " +
            "JOIN gcrf_region.auth_role r ON r.id = ur.role_id " +
            "WHERE u.user_id = 'admin' AND r.code = 'REGION_ADMIN'", Integer.class);
        assertThat(adminBound).isEqualTo(1);

        // REGION_ADMIN has ALL 12 permissions
        Integer regionAdminPerms = jdbc.queryForObject(
            "SELECT count(*) FROM gcrf_region.auth_role_permission rp " +
            "JOIN gcrf_region.auth_role r ON r.id = rp.role_id " +
            "WHERE r.code = 'REGION_ADMIN'", Integer.class);
        assertThat(regionAdminPerms).isEqualTo(12);  // all permissions
    }
}
