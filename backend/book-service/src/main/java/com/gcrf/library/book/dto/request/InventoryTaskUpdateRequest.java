package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新盘点任务请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "更新盘点任务请求")
public class InventoryTaskUpdateRequest {

    /**
     * 任务名称
     */
    @Size(max = 200, message = "任务名称长度不能超过200")
    @Schema(description = "任务名称")
    private String taskName;

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
    @Size(max = 100, message = "操作人姓名长度不能超过100")
    @Schema(description = "操作人姓名")
    private String operatorName;

    /**
     * 备注
     */
    @Size(max = 2000, message = "备注长度不能超过2000")
    @Schema(description = "备注")
    private String notes;
}
