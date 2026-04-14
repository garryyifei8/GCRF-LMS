package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.BookCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 图书分类Mapper
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Mapper
public interface BookCategoryMapper extends BaseMapper<BookCategory> {

    /**
     * 检查分类代码是否存在
     *
     * @param categoryCode 分类代码
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 存在返回true
     */
    @Select("SELECT COUNT(*) > 0 FROM book_category " +
            "WHERE category_code = #{categoryCode} " +
            "AND deleted_at IS NULL " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    boolean existsByCode(@Param("categoryCode") String categoryCode,
                        @Param("excludeId") Long excludeId);

    /**
     * 统计子分类数量
     *
     * @param parentId 父分类ID
     * @return 子分类数量
     */
    @Select("SELECT COUNT(*) FROM book_category " +
            "WHERE parent_id = #{parentId} AND deleted_at IS NULL")
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 统计分类下的图书数量
     *
     * @param categoryId 分类ID
     * @return 图书数量
     */
    @Select("SELECT COUNT(*) FROM books " +
            "WHERE category_id = #{categoryId} AND deleted_at IS NULL")
    int countBooks(@Param("categoryId") Long categoryId);
}
