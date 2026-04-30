package com.gcrf.library.org.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织节点实体
 *
 * @author Claude Code
 * @date 2025-10-30
 */
@Data
@TableName(value = "org_node", autoResultMap = true)
public class OrgNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String type;

    private String name;

    private String code;

    /**
     * ltree 字段，PostgreSQL 自动转换为 String
     */
    private String path;

    private String tenantSchema;

    private String status;

    /**
     * JSON 元数据，存储为 JSONB，Java 端以字符串形式处理
     */
    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
