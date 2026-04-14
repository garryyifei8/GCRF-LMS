package com.gcrf.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcrf.library.book.entity.enums.TaskScope;
import com.gcrf.library.book.entity.enums.TaskStatus;
import com.gcrf.library.book.entity.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点任务实体
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@TableName("inventory_task")
public class InventoryTask {

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 任务编号（唯一）
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 任务类型
     */
    @TableField("task_type")
    private TaskType taskType;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 盘点范围
     */
    private TaskScope scope;

    /**
     * 盘点位置（当scope为LOCATION时使用）
     */
    private String location;

    /**
     * 实际开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 实际结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 计划开始时间
     */
    @TableField("plan_start_time")
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    @TableField("plan_end_time")
    private LocalDateTime planEndTime;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 总图书数
     */
    @TableField("total_books")
    private Integer totalBooks;

    /**
     * 已盘点数
     */
    @TableField("checked_books")
    private Integer checkedBooks;

    /**
     * 差异数量
     */
    @TableField("discrepancy_count")
    private Integer discrepancyCount;

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
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;

    /**
     * 计算进度百分比
     */
    public int getProgressPercentage() {
        if (totalBooks == null || totalBooks == 0) {
            return 0;
        }
        int checked = checkedBooks != null ? checkedBooks : 0;
        return (int) Math.round((double) checked / totalBooks * 100);
    }

    /**
     * 是否可以开始
     */
    public boolean canStart() {
        return status != null && status.canStart();
    }

    /**
     * 是否可以完成
     */
    public boolean canComplete() {
        return status != null && status.canComplete();
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return status != null && status.canCancel();
    }

    /**
     * 是否可以编辑
     */
    public boolean canEdit() {
        return status != null && status.canEdit();
    }
}
