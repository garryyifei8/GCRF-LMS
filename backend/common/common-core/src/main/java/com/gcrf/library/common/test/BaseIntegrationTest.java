package com.gcrf.library.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test base class using a singleton PostgreSQL Testcontainer.
 *
 * All integration tests that need a database must extend this class and
 * use @Transactional to ensure data isolation between tests.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @AutoConfigureMockMvc
 * @Transactional
 * @ActiveProfiles("test")
 * class MyControllerIntegrationTest extends BaseIntegrationTest {
 *     // test methods
 * }
 * }
 * </pre>
 *
 * @author GCRF Team
 * @since 2026-04-14
 */
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("gcrf_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
