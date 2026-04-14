package com.gcrf.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcrf.library.book.entity.enums.TaskItemStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点明细实体
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@TableName("inventory_task_item")
public class InventoryTaskItem {

    /**
     * 明细ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 图书标题
     */
    @TableField("book_title")
    private String bookTitle;

    /**
     * ISBN号
     */
    private String isbn;

    /**
     * 期望数量
     */
    @TableField("expected_quantity")
    private Integer expectedQuantity;

    /**
     * 实际数量
     */
    @TableField("actual_quantity")
    private Integer actualQuantity;

    /**
     * 差异数量
     */
    private Integer discrepancy;

    /**
     * 状态
     */
    private TaskItemStatus status;

    /**
     * 盘点时间
     */
    @TableField("checked_time")
    private LocalDateTime checkedTime;

    /**
     * 盘点人ID
     */
    @TableField("checker_id")
    private Long checkerId;

    /**
     * 盘点人姓名
     */
    @TableField("checker_name")
    private String checkerName;

    /**
     * 备注
     */
    private String notes;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否有差异
     */
    public boolean hasDiscrepancy() {
        return discrepancy != null && discrepancy != 0;
    }

    /**
     * 计算差异
     */
    public void calculateDiscrepancy() {
        if (expectedQuantity != null && actualQuantity != null) {
            this.discrepancy = actualQuantity - expectedQuantity;
        }
    }

    /**
     * 是否可以录入结果
     */
    public boolean canCheck() {
        return status != null && status.canCheck();
    }
}
