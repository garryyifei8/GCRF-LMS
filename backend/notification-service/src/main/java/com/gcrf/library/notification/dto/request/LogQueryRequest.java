package com.gcrf.library.notification.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询请求DTO（通用）
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class LogQueryRequest {

    /**
     * 发送状态: PENDING-待发送, SENDING-发送中, SENT-已发送, FAILED-失败
     */
    @Pattern(regexp = "^(PENDING|SENDING|SENT|FAILED)$", message = "状态必须为PENDING、SENDING、SENT或FAILED")
    private String status;

    /**
     * 开始日期
     */
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    private LocalDateTime endDate;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
