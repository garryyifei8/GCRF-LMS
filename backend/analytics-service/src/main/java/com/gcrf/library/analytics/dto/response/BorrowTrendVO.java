package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 借阅趋势数据VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "借阅趋势数据")
public class BorrowTrendVO {

    @Schema(description = "日期", example = "2025-11-30")
    private LocalDate date;

    @Schema(description = "日期字符串", example = "2025-11-30")
    private String dateStr;

    @Schema(description = "借阅量", example = "85")
    private Long borrowed;

    @Schema(description = "归还量", example = "92")
    private Long returned;

    @Schema(description = "到馆人次", example = "320")
    private Long visits;

    @Schema(description = "新增读者", example = "5")
    private Long newReaders;

    @Schema(description = "预约量", example = "15")
    private Long reserved;

    @Schema(description = "续借量", example = "10")
    private Long renewed;
}
