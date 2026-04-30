package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈实体类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Data
@NoArgsConstructor
@TableName("system_feedback")
public class SystemFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 内容
     */
    @TableField("content")
    private String content;

    /**
     * 反馈类型: BUG/SUGGESTION/QUESTION/OTHER
     */
    @TableField("feedback_type")
    private String feedbackType;

    /**
     * 状态: PENDING/HANDLED/CLOSED
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /**
     * 处理人ID
     */
    @TableField("handled_by")
    private Long handledBy;

    /**
     * 处理回复
     */
    @TableField("response")
    private String response;
}
