package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.BookCategory;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树形结构响应VO
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
public class CategoryTreeVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类代码
     */
    private String categoryCode;

    /**
     * 物化路径
     */
    private String path;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 图书数量
     */
    private Integer bookCount;

    /**
     * 子分类数量
     */
    private Integer childCount;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 子分类列表
     */
    private List<CategoryTreeVO> children;

    /**
     * 从实体转换
     */
    public static CategoryTreeVO from(BookCategory entity) {
        if (entity == null) {
            return null;
        }
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setPath(entity.getPath());
        vo.setLevel(entity.getLevel());
        vo.setDescription(entity.getDescription());
        vo.setIcon(entity.getIcon());
        vo.setColor(entity.getColor());
        vo.setSortOrder(entity.getSortOrder());
        vo.setBookCount(entity.getBookCount() != null ? entity.getBookCount() : 0);
        vo.setChildCount(entity.getChildCount() != null ? entity.getChildCount() : 0);
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
