package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 库存调整请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "库存调整请求")
public class InventoryAdjustRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    @Schema(description = "图书ID", required = true)
    private Long bookId;

    /**
     * 调整类型：ADD-增加，REDUCE-减少，SET-设置
     */
    @NotBlank(message = "调整类型不能为空")
    @Pattern(regexp = "^(ADD|REDUCE|SET)$", message = "调整类型只能是ADD、REDUCE或SET")
    @Schema(description = "调整类型：ADD-增加，REDUCE-减少，SET-设置", required = true, allowableValues = {"ADD", "REDUCE", "SET"})
    private String adjustType;

    /**
     * 调整数量（当adjustType为ADD或REDUCE时使用）或目标数量（当adjustType为SET时使用）
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    @Schema(description = "调整数量或目标数量", required = true)
    private Integer quantity;

    /**
     * 调整原因
     */
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因长度不能超过500")
    @Schema(description = "调整原因", required = true)
    private String reason;

    /**
     * 存放位置（可选）
     */
    @Size(max = 100, message = "存放位置长度不能超过100")
    @Schema(description = "存放位置")
    private String location;

    /**
     * 书架号（可选）
     */
    @Size(max = 50, message = "书架号长度不能超过50")
    @Schema(description = "书架号")
    private String shelfNumber;

    /**
     * 预警阈值（可选）
     */
    @Min(value = 0, message = "预警阈值不能为负数")
    @Schema(description = "预警阈值")
    private Integer alertThreshold;
}
