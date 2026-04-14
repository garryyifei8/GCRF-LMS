package com.gcrf.library.analytics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 排行榜查询请求
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Schema(description = "排行榜查询请求")
public class RankingQueryRequest {

    @Schema(description = "排序依据", example = "BORROW_COUNT",
            allowableValues = {"BORROW_COUNT", "RATING", "VISIT_COUNT"})
    private String rankBy = "BORROW_COUNT";

    @Schema(description = "时间范围", example = "THIS_MONTH",
            allowableValues = {"LAST_7_DAYS", "THIS_MONTH", "THIS_YEAR", "ALL_TIME"})
    private String timeRange = "THIS_MONTH";

    @Min(1)
    @Max(100)
    @Schema(description = "返回数量限制", example = "20")
    private Integer limit = 20;

    @Schema(description = "分类编码（可选）", example = "I")
    private String categoryCode;
}
