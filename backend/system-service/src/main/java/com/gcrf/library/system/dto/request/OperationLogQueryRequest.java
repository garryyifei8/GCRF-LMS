package com.gcrf.library.system.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class OperationLogQueryRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 执行状态
     */
    private String status;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
