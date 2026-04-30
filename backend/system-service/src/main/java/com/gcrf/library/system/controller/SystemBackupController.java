package com.gcrf.library.system.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.entity.SystemBackup;
import com.gcrf.library.system.service.SystemBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统备份控制器
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/backup")
@RequiredArgsConstructor
@Tag(name = "系统备份", description = "数据库备份创建与下载")
public class SystemBackupController {

    private final SystemBackupService backupService;

    /**
     * 触发新备份
     */
    @PostMapping
    @Operation(summary = "创建备份", description = "触发系统数据库备份并返回备份记录")
    public Result<SystemBackup> create() {
        log.info("触发系统备份");
        return Result.success(backupService.createBackup());
    }

    /**
     * 查询最近备份列表
     */
    @GetMapping
    @Operation(summary = "备份列表", description = "返回最近 10 条备份记录")
    public Result<List<SystemBackup>> list() {
        return Result.success(backupService.listBackups());
    }

    /**
     * 下载指定备份文件
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载备份", description = "以文件流方式下载指定备份")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        log.info("下载备份文件, id: {}", id);
        Resource resource = backupService.downloadBackup(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
