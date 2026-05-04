package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.domain.dto.SchoolCreateDTO;
import com.gcrf.library.org.domain.vo.OrgNodeVO;
import com.gcrf.library.org.service.SchoolProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学校 Provisioning 控制器
 *
 * <p>端到端创建学校：创建节点、派生 schema、初始化数据库
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Slf4j
@Tag(name = "组织管理", description = "组织结构和节点管理相关接口")
@RestController
@RequestMapping("/api/v1/org/schools")
@RequiredArgsConstructor
public class SchoolProvisioningController {

    private final SchoolProvisioningService service;

    /**
     * 创建学校并完成 Provisioning
     *
     * <p>端到端操作：
     * <ol>
     *   <li>创建 SCHOOL 节点</li>
     *   <li>派生 schema 名称</li>
     *   <li>创建 PostgreSQL schema</li>
     *   <li>运行 per-school Flyway 迁移</li>
     *   <li>初始化 school_meta</li>
     * </ol>
     */
    @Operation(summary = "创建学校", description = "端到端创建学校并完成数据库初始化")
    @PostMapping
    public Result<OrgNodeVO> createSchool(@Valid @RequestBody SchoolCreateDTO dto) {
        log.info("创建学校: code={}, name={}, parentId={}", dto.getCode(), dto.getName(), dto.getParentId());
        OrgNodeVO result = service.createSchool(dto);
        return Result.success(result);
    }
}
