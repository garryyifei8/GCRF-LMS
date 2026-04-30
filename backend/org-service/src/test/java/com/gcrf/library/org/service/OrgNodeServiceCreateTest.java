package com.gcrf.library.org.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class OrgNodeServiceCreateTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    @Test
    void create_root_REGION_succeeds() {
        OrgNodeCreateDTO dto = new OrgNodeCreateDTO();
        dto.setType("REGION");
        dto.setName("天河区教育局");
        dto.setCode("th_edu");

        OrgNodeVO v = svc.create(dto);

        assertThat(v.getId()).isNotNull();
        assertThat(v.getPath()).isEqualTo(String.valueOf(v.getId()));
    }

    @Test
    void create_child_DISTRICT_under_REGION_succeeds() {
        OrgNodeCreateDTO root = new OrgNodeCreateDTO();
        root.setType("REGION"); root.setName("天河"); root.setCode("th");
        Long rootId = svc.create(root).getId();

        OrgNodeCreateDTO child = new OrgNodeCreateDTO();
        child.setParentId(rootId); child.setType("DISTRICT"); child.setName("石牌"); child.setCode("sp");

        OrgNodeVO v = svc.create(child);

        assertThat(v.getPath()).isEqualTo(rootId + "." + v.getId());
    }

    @Test
    void create_invalidParentType_isRejected() {
        OrgNodeCreateDTO root = new OrgNodeCreateDTO();
        root.setType("REGION"); root.setName("R"); root.setCode("r1");
        Long rootId = svc.create(root).getId();

        OrgNodeCreateDTO bad = new OrgNodeCreateDTO();
        bad.setParentId(rootId); bad.setType("CLASS"); bad.setName("C"); bad.setCode("c1");

        assertThatThrownBy(() -> svc.create(bad))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("CLASS cannot be child of REGION");
    }

    @Test
    void create_duplicateCode_isRejected() {
        OrgNodeCreateDTO a = new OrgNodeCreateDTO();
        a.setType("REGION"); a.setName("A"); a.setCode("dup");
        svc.create(a);

        OrgNodeCreateDTO b = new OrgNodeCreateDTO();
        b.setType("REGION"); b.setName("B"); b.setCode("dup");

        assertThatThrownBy(() -> svc.create(b))
            .hasMessageContaining("dup");
    }
}
