package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FAQ知识库实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName(value = "faq_knowledge", autoResultMap = true)
public class FaqKnowledge {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 问题
     */
    private String question;

    /**
     * 答案(支持HTML格式)
     */
    private String answer;

    /**
     * 关键词数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keywords;

    /**
     * 意图标签数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> intentTags;

    /**
     * 优先级(越高越优先匹配)
     */
    private Integer priority;

    /**
     * 查看次数
     */
    private Integer viewCount;

    /**
     * 有帮助次数
     */
    private Integer helpfulCount;

    /**
     * 无帮助次数
     */
    private Integer unhelpfulCount;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

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
