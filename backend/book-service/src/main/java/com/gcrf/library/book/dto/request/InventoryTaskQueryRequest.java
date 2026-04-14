package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点任务查询请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "盘点任务查询请求")
public class InventoryTaskQueryRequest {

    /**
     * 关键词（任务名称、任务编号）
     */
    @Schema(description = "关键词（任务名称、任务编号）")
    private String keyword;

    /**
     * 任务类型：FULL-全面盘点，PARTIAL-部分盘点，SPOT-抽查盘点
     */
    @Schema(description = "任务类型", allowableValues = {"FULL", "PARTIAL", "SPOT"})
    private String taskType;

    /**
     * 任务状态：PENDING-待执行，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消
     */
    @Schema(description = "任务状态", allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"})
    private String status;

    /**
     * 操作人ID
     */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /**
     * 创建时间开始
     */
    @Schema(description = "创建时间开始")
    private LocalDateTime createdAtStart;

    /**
     * 创建时间结束
     */
    @Schema(description = "创建时间结束")
    private LocalDateTime createdAtEnd;

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer pageSize = 10;
}
