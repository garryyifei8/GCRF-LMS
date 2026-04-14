package com.gcrf.library.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热门问题响应VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热门问题响应")
public class HotQuestionVO {

    /**
     * 问题内容
     */
    @Schema(description = "问题内容")
    private String question;

    /**
     * 提问次数
     */
    @Schema(description = "提问次数")
    private Integer count;

    /**
     * 关联的FAQ ID
     */
    @Schema(description = "关联的FAQ ID")
    private Long faqId;
}
