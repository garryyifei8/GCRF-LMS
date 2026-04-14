package com.gcrf.library.recommend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推荐请求DTO
 *
 * @author GCRF Team
 * @since 2025-11-26
 */
@Data
@Schema(description = "推荐请求")
public class RecommendRequest {

    @Schema(description = "读者ID")
    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    @Schema(description = "推荐数量", example = "20")
    @Min(value = 1, message = "推荐数量至少为1")
    @Max(value = 100, message = "推荐数量最多为100")
    private Integer limit = 20;

    @Schema(description = "推荐算法", example = "HYBRID")
    private String algorithm = "HYBRID";

    @Schema(description = "推荐场景", example = "HOMEPAGE")
    private String scene = "HOMEPAGE";

    @Schema(description = "读者类型筛选", example = "all/student/teacher")
    private String readerType = "all";
}
