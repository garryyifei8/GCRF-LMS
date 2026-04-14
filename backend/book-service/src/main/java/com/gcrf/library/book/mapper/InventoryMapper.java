package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存Mapper
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 根据图书ID查询库存
     *
     * @param bookId 图书ID
     * @return 库存记录
     */
    @Select("SELECT * FROM inventory WHERE book_id = #{bookId} AND deleted = 0")
    Inventory selectByBookId(@Param("bookId") Long bookId);

    /**
     * 查询需要预警的库存
     *
     * @return 预警库存列表
     */
    @Select("""
            SELECT i.* FROM inventory i
            WHERE i.deleted = 0
              AND i.available_quantity <= i.alert_threshold
            ORDER BY i.available_quantity ASC
            """)
    List<Inventory> selectAlertInventories();

    /**
     * 查询需要预警的库存（分页）
     *
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 预警库存列表
     */
    @Select("""
            SELECT i.* FROM inventory i
            WHERE i.deleted = 0
              AND i.available_quantity <= i.alert_threshold
            ORDER BY i.available_quantity ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Inventory> selectAlertInventoriesPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计需要预警的库存数量
     *
     * @return 预警数量
     */
    @Select("""
            SELECT COUNT(*) FROM inventory i
            WHERE i.deleted = 0
              AND i.available_quantity <= i.alert_threshold
            """)
    long countAlertInventories();

    /**
     * 按位置查询库存
     *
     * @param location 位置
     * @return 库存列表
     */
    @Select("""
            SELECT * FROM inventory
            WHERE deleted = 0
              AND location = #{location}
            ORDER BY shelf_number
            """)
    List<Inventory> selectByLocation(@Param("location") String location);

    /**
     * 批量更新最后盘点时间
     *
     * @param bookIds 图书ID列表
     */
    @Select("""
            <script>
            UPDATE inventory
            SET last_check_time = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE book_id IN
            <foreach collection="bookIds" item="bookId" open="(" separator="," close=")">
                #{bookId}
            </foreach>
              AND deleted = 0
            </script>
            """)
    void batchUpdateLastCheckTime(@Param("bookIds") List<Long> bookIds);
}
