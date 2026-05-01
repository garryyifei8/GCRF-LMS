package com.gcrf.library.org.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class OrgNodeControllerTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void post_creates_then_get_returns() throws Exception {
        OrgNodeCreateDTO d = new OrgNodeCreateDTO();
        d.setType("REGION");
        d.setName("天河");
        d.setCode("th_ctrl");

        mvc.perform(post("/api/v1/org/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(d)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.code").value("th_ctrl"));

        mvc.perform(get("/api/v1/org/nodes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.code=='th_ctrl')]").exists());
    }
}
