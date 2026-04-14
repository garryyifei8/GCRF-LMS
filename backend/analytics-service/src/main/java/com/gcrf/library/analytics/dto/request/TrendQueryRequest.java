package com.gcrf.library.analytics.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 趋势查询请求
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Schema(description = "趋势查询请求")
public class TrendQueryRequest {

    @Schema(description = "时间范围", example = "LAST_30_DAYS",
            allowableValues = {"LAST_7_DAYS", "LAST_30_DAYS", "THIS_MONTH", "THIS_YEAR"})
    private String timeRange = "LAST_30_DAYS";

    @Schema(description = "粒度", example = "DAILY",
            allowableValues = {"DAILY", "WEEKLY", "MONTHLY"})
    private String granularity = "DAILY";
}
