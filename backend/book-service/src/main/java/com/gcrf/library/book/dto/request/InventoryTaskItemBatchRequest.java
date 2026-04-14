package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量录入盘点结果请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "批量录入盘点结果请求")
public class InventoryTaskItemBatchRequest {

    /**
     * 盘点结果列表
     */
    @NotEmpty(message = "盘点结果列表不能为空")
    @Valid
    @Schema(description = "盘点结果列表", required = true)
    private List<InventoryTaskItemRequest> items;
}
