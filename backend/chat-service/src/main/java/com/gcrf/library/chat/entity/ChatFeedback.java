package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话反馈实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName("chat_feedback")
public class ChatFeedback {

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
     * 消息ID
     */
    private Long messageId;

    /**
     * FAQ知识ID
     */
    private Long faqId;

    /**
     * 反馈类型: helpful, unhelpful, report
     */
    private String feedbackType;

    /**
     * 反馈评论
     */
    private String comment;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
