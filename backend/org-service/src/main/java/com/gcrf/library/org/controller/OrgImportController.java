package com.gcrf.library.org.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.org.service.OrgImportService;
import com.gcrf.library.org.service.OrgImportService.ImportReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 批量导入控制器
 *
 * <p>Shares the {@code /api/v1/org/nodes} base path with {@link OrgNodeController};
 * the {@code /import} sub-path is unique so no routing conflict occurs.
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Slf4j
@Tag(name = "组织管理", description = "组织结构和节点管理相关接口")
@RestController
@RequestMapping("/api/v1/org/nodes")
@RequiredArgsConstructor
public class OrgImportController {

    private final OrgImportService importService;

    /**
     * 批量导入组织节点
     *
     * <p>Accepts an .xlsx file via {@code multipart/form-data}. Each row must contain
     * columns [类型, 上级 code, code, 名称]. Rows are processed top-to-bottom so parent
     * rows must appear before their children.
     *
     * @param file 上传的 Excel 文件（.xlsx）
     * @return 导入报告（成功数、失败数及错误明细）
     */
    @Operation(summary = "批量导入组织节点",
               description = "上传 .xlsx 文件批量创建组织节点（列: 类型, 上级 code, code, 名称）")
    @PostMapping("/import")
    public Result<ImportReport> importExcel(@RequestParam("file") MultipartFile file) {
        log.info("Excel import request received: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportReport report = importService.importExcel(file);
        log.info("Excel import finished: created={}, failed={}", report.getCreated(), report.getFailed());
        return Result.success(report);
    }
}
