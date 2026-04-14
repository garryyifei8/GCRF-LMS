package com.gcrf.library.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交反馈请求DTO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Schema(description = "提交反馈请求")
public class ChatFeedbackRequest {

    /**
     * 会话ID
     */
    @Schema(description = "会话ID", required = true)
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 消息ID（可选）
     */
    @Schema(description = "消息ID（可选）")
    private Long messageId;

    /**
     * FAQ知识ID（可选）
     */
    @Schema(description = "FAQ知识ID（可选）")
    private Long faqId;

    /**
     * 反馈类型: helpful, unhelpful, report
     */
    @Schema(description = "反馈类型: helpful, unhelpful, report", required = true)
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    /**
     * 反馈评论（可选）
     */
    @Schema(description = "反馈评论（可选）")
    @Size(max = 500, message = "反馈评论长度不能超过500")
    private String comment;

    /**
     * 读者ID（可选）
     */
    @Schema(description = "读者ID（可选）")
    private Long readerId;
}
