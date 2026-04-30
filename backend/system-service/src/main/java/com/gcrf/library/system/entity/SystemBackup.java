package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统备份实体类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Data
@NoArgsConstructor
@TableName("system_backup")
public class SystemBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 备份类型: FULL/INCREMENTAL
     */
    @TableField("backup_type")
    private String backupType;

    /**
     * 状态: PENDING/SUCCESS/FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
