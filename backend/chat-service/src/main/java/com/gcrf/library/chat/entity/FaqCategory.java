package com.gcrf.library.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FAQ分类实体
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@TableName("faq_category")
public class FaqCategory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类编码
     */
    private String code;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

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
