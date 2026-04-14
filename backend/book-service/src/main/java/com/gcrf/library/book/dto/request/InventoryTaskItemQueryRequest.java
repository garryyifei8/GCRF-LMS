package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 盘点明细查询请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "盘点明细查询请求")
public class InventoryTaskItemQueryRequest {

    /**
     * 关键词（图书标题、ISBN）
     */
    @Schema(description = "关键词（图书标题、ISBN）")
    private String keyword;

    /**
     * 状态：PENDING-待盘点，CHECKED-已盘点，SKIPPED-已跳过
     */
    @Schema(description = "状态", allowableValues = {"PENDING", "CHECKED", "SKIPPED"})
    private String status;

    /**
     * 是否只显示有差异的
     */
    @Schema(description = "是否只显示有差异的")
    private Boolean discrepancyOnly;

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
