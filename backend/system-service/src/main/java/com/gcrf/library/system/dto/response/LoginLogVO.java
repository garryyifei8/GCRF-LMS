package com.gcrf.library.system.dto.response;

import com.gcrf.library.system.entity.LoginLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志VO
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Data
public class LoginLogVO {

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
     * 登录类型
     */
    private String loginType;

    /**
     * 登录方式
     */
    private String loginMethod;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 登录地点
     */
    private String location;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录状态
     */
    private String status;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static LoginLogVO from(LoginLog log) {
        if (log == null) {
            return null;
        }
        LoginLogVO vo = new LoginLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setDeptName(log.getDeptName());
        vo.setLoginType(log.getLoginType());
        vo.setLoginMethod(log.getLoginMethod());
        vo.setIpAddress(log.getIpAddress());
        vo.setLocation(log.getLocation());
        vo.setBrowser(log.getBrowser());
        vo.setOs(log.getOs());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
