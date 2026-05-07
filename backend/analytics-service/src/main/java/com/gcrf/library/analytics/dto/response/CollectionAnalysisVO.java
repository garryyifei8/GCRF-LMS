package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 馆藏分析VO
 *
 * @author GCRF Team
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "馆藏分析数据")
public class CollectionAnalysisVO {

    @Schema(description = "馆藏图书种数", example = "50")
    private Long totalBooks;

    @Schema(description = "馆藏册数", example = "572")
    private Long totalCopies;

    @Schema(description = "分类分布")
    private List<CategoryDistributionVO> categoryDistribution;

    @Schema(description = "在馆状态分布")
    private List<StatusItem> statusDistribution;

    @Schema(description = "馆龄分布")
    private List<AgeItem> ageDistribution;

    @Schema(description = "流通分析")
    private CirculationStats circulationAnalysis;

    /**
     * 在馆状态分布条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "在馆状态条目")
    public static class StatusItem {

        @Schema(description = "状态编码", example = "available")
        private String status;

        @Schema(description = "状态名称", example = "在架")
        private String statusName;

        @Schema(description = "数量", example = "520")
        private Long count;

        @Schema(description = "占比", example = "0.945")
        private BigDecimal percentage;
    }

    /**
     * 馆龄分布条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "馆龄分布条目")
    public static class AgeItem {

        @Schema(description = "年龄区间", example = "0-1年")
        private String range;

        @Schema(description = "数量", example = "30")
        private Long count;

        @Schema(description = "占比", example = "0.6")
        private BigDecimal percentage;
    }

    /**
     * 流通统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "流通统计")
    public static class CirculationStats {

        @Schema(description = "高流通图书数（年借阅≥10次）", example = "5")
        private Long highCirculation;

        @Schema(description = "中流通图书数（年借阅5-9次）", example = "15")
        private Long mediumCirculation;

        @Schema(description = "低流通图书数（年借阅1-4次）", example = "10")
        private Long lowCirculation;

        @Schema(description = "零流通图书数（年借阅0次）", example = "20")
        private Long zeroCirculation;
    }
}
