package com.gcrf.library.recommend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 推荐结果VO
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@Schema(description = "图书推荐结果")
public class RecommendationVO {

    @Schema(description = "读者ID")
    private Long readerId;

    @Schema(description = "读者姓名")
    private String readerName;

    @Schema(description = "图书ID")
    private Long bookId;

    @Schema(description = "图书标题")
    private String bookTitle;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "ISBN")
    private String isbn;

    @Schema(description = "封面URL")
    private String coverUrl;

    @Schema(description = "分类代码")
    private String categoryCode;

    @Schema(description = "推荐分数（0-1之间）")
    private Double score;

    @Schema(description = "推荐算法", example = "USER_CF/ITEM_CF/POPULAR/HYBRID")
    private String algorithm;

    @Schema(description = "推荐场景", example = "HOMEPAGE/DETAIL/SEARCH/TOPIC")
    private String scene;

    @Schema(description = "推荐理由")
    private String reason;

    @Schema(description = "读者偏好标签")
    private List<String> readerPreferences;

    @Schema(description = "图书标签")
    private List<String> bookTags;
}
