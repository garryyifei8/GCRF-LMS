package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 录入盘点结果请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "录入盘点结果请求")
public class InventoryTaskItemRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    @Schema(description = "图书ID", required = true)
    private Long bookId;

    /**
     * 实际数量
     */
    @NotNull(message = "实际数量不能为空")
    @Min(value = 0, message = "实际数量不能为负数")
    @Schema(description = "实际数量", required = true)
    private Integer actualQuantity;

    /**
     * 盘点人ID
     */
    @Schema(description = "盘点人ID")
    private Long checkerId;

    /**
     * 盘点人姓名
     */
    @Size(max = 100, message = "盘点人姓名长度不能超过100")
    @Schema(description = "盘点人姓名")
    private String checkerName;

    /**
     * 备注
     */
    @Size(max = 2000, message = "备注长度不能超过2000")
    @Schema(description = "备注")
    private String notes;
}
