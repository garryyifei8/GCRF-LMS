package com.gcrf.library.system.service;

import com.gcrf.library.system.entity.SystemBackup;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 系统备份服务接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
public interface SystemBackupService {

    /**
     * 创建系统备份
     * 在 dev/test 环境生成占位备份文件并返回记录；
     * 生产环境可替换为真实 pg_dump 逻辑。
     *
     * @return 备份记录
     */
    SystemBackup createBackup();

    /**
     * 查询最近 10 条备份记录
     */
    List<SystemBackup> listBackups();

    /**
     * 下载备份文件
     *
     * @param id 备份记录ID
     * @return Spring Resource
     */
    Resource downloadBackup(Long id);
}
