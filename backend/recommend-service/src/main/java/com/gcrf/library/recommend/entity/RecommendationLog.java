package com.gcrf.library.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐日志实体
 *
 * 记录推荐结果和用户反馈，用于评估推荐效果
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@TableName("recommendation_log")
public class RecommendationLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 读者ID
     */
    @TableField("reader_id")
    private Long readerId;

    /**
     * 推荐的图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 推荐分数
     */
    @TableField("score")
    private Double score;

    /**
     * 推荐算法类型
     * USER_CF: 用户协同过滤
     * ITEM_CF: 物品协同过滤
     * POPULAR: 热门推荐
     * HYBRID: 混合推荐
     * CONTENT: 内容推荐
     */
    @TableField("algorithm")
    private String algorithm;

    /**
     * 推荐场景
     * HOMEPAGE: 首页推荐
     * DETAIL: 详情页推荐
     * SEARCH: 搜索推荐
     * TOPIC: 主题推荐
     */
    @TableField("scene")
    private String scene;

    /**
     * 推荐理由
     */
    @TableField("reason")
    private String reason;

    /**
     * 是否被点击
     */
    @TableField("clicked")
    private Boolean clicked;

    /**
     * 点击时间
     */
    @TableField("clicked_at")
    private LocalDateTime clickedAt;

    /**
     * 是否被借阅
     */
    @TableField("borrowed")
    private Boolean borrowed;

    /**
     * 借阅时间
     */
    @TableField("borrowed_at")
    private LocalDateTime borrowedAt;

    /**
     * 推荐时间
     */
    @TableField("recommended_at")
    private LocalDateTime recommendedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
