package com.gcrf.library.book.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 库存调整请求
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
public class InventoryUpdateRequest {

    /**
     * 新的总量（必填）
     */
    @NotNull(message = "总量不能为空")
    @Min(value = 0, message = "总量不能为负数")
    private Integer totalCopies;

    /**
     * 调整原因（必填）
     */
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因长度不能超过500")
    private String reason;

    /**
     * 操作人ID（可选，通过SecurityContext获取）
     */
    private Long operatorId;

    /**
     * 操作人姓名（可选，通过SecurityContext获取）
     */
    private String operatorName;
}
