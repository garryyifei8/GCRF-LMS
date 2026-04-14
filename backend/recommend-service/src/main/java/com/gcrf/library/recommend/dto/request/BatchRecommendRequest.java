package com.gcrf.library.recommend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 批量推荐请求DTO - 用于后台管理
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@Schema(description = "批量推荐请求")
public class BatchRecommendRequest {

    @Schema(description = "读者类型筛选", example = "all/STUDENT/TEACHER/STAFF/EXTERNAL")
    private String readerType = "all";

    @Schema(description = "推荐算法", example = "HYBRID/USER_CF/ITEM_CF/POPULAR")
    private String algorithm = "HYBRID";

    @Schema(description = "每位读者的推荐数量", example = "10")
    @Min(value = 1, message = "推荐数量至少为1")
    @Max(value = 50, message = "推荐数量最多为50")
    private Integer countPerReader = 10;

    @Schema(description = "推荐场景", example = "HOMEPAGE/DETAIL/SEARCH/TOPIC")
    private String scene = "HOMEPAGE";

    @Schema(description = "分页-页码", example = "1")
    @Min(value = 1)
    private Integer pageNum = 1;

    @Schema(description = "分页-每页数量", example = "20")
    @Min(value = 1)
    @Max(value = 100)
    private Integer pageSize = 20;
}
