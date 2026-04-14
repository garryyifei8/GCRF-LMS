package com.gcrf.library.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.recommend.entity.BorrowHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 借阅历史Mapper接口
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Mapper
public interface BorrowHistoryMapper extends BaseMapper<BorrowHistory> {

    /**
     * 获取读者借阅过的所有图书ID
     */
    @Select("SELECT DISTINCT book_id FROM borrow_history WHERE reader_id = #{readerId}")
    List<Long> findBookIdsByReaderId(@Param("readerId") Long readerId);

    /**
     * 获取借阅过某本图书的所有读者ID
     */
    @Select("SELECT DISTINCT reader_id FROM borrow_history WHERE book_id = #{bookId}")
    List<Long> findReaderIdsByBookId(@Param("bookId") Long bookId);

    /**
     * 获取读者的借阅评分向量（book_id -> rating）
     */
    @Select("SELECT book_id, COALESCE(implicit_rating, 3.0) as rating FROM borrow_history WHERE reader_id = #{readerId}")
    List<Map<String, Object>> findRatingVectorByReaderId(@Param("readerId") Long readerId);

    /**
     * 获取图书的被借阅评分向量（reader_id -> rating）
     */
    @Select("SELECT reader_id, COALESCE(implicit_rating, 3.0) as rating FROM borrow_history WHERE book_id = #{bookId}")
    List<Map<String, Object>> findRatingVectorByBookId(@Param("bookId") Long bookId);

    /**
     * 获取时间窗口内的热门图书（按借阅次数排序）
     */
    @Select("""
        SELECT book_id, COUNT(*) as borrow_count
        FROM borrow_history
        WHERE borrow_time >= #{startTime}
        GROUP BY book_id
        ORDER BY borrow_count DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findPopularBooks(
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit
    );

    /**
     * 获取两个读者共同借阅的图书数量
     */
    @Select("""
        SELECT COUNT(DISTINCT a.book_id)
        FROM borrow_history a
        INNER JOIN borrow_history b ON a.book_id = b.book_id
        WHERE a.reader_id = #{userIdA} AND b.reader_id = #{userIdB}
        """)
    Integer countCommonBooks(
            @Param("userIdA") Long userIdA,
            @Param("userIdB") Long userIdB
    );

    /**
     * 获取两本书共同被借阅的用户数量
     */
    @Select("""
        SELECT COUNT(DISTINCT a.reader_id)
        FROM borrow_history a
        INNER JOIN borrow_history b ON a.reader_id = b.reader_id
        WHERE a.book_id = #{bookIdA} AND b.book_id = #{bookIdB}
        """)
    Integer countCommonUsers(
            @Param("bookIdA") Long bookIdA,
            @Param("bookIdB") Long bookIdB
    );

    /**
     * 获取所有有借阅记录的读者ID
     */
    @Select("SELECT DISTINCT reader_id FROM borrow_history")
    List<Long> findAllReaderIds();

    /**
     * 获取所有被借阅过的图书ID
     */
    @Select("SELECT DISTINCT book_id FROM borrow_history")
    List<Long> findAllBookIds();

    /**
     * 获取读者借阅的图书分类统计
     */
    @Select("""
        SELECT category_code, COUNT(*) as count
        FROM borrow_history
        WHERE reader_id = #{readerId} AND category_code IS NOT NULL
        GROUP BY category_code
        ORDER BY count DESC
        """)
    List<Map<String, Object>> findCategoryPreference(@Param("readerId") Long readerId);
}
