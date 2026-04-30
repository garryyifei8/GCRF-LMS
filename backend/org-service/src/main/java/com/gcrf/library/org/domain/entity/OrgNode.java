package com.gcrf.library.org.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gcrf.library.org.handler.JsonbTypeHandler;
import com.gcrf.library.org.handler.LtreeTypeHandler;
import com.gcrf.library.org.handler.TimestamptzTypeHandler;
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
     * ltree 字段，通过 LtreeTypeHandler 序列化为 PGobject("ltree") 写入 PostgreSQL
     */
    @TableField(typeHandler = LtreeTypeHandler.class)
    private String path;

    private String tenantSchema;

    private String status;

    /**
     * JSON 元数据，存储为 JSONB，通过 JsonbTypeHandler 序列化为 PGobject("jsonb") 写入 PostgreSQL
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    @TableField(typeHandler = TimestamptzTypeHandler.class)
    private LocalDateTime createdAt;

    @TableField(typeHandler = TimestamptzTypeHandler.class)
    private LocalDateTime updatedAt;
}
