package com.gcrf.library.org.service;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.dto.OrgNodeUpdateDTO;
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
class OrgNodeServiceUpdateDeleteTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    private Long createRoot(String code) {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType("REGION");
        d.setName("R");
        d.setCode(code);
        return svc.create(d).getId();
    }

    @Test
    void update_changesNameAndStatus() {
        Long id = createRoot("u1");
        OrgNodeUpdateDTO d = new OrgNodeUpdateDTO();
        d.setName("天河区教育局-改");
        d.setStatus("INACTIVE");

        OrgNodeVO v = svc.update(id, d);

        assertThat(v.getName()).isEqualTo("天河区教育局-改");
        assertThat(v.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void delete_leafNode_succeeds() {
        Long id = createRoot("d1");
        svc.delete(id);
        assertThatThrownBy(() -> svc.findById(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_nodeWithChildren_isRejected() {
        Long parent = createRoot("d2");
        OrgNodeCreateDTO child = new OrgNodeCreateDTO();
        child.setParentId(parent);
        child.setType("DISTRICT");
        child.setName("c");
        child.setCode("c1");
        svc.create(child);

        assertThatThrownBy(() -> svc.delete(parent))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("has children");
    }
}
