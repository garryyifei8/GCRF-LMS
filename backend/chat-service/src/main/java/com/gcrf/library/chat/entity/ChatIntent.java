package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 意图定义实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName(value = "chat_intent", autoResultMap = true)
public class ChatIntent {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 意图名称
     */
    private String name;

    /**
     * 意图编码
     */
    private String code;

    /**
     * 意图描述
     */
    private String description;

    /**
     * 匹配模式数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> patterns;

    /**
     * 关联实体数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> entities;

    /**
     * 响应模板
     */
    private String responseTemplate;

    /**
     * 动作类型: FAQ_LOOKUP, API_CALL, TRANSFER, NONE
     */
    private String actionType;

    /**
     * 动作参数(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> actionParams;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间(软删除)
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
