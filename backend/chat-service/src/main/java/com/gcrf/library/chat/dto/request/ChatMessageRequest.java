package com.gcrf.library.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息请求DTO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Schema(description = "发送消息请求")
public class ChatMessageRequest {

    /**
     * 会话ID（可选，新会话时不传）
     */
    @Schema(description = "会话ID（可选，新会话时不传）")
    @Size(max = 64, message = "会话ID长度不能超过64")
    private String sessionId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容长度不能超过1000")
    private String content;

    /**
     * 读者ID（可选，登录用户才有）
     */
    @Schema(description = "读者ID（可选）")
    private Long readerId;
}
