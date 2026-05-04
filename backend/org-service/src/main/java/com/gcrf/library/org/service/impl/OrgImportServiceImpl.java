package com.gcrf.library.org.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.org.domain.dto.OrgImportRow;
import com.gcrf.library.org.domain.dto.OrgNodeCreateDTO;
import com.gcrf.library.org.domain.entity.OrgNode;
import com.gcrf.library.org.mapper.OrgNodeMapper;
import com.gcrf.library.org.service.OrgImportService;
import com.gcrf.library.org.service.OrgNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入服务实现
 *
 * <p>Reads all rows first, then processes them in order so that parent nodes created
 * earlier in the same batch are visible to subsequent rows (within the same transaction).
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgImportServiceImpl implements OrgImportService {

    private final OrgNodeService orgService;
    private final OrgNodeMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportReport importExcel(MultipartFile file) {
        List<OrgImportRow> rows = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), OrgImportRow.class, new ReadListener<OrgImportRow>() {
                @Override
                public void invoke(OrgImportRow row, AnalysisContext ctx) {
                    rows.add(row);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext ctx) {
                    // no-op
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException("read excel failed", e);
        }

        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (OrgImportRow row : rows) {
            try {
                Long parentId = resolveParentId(row.getParentCode());

                OrgNodeCreateDTO dto = new OrgNodeCreateDTO();
                dto.setParentId(parentId);
                dto.setType(row.getType());
                dto.setName(row.getName());
                dto.setCode(row.getCode());

                orgService.create(dto);
                created++;
                log.debug("imported row: code={}, type={}", row.getCode(), row.getType());
            } catch (Exception e) {
                failed++;
                errors.add(row.getCode() + ": " + e.getMessage());
                log.warn("import row failed: code={}, type={}, error={}", row.getCode(), row.getType(), e.getMessage());
            }
        }

        log.info("Excel import completed: created={}, failed={}", created, failed);
        return ImportReport.builder()
            .created(created)
            .failed(failed)
            .errors(errors)
            .build();
    }

    /**
     * Looks up the parent node ID by code. Returns {@code null} for root nodes
     * (when parentCode is null or blank).
     *
     * @throws IllegalArgumentException if parentCode is non-blank but no matching node exists
     */
    private Long resolveParentId(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        OrgNode parent = mapper.selectOne(
            new LambdaQueryWrapper<OrgNode>().eq(OrgNode::getCode, parentCode));
        if (parent == null) {
            throw new IllegalArgumentException("parent code not found: " + parentCode);
        }
        return parent.getId();
    }
}
