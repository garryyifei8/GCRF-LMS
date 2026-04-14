package com.gcrf.library.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品（图书）相似度矩阵实体
 *
 * 预计算并存储图书间的相似度，用于Item-based CF算法
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@TableName("item_similarity")
public class ItemSimilarity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图书A的ID
     */
    @TableField("book_id_a")
    private Long bookIdA;

    /**
     * 图书B的ID
     */
    @TableField("book_id_b")
    private Long bookIdB;

    /**
     * 相似度分数（0-1之间，使用余弦相似度）
     */
    @TableField("similarity_score")
    private Double similarityScore;

    /**
     * 共同借阅用户数量
     */
    @TableField("common_users_count")
    private Integer commonUsersCount;

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
