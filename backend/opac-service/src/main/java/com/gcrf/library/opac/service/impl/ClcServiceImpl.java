package com.gcrf.library.opac.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.opac.domain.vo.ClcNodeVO;
import com.gcrf.library.opac.service.ClcService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClcServiceImpl implements ClcService {

    private final ObjectMapper mapper;
    private List<ClcNodeVO> tree = Collections.emptyList();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("data/clc-22-categories.json").getInputStream()) {
            tree = mapper.readValue(in, new TypeReference<List<ClcNodeVO>>() {});
            log.info("CLC tree loaded: {} top-level categories", tree.size());
        } catch (Exception e) {
            log.error("failed to load CLC tree", e);
        }
    }

    @Override
    public List<ClcNodeVO> getTree() { return tree; }
}
