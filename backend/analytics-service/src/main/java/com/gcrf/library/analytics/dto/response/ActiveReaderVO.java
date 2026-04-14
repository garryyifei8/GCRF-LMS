package com.gcrf.library.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活跃读者VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "活跃读者数据")
public class ActiveReaderVO {

    @Schema(description = "排名", example = "1")
    private Integer rank;

    @Schema(description = "读者ID", example = "1")
    private Long readerId;

    @Schema(description = "读者证号", example = "RD00000001")
    private String cardNo;

    @Schema(description = "读者姓名", example = "张三")
    private String realName;

    @Schema(description = "读者类型编码", example = "student")
    private String readerType;

    @Schema(description = "读者类型名称", example = "学生")
    private String readerTypeName;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "借阅次数", example = "156")
    private Long borrowCount;

    @Schema(description = "到馆次数", example = "320")
    private Long visitCount;

    @Schema(description = "偏好分类", example = "文学")
    private String favoriteCategory;

    @Schema(description = "最后借阅日期")
    private LocalDateTime lastBorrowDate;

    @Schema(description = "当前借阅数", example = "5")
    private Integer currentBorrowCount;

    @Schema(description = "累计逾期次数", example = "2")
    private Integer overdueCount;
}
