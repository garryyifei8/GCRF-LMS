package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Data
@NoArgsConstructor
@TableName("system_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置键（主键）
     */
    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;

    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 配置类型: STRING/NUMBER/BOOLEAN/JSON
     */
    @TableField("config_type")
    private String configType;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private Long updatedBy;
}
