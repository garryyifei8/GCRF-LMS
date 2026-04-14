package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 图书Mapper
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * PostgreSQL全文搜索 - 使用tsvector和ts_rank
     * 支持中文分词（需要配置pg_jieba或zhparser扩展）
     *
     * @param query 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 匹配的图书列表
     */
    @Select("""
            SELECT b.*,
                   ts_rank(b.search_vector, plainto_tsquery('simple', #{query})) AS relevance_score
            FROM books b
            WHERE b.deleted_at IS NULL
              AND b.status = 'ACTIVE'
              AND (
                  b.search_vector @@ plainto_tsquery('simple', #{query})
                  OR b.title ILIKE '%' || #{query} || '%'
                  OR b.author ILIKE '%' || #{query} || '%'
                  OR b.isbn ILIKE '%' || #{query} || '%'
              )
            ORDER BY relevance_score DESC, b.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Book> fullTextSearch(@Param("query") String query,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 全文搜索结果计数
     *
     * @param query 搜索关键词
     * @return 匹配的记录数
     */
    @Select("""
            SELECT COUNT(*)
            FROM books b
            WHERE b.deleted_at IS NULL
              AND b.status = 'ACTIVE'
              AND (
                  b.search_vector @@ plainto_tsquery('simple', #{query})
                  OR b.title ILIKE '%' || #{query} || '%'
                  OR b.author ILIKE '%' || #{query} || '%'
                  OR b.isbn ILIKE '%' || #{query} || '%'
              )
            """)
    long fullTextSearchCount(@Param("query") String query);

    /**
     * 带筛选条件的全文搜索
     *
     * @param query 搜索关键词
     * @param categoryCode 分类代码（可为null）
     * @param publisher 出版社（可为null）
     * @param language 语言（可为null）
     * @param availableOnly 仅显示有库存
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 匹配的图书列表
     */
    @Select("""
            <script>
            SELECT b.*,
                   ts_rank(b.search_vector, plainto_tsquery('simple', #{query})) AS relevance_score
            FROM books b
            WHERE b.deleted_at IS NULL
              AND b.status = 'ACTIVE'
              AND (
                  b.search_vector @@ plainto_tsquery('simple', #{query})
                  OR b.title ILIKE '%' || #{query} || '%'
                  OR b.author ILIKE '%' || #{query} || '%'
                  OR b.isbn ILIKE '%' || #{query} || '%'
              )
              <if test="categoryCode != null and categoryCode != ''">
                AND b.classification_code = #{categoryCode}
              </if>
              <if test="publisher != null and publisher != ''">
                AND b.publisher ILIKE '%' || #{publisher} || '%'
              </if>
              <if test="language != null and language != ''">
                AND b.language = #{language}
              </if>
              <if test="availableOnly == true">
                AND b.available_quantity > 0
              </if>
            ORDER BY relevance_score DESC, b.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Book> fullTextSearchWithFilters(@Param("query") String query,
                                         @Param("categoryCode") String categoryCode,
                                         @Param("publisher") String publisher,
                                         @Param("language") String language,
                                         @Param("availableOnly") boolean availableOnly,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 带筛选条件的全文搜索计数
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM books b
            WHERE b.deleted_at IS NULL
              AND b.status = 'ACTIVE'
              AND (
                  b.search_vector @@ plainto_tsquery('simple', #{query})
                  OR b.title ILIKE '%' || #{query} || '%'
                  OR b.author ILIKE '%' || #{query} || '%'
                  OR b.isbn ILIKE '%' || #{query} || '%'
              )
              <if test="categoryCode != null and categoryCode != ''">
                AND b.classification_code = #{categoryCode}
              </if>
              <if test="publisher != null and publisher != ''">
                AND b.publisher ILIKE '%' || #{publisher} || '%'
              </if>
              <if test="language != null and language != ''">
                AND b.language = #{language}
              </if>
              <if test="availableOnly == true">
                AND b.available_quantity > 0
              </if>
            </script>
            """)
    long fullTextSearchCountWithFilters(@Param("query") String query,
                                        @Param("categoryCode") String categoryCode,
                                        @Param("publisher") String publisher,
                                        @Param("language") String language,
                                        @Param("availableOnly") boolean availableOnly);

    /**
     * 根据条码查询图书
     */
    @Select("SELECT * FROM books WHERE barcode = #{barcode} AND deleted_at IS NULL")
    Book findByBarcode(@Param("barcode") String barcode);

    /**
     * 获取下一个条码序号
     */
    @Select("SELECT nextval('book_barcode_seq')")
    Long getNextBarcodeSequence();
}
