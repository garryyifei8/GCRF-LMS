package com.gcrf.library.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("login_logs")
public class LoginLog implements Serializable {

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
     * 登录类型: WEB-网页, MOBILE-移动端, API-API接口
     */
    @TableField("login_type")
    private String loginType;

    /**
     * 登录方式: PASSWORD-密码, SMS-短信, WECHAT-微信, QR_CODE-扫码
     */
    @TableField("login_method")
    private String loginMethod;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 登录地点 (根据IP解析)
     */
    @TableField("location")
    private String location;

    /**
     * 浏览器
     */
    @TableField("browser")
    private String browser;

    /**
     * 操作系统
     */
    @TableField("os")
    private String os;

    /**
     * User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 登录状态: SUCCESS-成功, FAILURE-失败
     */
    @TableField("status")
    private String status;

    /**
     * 失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * JWT Token (可选,用于追踪)
     */
    @TableField("token")
    private String token;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
