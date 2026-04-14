package com.gcrf.library.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史响应VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话历史响应")
public class ChatHistoryVO {

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private String sessionId;

    /**
     * 读者ID
     */
    @Schema(description = "读者ID")
    private Long readerId;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    /**
     * 消息总数
     */
    @Schema(description = "消息总数")
    private Integer messageCount;

    /**
     * 消息列表
     */
    @Schema(description = "消息列表")
    private List<ChatMessageVO> messages;
}
