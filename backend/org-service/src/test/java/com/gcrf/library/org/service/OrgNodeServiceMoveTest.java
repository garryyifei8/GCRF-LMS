package com.gcrf.library.org.service;

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
class OrgNodeServiceMoveTest {

    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired OrgNodeService svc;

    @Test
    void move_subtree_rewritesPathOfAllDescendants() {
        // R1 -> D1 -> S1
        // R2
        // move D1 (with S1) to R2
        Long r1 = create("REGION", null, "r1");
        Long r2 = create("REGION", null, "r2");
        Long d1 = create("DISTRICT", r1, "d1");
        Long s1 = create("SCHOOL",   d1, "s1");

        OrgNodeVO moved = svc.move(d1, r2);

        assertThat(moved.getParentId()).isEqualTo(r2);
        assertThat(moved.getPath()).isEqualTo(r2 + "." + d1);

        OrgNodeVO sChild = svc.findById(s1);
        assertThat(sChild.getPath()).isEqualTo(r2 + "." + d1 + "." + s1);
    }

    @Test
    void move_rejectsCycle() {
        Long r = create("REGION", null, "rc");
        Long c = create("DISTRICT", r, "cc");
        // can't move root under its own descendant
        assertThatThrownBy(() -> svc.move(r, c))
            .hasMessageContaining("cycle");
    }

    private Long create(String type, Long parent, String code) {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType(type); d.setParentId(parent); d.setName(code); d.setCode(code);
        return svc.create(d).getId();
    }
}
