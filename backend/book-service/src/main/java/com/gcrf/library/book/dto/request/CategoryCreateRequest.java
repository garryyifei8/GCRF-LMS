package com.gcrf.library.book.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 分类创建请求
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
public class CategoryCreateRequest {

    /**
     * 父分类ID（根分类为null）
     */
    private Long parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String categoryName;

    /**
     * 分类代码
     */
    @NotBlank(message = "分类代码不能为空")
    @Size(max = 50, message = "分类代码长度不能超过50")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "分类代码只能包含大写字母、数字和下划线")
    private String categoryCode;

    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    /**
     * 图标
     */
    @Size(max = 100, message = "图标长度不能超过100")
    private String icon;

    /**
     * 颜色
     */
    @Size(max = 20, message = "颜色长度不能超过20")
    private String color;

    /**
     * 排序顺序
     */
    @Min(value = 0, message = "排序顺序不能为负数")
    private Integer sortOrder;

    /**
     * 状态
     */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "状态必须是ACTIVE或INACTIVE")
    private String status = "ACTIVE";
}
