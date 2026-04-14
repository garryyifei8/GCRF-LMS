package com.gcrf.library.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 库存查询请求
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "库存查询请求")
public class InventoryQueryRequest {

    /**
     * 关键词（图书标题、ISBN）
     */
    @Schema(description = "关键词（图书标题、ISBN）")
    private String keyword;

    /**
     * 存放位置
     */
    @Schema(description = "存放位置")
    private String location;

    /**
     * 书架号
     */
    @Schema(description = "书架号")
    private String shelfNumber;

    /**
     * 是否只显示预警库存
     */
    @Schema(description = "是否只显示预警库存")
    private Boolean alertOnly;

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
