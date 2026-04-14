package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建盘点任务请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "创建盘点任务请求")
public class InventoryTaskCreateRequest {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 200, message = "任务名称长度不能超过200")
    @Schema(description = "任务名称", required = true)
    private String taskName;

    /**
     * 任务类型：FULL-全面盘点，PARTIAL-部分盘点，SPOT-抽查盘点
     */
    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "^(FULL|PARTIAL|SPOT)$", message = "任务类型只能是FULL、PARTIAL或SPOT")
    @Schema(description = "任务类型", required = true, allowableValues = {"FULL", "PARTIAL", "SPOT"})
    private String taskType;

    /**
     * 盘点范围：ALL-全部，LOCATION-按位置，CATEGORY-按分类
     */
    @NotBlank(message = "盘点范围不能为空")
    @Pattern(regexp = "^(ALL|LOCATION|CATEGORY)$", message = "盘点范围只能是ALL、LOCATION或CATEGORY")
    @Schema(description = "盘点范围", required = true, allowableValues = {"ALL", "LOCATION", "CATEGORY"})
    private String scope;

    /**
     * 盘点位置（当scope为LOCATION时必填）
     */
    @Size(max = 100, message = "盘点位置长度不能超过100")
    @Schema(description = "盘点位置（当scope为LOCATION时必填）")
    private String location;

    /**
     * 分类代码（当scope为CATEGORY时使用）
     */
    @Schema(description = "分类代码（当scope为CATEGORY时使用）")
    private String categoryCode;

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

    /**
     * 指定图书ID列表（当taskType为SPOT时使用）
     */
    @Schema(description = "指定图书ID列表（抽查盘点时使用）")
    private List<Long> bookIds;
}
