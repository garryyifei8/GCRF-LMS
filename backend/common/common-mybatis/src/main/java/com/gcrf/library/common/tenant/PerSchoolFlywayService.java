package com.gcrf.library.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;

/**
 * 给指定 PostgreSQL schema 跑 per-school 模板 migration。
 * 由 SchoolProvisioningService 在新建学校时调用。
 */
@Slf4j
@RequiredArgsConstructor
public class PerSchoolFlywayService {

    private static final String DEFAULT_LOCATION = "classpath:db/migration/per-school";

    private final DataSource dataSource;

    public MigrateResult migrateSchool(String schemaName) {
        return migrateSchool(schemaName, DEFAULT_LOCATION);
    }

    public MigrateResult migrateSchool(String schemaName, String migrationLocation) {
        if (schemaName == null || !schemaName.matches("^[a-z][a-z0-9_]+$")) {
            throw new IllegalArgumentException("invalid schema name: " + schemaName);
        }
        log.info("running per-school flyway for schema={}, location={}", schemaName, migrationLocation);

        Flyway fw = Flyway.configure()
            .dataSource(dataSource)
            .schemas(schemaName)
            .defaultSchema(schemaName)
            .locations(migrationLocation)
            .placeholders(java.util.Map.of("schema", schemaName))
            .baselineOnMigrate(true)
            .table("flyway_schema_history") // table inside the school schema
            .load();

        MigrateResult result = fw.migrate();
        log.info("per-school migration done: schema={}, migrationsExecuted={}",
                 schemaName, result.migrationsExecuted);
        return result;
    }
}
