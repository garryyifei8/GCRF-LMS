package com.gcrf.library.org.service;

import com.alibaba.excel.EasyExcel;
import com.gcrf.library.org.domain.dto.OrgImportRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class OrgImportServiceTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired
    OrgImportService importService;

    @Test
    void importExcel_createsHierarchy() throws Exception {
        // build excel in memory: 3 rows (region, district, school)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, OrgImportRow.class).sheet("Sheet1").doWrite(List.of(
            row("REGION",   null,       "th_imp",   "天河区教育局"),
            row("DISTRICT", "th_imp",   "sp_imp",   "石牌街"),
            row("SCHOOL",   "sp_imp",   "syxx_imp", "实验小学")
        ));
        MockMultipartFile mf = new MockMultipartFile("file", "import.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

        var report = importService.importExcel(mf);
        assertThat(report.getCreated()).isEqualTo(3);
        assertThat(report.getFailed()).isEqualTo(0);
    }

    private OrgImportRow row(String type, String parentCode, String code, String name) {
        OrgImportRow r = new OrgImportRow();
        r.setType(type);
        r.setParentCode(parentCode);
        r.setCode(code);
        r.setName(name);
        return r;
    }
}
