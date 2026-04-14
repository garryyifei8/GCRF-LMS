package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.InventoryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 盘点任务Mapper
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Mapper
public interface InventoryTaskMapper extends BaseMapper<InventoryTask> {

    /**
     * 根据任务编号查询
     *
     * @param taskCode 任务编号
     * @return 任务
     */
    @Select("SELECT * FROM inventory_task WHERE task_code = #{taskCode} AND deleted = 0")
    InventoryTask selectByTaskCode(@Param("taskCode") String taskCode);

    /**
     * 更新任务统计信息
     *
     * @param taskId          任务ID
     * @param checkedBooks    已盘点数
     * @param discrepancyCount 差异数量
     */
    @Update("""
            UPDATE inventory_task
            SET checked_books = #{checkedBooks},
                discrepancy_count = #{discrepancyCount},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId}
              AND deleted = 0
            """)
    void updateTaskStatistics(@Param("taskId") Long taskId,
                              @Param("checkedBooks") Integer checkedBooks,
                              @Param("discrepancyCount") Integer discrepancyCount);

    /**
     * 增加已盘点数
     *
     * @param taskId 任务ID
     * @param increment 增量
     */
    @Update("""
            UPDATE inventory_task
            SET checked_books = COALESCE(checked_books, 0) + #{increment},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId}
              AND deleted = 0
            """)
    void incrementCheckedBooks(@Param("taskId") Long taskId, @Param("increment") int increment);

    /**
     * 增加差异数量
     *
     * @param taskId 任务ID
     * @param increment 增量
     */
    @Update("""
            UPDATE inventory_task
            SET discrepancy_count = COALESCE(discrepancy_count, 0) + #{increment},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{taskId}
              AND deleted = 0
            """)
    void incrementDiscrepancyCount(@Param("taskId") Long taskId, @Param("increment") int increment);

    /**
     * 统计指定状态的任务数量
     *
     * @param status 状态
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM inventory_task WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);
}
