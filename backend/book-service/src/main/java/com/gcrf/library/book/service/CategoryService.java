package com.gcrf.library.book.service;

import com.gcrf.library.book.dto.request.CategoryCreateRequest;
import com.gcrf.library.book.dto.request.CategoryUpdateRequest;
import com.gcrf.library.book.dto.response.CategoryTreeVO;
import com.gcrf.library.book.dto.response.CategoryVO;
import java.util.List;

/**
 * 分类服务接口
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
public interface CategoryService {

    /**
     * 创建分类
     *
     * @param request 创建请求
     * @return 分类VO
     */
    CategoryVO createCategory(CategoryCreateRequest request);

    /**
     * 更新分类
     *
     * @param request 更新请求
     * @return 分类VO
     */
    CategoryVO updateCategory(CategoryUpdateRequest request);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 获取分类树（支持按parentId过滤）
     *
     * @param parentId 父分类ID（null表示所有）
     * @param treeMode 是否返回树形结构（false返回平铺列表）
     * @return 分类列表或树形结构
     */
    List<CategoryTreeVO> getCategoryTree(Long parentId, boolean treeMode);

    /**
     * 根据ID获取分类
     *
     * @param id 分类ID
     * @return 分类VO
     */
    CategoryVO getCategoryById(Long id);
}
