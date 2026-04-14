package com.gcrf.library.book.controller;

import com.gcrf.library.book.dto.request.CategoryCreateRequest;
import com.gcrf.library.book.dto.request.CategoryUpdateRequest;
import com.gcrf.library.book.dto.response.CategoryTreeVO;
import com.gcrf.library.book.dto.response.CategoryVO;
import com.gcrf.library.book.service.CategoryService;
import com.gcrf.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/books/categories")
@RequiredArgsConstructor
@Tag(name = "图书分类管理", description = "图书分类的增删改查接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类树
     */
    @GetMapping
    @Operation(summary = "获取分类树", description = "获取图书分类树形结构或列表")
    public Result<List<CategoryTreeVO>> getCategories(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "true") boolean treeMode) {
        log.info("获取分类树: parentId={}, treeMode={}", parentId, treeMode);
        List<CategoryTreeVO> categories = categoryService.getCategoryTree(parentId, treeMode);
        return Result.success(categories);
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据分类ID查询详细信息")
    public Result<CategoryVO> getCategory(@PathVariable Long id) {
        log.info("获取分类详情: id={}", id);
        CategoryVO category = categoryService.getCategoryById(id);
        return Result.success(category);
    }

    /**
     * 创建分类
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "新增图书分类")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        log.info("创建分类: categoryName={}", request.getCategoryName());
        CategoryVO result = categoryService.createCategory(request);
        return Result.success(result);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "修改图书分类信息")
    public Result<CategoryVO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        log.info("更新分类: id={}", id);
        // 确保路径参数和请求体的ID一致
        request.setId(id);
        CategoryVO result = categoryService.updateCategory(request);
        return Result.success(result);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除图书分类（逻辑删除）")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        log.info("删除分类: id={}", id);
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
