package com.gcrf.library.common.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PerSchoolFlywayServiceTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void migrateSchema_createsAllTablesInTargetSchema() throws Exception {
        DataSource ds = DataSourceBuilder.create()
            .url(PG.getJdbcUrl())
            .username(PG.getUsername())
            .password(PG.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();

        // create empty schema
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("CREATE SCHEMA school_test");
        }

        new PerSchoolFlywayService(ds).migrateSchool("school_test", "classpath:db/migration/per-school-test");

        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("SET search_path TO school_test");
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='school_test' AND table_name='probe'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
