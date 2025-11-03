package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_logs")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 部门名称
     */
    @TableField("dept_name")
    private String deptName;

    /**
     * 操作描述
     */
    @TableField("operation")
    private String operation;

    /**
     * 操作类型: CREATE-新增, UPDATE-修改, DELETE-删除, QUERY-查询, EXPORT-导出, IMPORT-导入, OTHER-其他
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 业务类型
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 请求方法 (格式: Controller.method)
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求URL
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * HTTP方法
     */
    @TableField("http_method")
    private String httpMethod;

    /**
     * 请求参数 (JSON格式)
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应结果 (JSON格式,可能很大,仅记录概要)
     */
    @TableField("response_result")
    private String responseResult;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 执行状态: SUCCESS-成功, FAILURE-失败
     */
    @TableField("status")
    private String status;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 操作地点 (根据IP解析)
     */
    @TableField("location")
    private String location;

    /**
     * 浏览器User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 操作系统
     */
    @TableField("os_info")
    private String osInfo;

    /**
     * 浏览器信息
     */
    @TableField("browser_info")
    private String browserInfo;

    /**
     * 执行时长 (毫秒)
     */
    @TableField("execution_time")
    private Integer executionTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
