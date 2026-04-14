package com.gcrf.library.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 对话统计响应VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话统计响应")
public class ChatStatsVO {

    /**
     * 总提问数
     */
    @Schema(description = "总提问数")
    private Long totalQuestions;

    /**
     * 解答成功率(%)
     */
    @Schema(description = "解答成功率(%)")
    private BigDecimal successRate;

    /**
     * 平均响应时间(秒)
     */
    @Schema(description = "平均响应时间(秒)")
    private BigDecimal avgResponseTime;

    /**
     * 满意度评分(1-5)
     */
    @Schema(description = "满意度评分(1-5)")
    private BigDecimal satisfaction;

    /**
     * 今日提问数
     */
    @Schema(description = "今日提问数")
    private Long todayQuestions;

    /**
     * 活跃会话数
     */
    @Schema(description = "活跃会话数")
    private Long activeSessions;
}
