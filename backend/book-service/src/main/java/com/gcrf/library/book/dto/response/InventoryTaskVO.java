package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.InventoryTask;
import com.gcrf.library.book.entity.enums.TaskScope;
import com.gcrf.library.book.entity.enums.TaskStatus;
import com.gcrf.library.book.entity.enums.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点任务响应VO
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "盘点任务响应")
public class InventoryTaskVO {

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    private Long id;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称")
    private String taskName;

    /**
     * 任务编号
     */
    @Schema(description = "任务编号")
    private String taskCode;

    /**
     * 任务类型
     */
    @Schema(description = "任务类型")
    private TaskType taskType;

    /**
     * 任务类型描述
     */
    @Schema(description = "任务类型描述")
    private String taskTypeDesc;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态")
    private TaskStatus status;

    /**
     * 任务状态描述
     */
    @Schema(description = "任务状态描述")
    private String statusDesc;

    /**
     * 盘点范围
     */
    @Schema(description = "盘点范围")
    private TaskScope scope;

    /**
     * 盘点范围描述
     */
    @Schema(description = "盘点范围描述")
    private String scopeDesc;

    /**
     * 盘点位置
     */
    @Schema(description = "盘点位置")
    private String location;

    /**
     * 实际开始时间
     */
    @Schema(description = "实际开始时间")
    private LocalDateTime startTime;

    /**
     * 实际结束时间
     */
    @Schema(description = "实际结束时间")
    private LocalDateTime endTime;

    /**
     * 计划开始时间
     */
    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    /**
     * 操作人ID
     */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @Schema(description = "操作人姓名")
    private String operatorName;

    /**
     * 总图书数
     */
    @Schema(description = "总图书数")
    private Integer totalBooks;

    /**
     * 已盘点数
     */
    @Schema(description = "已盘点数")
    private Integer checkedBooks;

    /**
     * 差异数量
     */
    @Schema(description = "差异数量")
    private Integer discrepancyCount;

    /**
     * 进度百分比
     */
    @Schema(description = "进度百分比")
    private Integer progressPercentage;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String notes;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 是否可以开始
     */
    @Schema(description = "是否可以开始")
    private Boolean canStart;

    /**
     * 是否可以完成
     */
    @Schema(description = "是否可以完成")
    private Boolean canComplete;

    /**
     * 是否可以取消
     */
    @Schema(description = "是否可以取消")
    private Boolean canCancel;

    /**
     * 是否可以编辑
     */
    @Schema(description = "是否可以编辑")
    private Boolean canEdit;

    /**
     * 从实体转换
     */
    public static InventoryTaskVO from(InventoryTask entity) {
        if (entity == null) {
            return null;
        }
        InventoryTaskVO vo = new InventoryTaskVO();
        vo.setId(entity.getId());
        vo.setTaskName(entity.getTaskName());
        vo.setTaskCode(entity.getTaskCode());
        vo.setTaskType(entity.getTaskType());
        vo.setTaskTypeDesc(entity.getTaskType() != null ? entity.getTaskType().getDescription() : null);
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null ? entity.getStatus().getDescription() : null);
        vo.setScope(entity.getScope());
        vo.setScopeDesc(entity.getScope() != null ? entity.getScope().getDescription() : null);
        vo.setLocation(entity.getLocation());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setPlanStartTime(entity.getPlanStartTime());
        vo.setPlanEndTime(entity.getPlanEndTime());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setTotalBooks(entity.getTotalBooks());
        vo.setCheckedBooks(entity.getCheckedBooks());
        vo.setDiscrepancyCount(entity.getDiscrepancyCount());
        vo.setProgressPercentage(entity.getProgressPercentage());
        vo.setNotes(entity.getNotes());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCanStart(entity.canStart());
        vo.setCanComplete(entity.canComplete());
        vo.setCanCancel(entity.canCancel());
        vo.setCanEdit(entity.canEdit());
        return vo;
    }
}
