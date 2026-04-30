package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.system.entity.SystemBackup;
import com.gcrf.library.system.mapper.SystemBackupMapper;
import com.gcrf.library.system.service.SystemBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 系统备份服务实现类
 *
 * 在 dev/test 环境中，生成包含时间戳和 schema 列表的占位文件作为备份。
 * 生产环境可替换 doBackup() 为真实 pg_dump 调用。
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemBackupServiceImpl implements SystemBackupService {

    private static final String BACKUP_DIR = "/tmp/gcrf-backups";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SystemBackupMapper backupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemBackup createBackup() {
        // 创建备份目录
        Path backupPath = Paths.get(BACKUP_DIR);
        try {
            Files.createDirectories(backupPath);
        } catch (IOException e) {
            throw new BusinessException("创建备份目录失败: " + e.getMessage());
        }

        String timestamp = LocalDateTime.now().format(FILE_TS);
        String fileName = "gcrf_backup_" + timestamp + ".sql";
        Path filePath = backupPath.resolve(fileName);

        // 创建占位备份文件（dev/test 环境）
        SystemBackup backup = new SystemBackup();
        backup.setFileName(fileName);
        backup.setFilePath(filePath.toString());
        backup.setBackupType("FULL");
        backup.setStatus("PENDING");
        backupMapper.insert(backup);

        try {
            String content = buildPlaceholderContent(timestamp);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

            long fileSize = Files.size(filePath);
            backup.setFileSize(fileSize);
            backup.setStatus("SUCCESS");
            backup.setCompletedAt(LocalDateTime.now());
            backupMapper.updateById(backup);

            log.info("系统备份创建成功, fileName: {}, size: {} bytes", fileName, fileSize);
        } catch (IOException e) {
            backup.setStatus("FAILED");
            backupMapper.updateById(backup);
            log.error("系统备份创建失败, fileName: {}", fileName, e);
            throw new BusinessException("备份文件写入失败: " + e.getMessage());
        }

        return backup;
    }

    @Override
    public List<SystemBackup> listBackups() {
        Page<SystemBackup> page = new Page<>(1, 10);
        Page<SystemBackup> result = backupMapper.selectPage(page,
                new LambdaQueryWrapper<SystemBackup>()
                        .orderByDesc(SystemBackup::getCreatedAt));
        return result.getRecords();
    }

    @Override
    public Resource downloadBackup(Long id) {
        SystemBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw new BusinessException("备份记录不存在, id: " + id);
        }
        File file = new File(backup.getFilePath());
        if (!file.exists()) {
            throw new BusinessException("备份文件不存在: " + backup.getFilePath());
        }
        return new FileSystemResource(file);
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private String buildPlaceholderContent(String timestamp) {
        return "-- GCRF Library Management System Backup\n" +
               "-- Generated at: " + timestamp + "\n" +
               "-- Environment: dev/test (placeholder)\n" +
               "--\n" +
               "-- Schema list:\n" +
               "--   system_service\n" +
               "--   book_service\n" +
               "--   reader_service\n" +
               "--   circulation_service\n" +
               "--   auth_service\n" +
               "--\n" +
               "-- NOTE: Replace this file with real pg_dump output in production.\n";
    }
}
