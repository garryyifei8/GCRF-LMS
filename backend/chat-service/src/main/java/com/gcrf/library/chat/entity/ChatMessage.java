package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话消息实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName(value = "chat_message", autoResultMap = true)
public class ChatMessage {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色: user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 识别的意图编码
     */
    private String intentCode;

    /**
     * 意图置信度(0-1)
     */
    private BigDecimal confidence;

    /**
     * 匹配的FAQ知识ID
     */
    private Long matchedFaqId;

    /**
     * 元数据(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
