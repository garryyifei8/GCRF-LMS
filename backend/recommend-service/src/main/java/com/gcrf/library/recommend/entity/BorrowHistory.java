package com.gcrf.library.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅历史实体 - 用于推荐算法计算
 *
 * 该表从circulation_records同步或视图映射
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@TableName("borrow_history")
public class BorrowHistory {

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
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 图书标题（冗余字段，方便查询）
     */
    @TableField("book_title")
    private String bookTitle;

    /**
     * 图书分类代码
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 借阅时间
     */
    @TableField("borrow_time")
    private LocalDateTime borrowTime;

    /**
     * 归还时间
     */
    @TableField("return_time")
    private LocalDateTime returnTime;

    /**
     * 借阅天数
     */
    @TableField("borrow_days")
    private Integer borrowDays;

    /**
     * 评分（1-5，可选）
     */
    @TableField("rating")
    private Integer rating;

    /**
     * 隐式评分（基于借阅行为计算）
     * 计算规则：完成借阅=3分，续借=4分，提前归还=2分，逾期=1分
     */
    @TableField("implicit_rating")
    private Double implicitRating;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
