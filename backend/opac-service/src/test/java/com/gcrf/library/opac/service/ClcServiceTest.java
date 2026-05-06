package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ClcServiceTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired ClcService clc;

    @Test
    void getTree_loads22Categories() {
        List<ClcNodeVO> tree = clc.getTree();
        assertThat(tree).hasSize(22);
        ClcNodeVO i = tree.stream().filter(n -> "I".equals(n.getCode())).findFirst().orElseThrow();
        assertThat(i.getName()).isEqualTo("文学");
        assertThat(i.getChildren()).extracting("code")
            .contains("I0", "I2", "I3/7");
    }
}
