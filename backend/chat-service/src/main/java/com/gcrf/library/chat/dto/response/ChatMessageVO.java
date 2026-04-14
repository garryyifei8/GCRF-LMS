package com.gcrf.library.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息响应VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天消息响应")
public class ChatMessageVO {

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private Long id;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private String sessionId;

    /**
     * 角色: user, assistant, system
     */
    @Schema(description = "角色: user, assistant, system")
    private String role;

    /**
     * 消息内容（HTML格式）
     */
    @Schema(description = "消息内容（HTML格式）")
    private String content;

    /**
     * 识别的意图编码
     */
    @Schema(description = "识别的意图编码")
    private String intentCode;

    /**
     * 意图置信度(0-1)
     */
    @Schema(description = "意图置信度(0-1)")
    private BigDecimal confidence;

    /**
     * 匹配的FAQ ID
     */
    @Schema(description = "匹配的FAQ ID")
    private Long matchedFaqId;

    /**
     * 推荐的相关问题
     */
    @Schema(description = "推荐的相关问题")
    private List<RelatedQuestionVO> relatedQuestions;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 相关问题VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "相关问题")
    public static class RelatedQuestionVO {
        @Schema(description = "FAQ ID")
        private Long faqId;

        @Schema(description = "问题内容")
        private String question;
    }
}
