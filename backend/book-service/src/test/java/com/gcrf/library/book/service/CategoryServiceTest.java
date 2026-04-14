package com.gcrf.library.book.service;

import com.gcrf.library.book.dto.request.CategoryCreateRequest;
import com.gcrf.library.book.dto.request.CategoryUpdateRequest;
import com.gcrf.library.book.dto.response.CategoryTreeVO;
import com.gcrf.library.book.dto.response.CategoryVO;
import com.gcrf.library.book.entity.BookCategory;
import com.gcrf.library.book.mapper.BookCategoryMapper;
import com.gcrf.library.book.service.impl.CategoryServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CategoryService单元测试
 *
 * @author GCRF Team
 * @date 2025-11-06
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("图书分类服务测试")
class CategoryServiceTest {

    @Mock
    private BookCategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private BookCategory testCategory;
    private CategoryCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testCategory = new BookCategory();
        testCategory.setId(1L);
        testCategory.setParentId(null);
        testCategory.setCategoryName("计算机科学");
        testCategory.setCategoryCode("CS");
        testCategory.setLevel(1);
        testCategory.setPath(null);
        testCategory.setBookCount(10);
        testCategory.setChildCount(3);
        testCategory.setStatus("ACTIVE");
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setUpdatedAt(LocalDateTime.now());

        createRequest = new CategoryCreateRequest();
        createRequest.setCategoryName("编程语言");
        createRequest.setCategoryCode("PL");
        createRequest.setParentId(1L);
        createRequest.setDescription("各种编程语言相关图书");
        createRequest.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("创建分类 - 成功")
    void createCategory_Success() {
        // Arrange
        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(false);
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.insert(any(BookCategory.class))).thenReturn(1);

        // Act
        CategoryVO result = categoryService.createCategory(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategoryName()).isEqualTo("编程语言");
        verify(categoryMapper, times(1)).insert(any(BookCategory.class));
    }

    @Test
    @DisplayName("创建分类 - 分类代码已存在")
    void createCategory_CodeExists() {
        // Arrange
        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类代码已存在");

        verify(categoryMapper, never()).insert(any(BookCategory.class));
    }

    @Test
    @DisplayName("创建分类 - 父分类不存在")
    void createCategory_ParentNotFound() {
        // Arrange
        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(false);
        when(categoryMapper.selectById(1L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    @DisplayName("更新分类 - 成功")
    void updateCategory_Success() {
        // Arrange
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setCategoryName("更新后的分类名");
        updateRequest.setDescription("更新后的描述");

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.updateById(any(BookCategory.class))).thenReturn(1);

        // Act
        CategoryVO result = categoryService.updateCategory(updateRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryMapper, times(1)).updateById(any(BookCategory.class));
    }

    @Test
    @DisplayName("更新分类 - 分类不存在")
    void updateCategory_CategoryNotFound() {
        // Arrange
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest();
        updateRequest.setId(999L);

        when(categoryMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("删除分类 - 成功")
    void deleteCategory_Success() {
        // Arrange
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildren(1L)).thenReturn(0);
        when(categoryMapper.countBooks(1L)).thenReturn(0);
        when(categoryMapper.updateById(any(BookCategory.class))).thenReturn(1);

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryMapper, times(1)).updateById(any(BookCategory.class));
    }

    @Test
    @DisplayName("删除分类 - 存在子分类")
    void deleteCategory_HasChildren() {
        // Arrange
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildren(1L)).thenReturn(5);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子分类");
    }

    @Test
    @DisplayName("删除分类 - 存在关联图书")
    void deleteCategory_HasBooks() {
        // Arrange
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.countChildren(1L)).thenReturn(0);
        when(categoryMapper.countBooks(1L)).thenReturn(10);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在图书");
    }

    @Test
    @DisplayName("获取分类树 - 平铺模式")
    void getCategoryTree_FlatMode() {
        // Arrange
        BookCategory cat1 = createCategory(1L, null, "计算机", "CS", 1);
        BookCategory cat2 = createCategory(2L, null, "文学", "LIT", 1);

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(cat1, cat2));

        // Act
        List<CategoryTreeVO> result = categoryService.getCategoryTree(null, false);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("计算机");
        assertThat(result.get(1).getCategoryName()).isEqualTo("文学");
    }

    @Test
    @DisplayName("获取分类树 - 树形模式")
    void getCategoryTree_TreeMode() {
        // Arrange
        BookCategory cat1 = createCategory(1L, null, "计算机", "CS", 1);
        BookCategory cat2 = createCategory(2L, 1L, "编程语言", "PL", 2);

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(cat1, cat2));

        // Act
        List<CategoryTreeVO> result = categoryService.getCategoryTree(null, true);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("计算机");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getCategoryName()).isEqualTo("编程语言");
    }

    @Test
    @DisplayName("根据ID获取分类")
    void getCategoryById_Success() {
        // Arrange
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);

        // Act
        CategoryVO result = categoryService.getCategoryById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("计算机科学");
    }

    @Test
    @DisplayName("根据ID获取分类 - 分类不存在")
    void getCategoryById_NotFound() {
        // Arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("创建分类 - 根分类成功")
    void createCategory_RootCategory_Success() {
        // Arrange
        CategoryCreateRequest rootRequest = new CategoryCreateRequest();
        rootRequest.setCategoryName("新根分类");
        rootRequest.setCategoryCode("ROOT");
        rootRequest.setParentId(null);  // 无父分类
        rootRequest.setStatus("ACTIVE");

        when(categoryMapper.existsByCode(eq("ROOT"), any())).thenReturn(false);
        when(categoryMapper.insert(any(BookCategory.class))).thenReturn(1);

        // Act
        CategoryVO result = categoryService.createCategory(rootRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategoryName()).isEqualTo("新根分类");
        assertThat(result.getLevel()).isEqualTo(1);
        verify(categoryMapper, times(1)).insert(any(BookCategory.class));
        // 不应更新父分类计数（因为是根分类）
        verify(categoryMapper, never()).countChildren(any());
    }

    @Test
    @DisplayName("创建分类 - 父分类层级超限")
    void createCategory_LevelExceeded() {
        // Arrange
        BookCategory level5Parent = createCategory(5L, 4L, "五级分类", "L5", 5);

        createRequest.setParentId(5L);

        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(false);
        when(categoryMapper.selectById(5L)).thenReturn(level5Parent);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类层级不能超过5级");
    }

    @Test
    @DisplayName("创建分类 - 父分类已删除")
    void createCategory_ParentDeleted() {
        // Arrange
        BookCategory deletedParent = createCategory(1L, null, "已删除", "DEL", 1);
        deletedParent.setDeletedAt(LocalDateTime.now());

        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(false);
        when(categoryMapper.selectById(1L)).thenReturn(deletedParent);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    @DisplayName("更新分类 - 已删除的分类")
    void updateCategory_DeletedCategory() {
        // Arrange
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setCategoryName("尝试更新");

        BookCategory deletedCategory = createCategory(1L, null, "已删除", "DEL", 1);
        deletedCategory.setDeletedAt(LocalDateTime.now());

        when(categoryMapper.selectById(1L)).thenReturn(deletedCategory);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("更新分类 - 只更新部分字段")
    void updateCategory_PartialUpdate() {
        // Arrange
        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setIcon("new-icon");  // 只更新图标
        // 其他字段为null

        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.updateById(any(BookCategory.class))).thenReturn(1);

        // Act
        CategoryVO result = categoryService.updateCategory(updateRequest);

        // Assert
        assertThat(result).isNotNull();
        // 原有名称应保持不变
        assertThat(result.getCategoryName()).isEqualTo("计算机科学");
        verify(categoryMapper, times(1)).updateById(any(BookCategory.class));
    }

    @Test
    @DisplayName("删除分类 - 分类不存在")
    void deleteCategory_NotFound() {
        // Arrange
        when(categoryMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("删除分类 - 已删除的分类")
    void deleteCategory_AlreadyDeleted() {
        // Arrange
        BookCategory deletedCategory = createCategory(1L, null, "已删除", "DEL", 1);
        deletedCategory.setDeletedAt(LocalDateTime.now());

        when(categoryMapper.selectById(1L)).thenReturn(deletedCategory);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("删除分类 - 带有父分类的子分类")
    void deleteCategory_WithParent_UpdatesParentChildCount() {
        // Arrange
        BookCategory childCategory = createCategory(2L, 1L, "子分类", "CHILD", 2);
        BookCategory parentCategory = createCategory(1L, null, "父分类", "PARENT", 1);
        parentCategory.setChildCount(3);

        when(categoryMapper.selectById(2L)).thenReturn(childCategory);
        when(categoryMapper.countChildren(2L)).thenReturn(0);
        when(categoryMapper.countBooks(2L)).thenReturn(0);
        when(categoryMapper.updateById(any(BookCategory.class))).thenReturn(1);
        when(categoryMapper.selectById(1L)).thenReturn(parentCategory);
        when(categoryMapper.countChildren(1L)).thenReturn(2);  // 删除后还剩2个

        // Act
        categoryService.deleteCategory(2L);

        // Assert - 应该更新父分类的子分类数
        verify(categoryMapper, times(2)).updateById(any(BookCategory.class));
        verify(categoryMapper, times(1)).countChildren(1L);
    }

    @Test
    @DisplayName("获取分类树 - 多级树结构")
    void getCategoryTree_MultiLevelTree() {
        // Arrange - 三级树结构
        BookCategory root = createCategory(1L, null, "计算机", "CS", 1);
        BookCategory level2 = createCategory(2L, 1L, "编程语言", "PL", 2);
        BookCategory level3 = createCategory(3L, 2L, "Java", "JAVA", 3);

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(root, level2, level3));

        // Act
        List<CategoryTreeVO> result = categoryService.getCategoryTree(null, true);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("计算机");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getCategoryName()).isEqualTo("编程语言");
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getCategoryName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("获取分类树 - 按父分类ID过滤")
    void getCategoryTree_FilterByParentId() {
        // Arrange
        BookCategory child1 = createCategory(2L, 1L, "编程语言", "PL", 2);
        BookCategory child2 = createCategory(3L, 1L, "数据库", "DB", 2);

        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(child1, child2));

        // Act
        List<CategoryTreeVO> result = categoryService.getCategoryTree(1L, false);

        // Assert
        assertThat(result).hasSize(2);
        verify(categoryMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("获取分类树 - 空结果")
    void getCategoryTree_EmptyResult() {
        // Arrange
        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList());

        // Act
        List<CategoryTreeVO> result = categoryService.getCategoryTree(null, true);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("根据ID获取分类 - 已删除的分类")
    void getCategoryById_DeletedCategory() {
        // Arrange
        BookCategory deletedCategory = createCategory(1L, null, "已删除", "DEL", 1);
        deletedCategory.setDeletedAt(LocalDateTime.now());

        when(categoryMapper.selectById(1L)).thenReturn(deletedCategory);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("创建分类 - 更新父分类子分类计数")
    void createCategory_UpdatesParentChildCount() {
        // Arrange
        when(categoryMapper.existsByCode(eq("PL"), any())).thenReturn(false);
        when(categoryMapper.selectById(1L)).thenReturn(testCategory);
        when(categoryMapper.insert(any(BookCategory.class))).thenReturn(1);
        when(categoryMapper.countChildren(1L)).thenReturn(4);  // 新增后变成4
        when(categoryMapper.updateById(any(BookCategory.class))).thenReturn(1);

        // Act
        CategoryVO result = categoryService.createCategory(createRequest);

        // Assert
        assertThat(result).isNotNull();
        // 验证更新了父分类的子分类计数
        verify(categoryMapper, times(1)).countChildren(1L);
        verify(categoryMapper, times(1)).updateById(any(BookCategory.class));
    }

    // ==================== 辅助方法 ====================

    private BookCategory createCategory(Long id, Long parentId, String name, String code, int level) {
        BookCategory category = new BookCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setCategoryName(name);
        category.setCategoryCode(code);
        category.setLevel(level);
        category.setBookCount(0);
        category.setChildCount(0);
        category.setStatus("ACTIVE");
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}
