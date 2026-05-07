package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 近期活动VO
 *
 * @author GCRF Team
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "近期活动记录")
public class RecentActivityVO {

    @Schema(description = "活动ID", example = "1")
    private Long id;

    @Schema(description = "活动类型编码", example = "borrow")
    private String type;

    @Schema(description = "活动类型名称", example = "借书")
    private String typeName;

    @Schema(description = "图标名称（Element Plus图标）", example = "DocumentAdd")
    private String icon;

    @Schema(description = "读者姓名", example = "韩雅静")
    private String readerName;

    @Schema(description = "书名", example = "罪与罚")
    private String bookTitle;

    @Schema(description = "活动描述", example = "借书《罪与罚》")
    private String description;

    @Schema(description = "活动时间（ISO格式）", example = "2026-05-06T11:30:00")
    private String timestamp;

    @Schema(description = "状态", example = "success")
    private String status;
}
