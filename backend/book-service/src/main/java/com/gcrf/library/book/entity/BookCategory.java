package com.gcrf.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 图书分类实体
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
@TableName("book_category")
public class BookCategory {

    /**
     * 分类ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父分类ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类代码
     */
    @NotBlank(message = "分类代码不能为空")
    @Size(max = 50, message = "分类代码长度不能超过50")
    @TableField("category_code")
    private String categoryCode;

    /**
     * 物化路径 (e.g., "001.002.003")
     */
    @TableField("path")
    private String path;

    /**
     * 层级 (1-5)
     */
    @Min(value = 1, message = "层级必须大于0")
    @Max(value = 5, message = "层级不能超过5")
    @TableField("level")
    private Integer level;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 颜色
     */
    @TableField("color")
    private String color;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 图书数量（冗余字段）
     */
    @TableField("book_count")
    private Integer bookCount;

    /**
     * 子分类数量（冗余字段）
     */
    @TableField("child_count")
    private Integer childCount;

    /**
     * 状态：ACTIVE-正常 INACTIVE-停用
     */
    @TableField("status")
    private String status;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间（逻辑删除）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
