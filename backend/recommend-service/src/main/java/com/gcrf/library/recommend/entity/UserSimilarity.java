package com.gcrf.library.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户相似度矩阵实体
 *
 * 预计算并存储用户间的相似度，避免实时计算开销
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@TableName("user_similarity")
public class UserSimilarity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户A的ID
     */
    @TableField("user_id_a")
    private Long userIdA;

    /**
     * 用户B的ID
     */
    @TableField("user_id_b")
    private Long userIdB;

    /**
     * 相似度分数（0-1之间，使用余弦相似度）
     */
    @TableField("similarity_score")
    private Double similarityScore;

    /**
     * 共同借阅的图书数量
     */
    @TableField("common_items_count")
    private Integer commonItemsCount;

    /**
     * 计算时间
     */
    @TableField("calculated_at")
    private LocalDateTime calculatedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
