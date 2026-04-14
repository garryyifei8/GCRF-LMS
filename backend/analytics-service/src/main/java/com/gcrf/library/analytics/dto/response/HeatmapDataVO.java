package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 热力图数据VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热力图数据")
public class HeatmapDataVO {

    @Schema(description = "X轴标签（小时）")
    private List<String> hours;

    @Schema(description = "Y轴标签（星期）")
    private List<String> days;

    @Schema(description = "热力图数据点", example = "[[0, 0, 50], [1, 0, 75]]")
    private List<int[]> data;

    @Schema(description = "最小值", example = "0")
    private Integer minValue;

    @Schema(description = "最大值", example = "100")
    private Integer maxValue;

    /**
     * 单个数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        @Schema(description = "X轴索引（小时）", example = "0")
        private Integer x;

        @Schema(description = "Y轴索引（星期）", example = "0")
        private Integer y;

        @Schema(description = "值", example = "50")
        private Integer value;
    }
}
