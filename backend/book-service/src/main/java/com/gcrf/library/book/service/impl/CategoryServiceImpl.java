package com.gcrf.library.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.book.dto.request.CategoryCreateRequest;
import com.gcrf.library.book.dto.request.CategoryUpdateRequest;
import com.gcrf.library.book.dto.response.CategoryTreeVO;
import com.gcrf.library.book.dto.response.CategoryVO;
import com.gcrf.library.book.entity.BookCategory;
import com.gcrf.library.book.mapper.BookCategoryMapper;
import com.gcrf.library.book.config.CacheConfig;
import com.gcrf.library.book.service.CategoryService;
import com.gcrf.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final BookCategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public CategoryVO createCategory(CategoryCreateRequest request) {
        log.info("创建分类: categoryName={}, categoryCode={}", request.getCategoryName(), request.getCategoryCode());

        // 验证分类代码唯一性
        if (categoryMapper.existsByCode(request.getCategoryCode(), null)) {
            throw new BusinessException("分类代码已存在: " + request.getCategoryCode());
        }

        // 验证父分类
        Integer level = 1;
        String path = null;
        if (request.getParentId() != null) {
            BookCategory parent = categoryMapper.selectById(request.getParentId());
            if (parent == null || parent.getDeletedAt() != null) {
                throw new BusinessException("父分类不存在");
            }
            if (parent.getLevel() >= 5) {
                throw new BusinessException("分类层级不能超过5级");
            }
            level = parent.getLevel() + 1;
            path = generatePath(parent);
        }

        // 创建分类实体
        BookCategory category = new BookCategory();
        category.setParentId(request.getParentId());
        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setLevel(level);
        category.setPath(path);
        category.setBookCount(0);
        category.setChildCount(0);
        category.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        categoryMapper.insert(category);

        // 更新父分类的子分类计数
        if (request.getParentId() != null) {
            updateChildCount(request.getParentId());
        }

        log.info("分类创建成功: id={}", category.getId());
        return CategoryVO.from(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public CategoryVO updateCategory(CategoryUpdateRequest request) {
        log.info("更新分类: id={}", request.getId());

        // 检查分类是否存在
        BookCategory category = categoryMapper.selectById(request.getId());
        if (category == null || category.getDeletedAt() != null) {
            throw new BusinessException("分类不存在");
        }

        // 更新字段
        if (request.getCategoryName() != null) {
            category.setCategoryName(request.getCategoryName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        categoryMapper.updateById(category);

        log.info("分类更新成功: id={}", category.getId());
        return CategoryVO.from(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConfig.CACHE_CATEGORY_TREE, allEntries = true)
    public void deleteCategory(Long id) {
        log.info("删除分类: id={}", id);

        // 检查分类是否存在
        BookCategory category = categoryMapper.selectById(id);
        if (category == null || category.getDeletedAt() != null) {
            throw new BusinessException("分类不存在");
        }

        // 检查是否有子分类
        int childCount = categoryMapper.countChildren(id);
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }

        // 检查是否有关联图书
        int bookCount = categoryMapper.countBooks(id);
        if (bookCount > 0) {
            throw new BusinessException("该分类下存在图书，无法删除");
        }

        // 软删除
        category.setDeletedAt(LocalDateTime.now());
        categoryMapper.updateById(category);

        // 更新父分类的子分类计数
        if (category.getParentId() != null) {
            updateChildCount(category.getParentId());
        }

        log.info("分类删除成功: id={}", id);
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_CATEGORY_TREE, key = "'tree:' + (#parentId != null ? #parentId : 'root') + ':' + #treeMode")
    public List<CategoryTreeVO> getCategoryTree(Long parentId, boolean treeMode) {
        log.info("获取分类树: parentId={}, treeMode={}", parentId, treeMode);

        // 查询所有有效分类
        LambdaQueryWrapper<BookCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(BookCategory::getDeletedAt)
               .orderByAsc(BookCategory::getSortOrder)
               .orderByAsc(BookCategory::getId);

        if (parentId != null) {
            wrapper.eq(BookCategory::getParentId, parentId);
        }

        List<BookCategory> categories = categoryMapper.selectList(wrapper);

        if (!treeMode) {
            // 返回平铺列表
            return categories.stream()
                    .map(this::convertToTreeVO)
                    .collect(Collectors.toList());
        }

        // 构建树形结构
        return buildTree(categories, parentId);
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        log.info("获取分类详情: id={}", id);

        BookCategory category = categoryMapper.selectById(id);
        if (category == null || category.getDeletedAt() != null) {
            throw new BusinessException("分类不存在");
        }

        return CategoryVO.from(category);
    }

    /**
     * 生成物化路径
     */
    private String generatePath(BookCategory parent) {
        if (parent == null) {
            return null;
        }

        String parentPath = parent.getPath();
        String parentIdStr = String.format("%03d", parent.getId());

        if (parentPath == null || parentPath.isEmpty()) {
            return parentIdStr;
        }

        return parentPath + "." + parentIdStr;
    }

    /**
     * 更新子分类数量
     */
    private void updateChildCount(Long parentId) {
        if (parentId == null) {
            return;
        }

        BookCategory parent = categoryMapper.selectById(parentId);
        if (parent != null) {
            int childCount = categoryMapper.countChildren(parentId);
            parent.setChildCount(childCount);
            categoryMapper.updateById(parent);
        }
    }

    /**
     * 构建树形结构
     */
    private List<CategoryTreeVO> buildTree(List<BookCategory> categories, Long parentId) {
        Map<Long, List<BookCategory>> childrenMap = categories.stream()
                .collect(Collectors.groupingBy(
                        cat -> cat.getParentId() != null ? cat.getParentId() : -1L
                ));

        return categories.stream()
                .filter(cat -> {
                    if (parentId == null) {
                        return cat.getParentId() == null;
                    }
                    return parentId.equals(cat.getParentId());
                })
                .map(cat -> buildTreeNode(cat, childrenMap))
                .collect(Collectors.toList());
    }

    /**
     * 构建树节点
     */
    private CategoryTreeVO buildTreeNode(BookCategory category, Map<Long, List<BookCategory>> childrenMap) {
        CategoryTreeVO node = convertToTreeVO(category);

        List<BookCategory> children = childrenMap.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryTreeVO> childNodes = children.stream()
                    .map(child -> buildTreeNode(child, childrenMap))
                    .collect(Collectors.toList());
            node.setChildren(childNodes);
        } else {
            node.setChildren(new ArrayList<>());
        }

        return node;
    }

    /**
     * 转换为TreeVO
     */
    private CategoryTreeVO convertToTreeVO(BookCategory category) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setCategoryName(category.getCategoryName());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setPath(category.getPath());
        vo.setLevel(category.getLevel());
        vo.setDescription(category.getDescription());
        vo.setIcon(category.getIcon());
        vo.setColor(category.getColor());
        vo.setSortOrder(category.getSortOrder());
        vo.setBookCount(category.getBookCount() != null ? category.getBookCount() : 0);
        vo.setChildCount(category.getChildCount() != null ? category.getChildCount() : 0);
        vo.setStatus(category.getStatus());
        vo.setCreatedAt(category.getCreatedAt());
        vo.setUpdatedAt(category.getUpdatedAt());
        return vo;
    }
}
