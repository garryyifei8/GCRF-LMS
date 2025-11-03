package com.gcrf.library.system.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class LoginLogQueryRequest {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 登录类型
     */
    private String loginType;

    /**
     * 登录方式
     */
    private String loginMethod;

    /**
     * 登录状态
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
