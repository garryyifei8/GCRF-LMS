package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.LoginLogQueryRequest;
import com.gcrf.library.system.dto.response.LoginLogVO;
import com.gcrf.library.system.entity.LoginLog;

/**
 * 登录日志服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface LoginLogService {

    /**
     * 分页查询登录日志
     */
    PageResult<LoginLogVO> queryLogs(LoginLogQueryRequest request);

    /**
     * 记录登录日志
     */
    void recordLogin(LoginLog log);
}
