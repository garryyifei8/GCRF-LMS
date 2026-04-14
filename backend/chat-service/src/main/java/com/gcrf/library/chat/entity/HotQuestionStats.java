package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热门问题统计实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName("hot_question_stats")
public class HotQuestionStats {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始问题文本
     */
    private String questionText;

    /**
     * 标准化后的问题文本
     */
    private String normalizedText;

    /**
     * 关联的FAQ知识ID
     */
    private Long faqId;

    /**
     * 提问次数
     */
    private Integer askCount;

    /**
     * 最后提问时间
     */
    private LocalDateTime lastAskedAt;

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
