package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话记录实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName(value = "chat_session", autoResultMap = true)
public class ChatSession {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一ID
     */
    private String sessionId;

    /**
     * 读者ID(可为空-匿名用户)
     */
    private Long readerId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 是否已解决
     */
    private Boolean resolved;

    /**
     * 满意度评分(1-5)
     */
    private Integer satisfactionScore;

    /**
     * 反馈内容
     */
    private String feedback;

    /**
     * 会话上下文(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> context;

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
}
