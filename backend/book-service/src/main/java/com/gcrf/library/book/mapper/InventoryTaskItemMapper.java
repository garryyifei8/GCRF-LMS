package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.InventoryTaskItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 盘点明细Mapper
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Mapper
public interface InventoryTaskItemMapper extends BaseMapper<InventoryTaskItem> {

    /**
     * 根据任务ID查询明细列表
     *
     * @param taskId 任务ID
     * @return 明细列表
     */
    @Select("SELECT * FROM inventory_task_item WHERE task_id = #{taskId} ORDER BY id")
    List<InventoryTaskItem> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据任务ID分页查询明细
     *
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 明细列表
     */
    @Select("""
            SELECT * FROM inventory_task_item
            WHERE task_id = #{taskId}
            ORDER BY id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<InventoryTaskItem> selectByTaskIdPage(@Param("taskId") Long taskId,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    /**
     * 统计任务明细数量
     *
     * @param taskId 任务ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM inventory_task_item WHERE task_id = #{taskId}")
    long countByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据任务ID和图书ID查询明细
     *
     * @param taskId 任务ID
     * @param bookId 图书ID
     * @return 明细
     */
    @Select("SELECT * FROM inventory_task_item WHERE task_id = #{taskId} AND book_id = #{bookId}")
    InventoryTaskItem selectByTaskIdAndBookId(@Param("taskId") Long taskId, @Param("bookId") Long bookId);

    /**
     * 统计指定状态的明细数量
     *
     * @param taskId 任务ID
     * @param status 状态
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM inventory_task_item WHERE task_id = #{taskId} AND status = #{status}")
    long countByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") String status);

    /**
     * 统计有差异的明细数量
     *
     * @param taskId 任务ID
     * @return 数量
     */
    @Select("""
            SELECT COUNT(*) FROM inventory_task_item
            WHERE task_id = #{taskId}
              AND status = 'CHECKED'
              AND discrepancy != 0
            """)
    long countDiscrepancyByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询有差异的明细
     *
     * @param taskId 任务ID
     * @return 有差异的明细列表
     */
    @Select("""
            SELECT * FROM inventory_task_item
            WHERE task_id = #{taskId}
              AND status = 'CHECKED'
              AND discrepancy != 0
            ORDER BY ABS(discrepancy) DESC
            """)
    List<InventoryTaskItem> selectDiscrepancyItems(@Param("taskId") Long taskId);

    /**
     * 批量插入明细
     *
     * @param items 明细列表
     */
    @Select("""
            <script>
            INSERT INTO inventory_task_item
                (task_id, book_id, book_title, isbn, expected_quantity, status, created_at, updated_at)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.taskId}, #{item.bookId}, #{item.bookTitle}, #{item.isbn},
                 #{item.expectedQuantity}, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            </foreach>
            </script>
            """)
    void batchInsert(@Param("items") List<InventoryTaskItem> items);
}
