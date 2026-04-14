package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分类分布VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类分布数据")
public class CategoryDistributionVO {

    @Schema(description = "分类编码", example = "I")
    private String code;

    @Schema(description = "分类名称", example = "文学")
    private String name;

    @Schema(description = "图表颜色", example = "#5470c6")
    private String color;

    @Schema(description = "图书数量", example = "1500")
    private Long bookCount;

    @Schema(description = "借阅次数", example = "3500")
    private Long borrowCount;

    @Schema(description = "流通率", example = "0.75")
    private BigDecimal circulationRate;

    @Schema(description = "读者数量（借阅该分类的读者）", example = "800")
    private Long readerCount;

    @Schema(description = "占比", example = "0.15")
    private BigDecimal percentage;

    @Schema(description = "零借阅数量", example = "50")
    private Long zeroCirculationCount;

    @Schema(description = "零借阅率", example = "0.03")
    private BigDecimal zeroCirculationRate;
}
