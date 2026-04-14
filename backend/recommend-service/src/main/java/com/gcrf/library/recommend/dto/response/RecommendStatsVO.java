package com.gcrf.library.recommend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 推荐统计VO
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@Schema(description = "推荐效果统计")
public class RecommendStatsVO {

    @Schema(description = "推荐准确率（借阅转化/总推荐）")
    private Double precision;

    @Schema(description = "点击率CTR")
    private Double ctr;

    @Schema(description = "借阅转化率")
    private Double conversion;

    @Schema(description = "总推荐次数")
    private Long totalRecommendations;

    @Schema(description = "被点击次数")
    private Long clickedCount;

    @Schema(description = "借阅转化次数")
    private Long borrowedCount;

    @Schema(description = "各算法统计")
    private List<AlgorithmStats> algorithmStats;

    /**
     * 算法统计详情
     */
    @Data
    @Schema(description = "算法效果统计")
    public static class AlgorithmStats {

        @Schema(description = "算法名称")
        private String algorithm;

        @Schema(description = "推荐总数")
        private Long total;

        @Schema(description = "点击数")
        private Long clicked;

        @Schema(description = "借阅数")
        private Long borrowed;

        @Schema(description = "准确率")
        private Double precision;
    }
}
