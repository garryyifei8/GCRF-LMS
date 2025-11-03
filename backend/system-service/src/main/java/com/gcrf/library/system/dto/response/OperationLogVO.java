package com.gcrf.library.system.dto.response;

import com.gcrf.library.system.entity.OperationLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class OperationLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 执行状态
     */
    private String status;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作地点
     */
    private String location;

    /**
     * 操作系统
     */
    private String osInfo;

    /**
     * 浏览器信息
     */
    private String browserInfo;

    /**
     * 执行时长 (毫秒)
     */
    private Integer executionTime;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static OperationLogVO from(OperationLog log) {
        if (log == null) {
            return null;
        }
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setDeptName(log.getDeptName());
        vo.setOperation(log.getOperation());
        vo.setOperationType(log.getOperationType());
        vo.setBusinessType(log.getBusinessType());
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setHttpMethod(log.getHttpMethod());
        vo.setStatus(log.getStatus());
        vo.setIpAddress(log.getIpAddress());
        vo.setLocation(log.getLocation());
        vo.setOsInfo(log.getOsInfo());
        vo.setBrowserInfo(log.getBrowserInfo());
        vo.setExecutionTime(log.getExecutionTime());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
