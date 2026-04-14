package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 热门图书VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热门图书数据")
public class PopularBookVO {

    @Schema(description = "排名", example = "1")
    private Integer rank;

    @Schema(description = "图书ID", example = "1")
    private Long bookId;

    @Schema(description = "ISBN", example = "9787544253994")
    private String isbn;

    @Schema(description = "书名", example = "三体")
    private String title;

    @Schema(description = "作者", example = "刘慈欣")
    private String author;

    @Schema(description = "分类编码", example = "I247")
    private String categoryCode;

    @Schema(description = "分类名称", example = "文学")
    private String categoryName;

    @Schema(description = "封面URL")
    private String coverUrl;

    @Schema(description = "借阅次数", example = "356")
    private Long borrowCount;

    @Schema(description = "评分", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "总副本数", example = "10")
    private Integer totalCopies;

    @Schema(description = "可借副本数", example = "3")
    private Integer availableCopies;

    @Schema(description = "当前借出数", example = "7")
    private Integer borrowedCopies;

    @Schema(description = "预约数", example = "5")
    private Integer reservationCount;
}
