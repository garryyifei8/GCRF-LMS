package com.gcrf.library.org.service;

import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
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

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SchoolProvisioningServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService orgService;
    @Autowired SchoolProvisioningService prov;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createSchool_buildsSchemaAndRunsMigration() {
        OrgNodeCreateDTO region = new OrgNodeCreateDTO();
        region.setType("REGION"); region.setName("天河"); region.setCode("th_prov");
        Long regionId = orgService.create(region).getId();

        SchoolCreateDTO sd = new SchoolCreateDTO();
        sd.setParentId(regionId); sd.setName("实验小学"); sd.setCode("syxx_prov");
        OrgNodeVO school = prov.createSchool(sd);

        assertThat(school.getTenantSchema()).matches("^school_\\d+$");

        String schema = school.getTenantSchema();
        Integer tableCount = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = ?",
            Integer.class, schema);
        assertThat(tableCount).isGreaterThanOrEqualTo(4); // school_meta + reader + book_catalog + book_copy + borrow_record + flyway_schema_history
    }
}
