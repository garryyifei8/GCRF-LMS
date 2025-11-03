package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.OperationLogQueryRequest;
import com.gcrf.library.system.dto.response.OperationLogVO;
import com.gcrf.library.system.entity.OperationLog;

/**
 * 操作日志服务接口
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     */
    PageResult<OperationLogVO> queryLogs(OperationLogQueryRequest request);

    /**
     * 创建操作日志
     */
    void createLog(OperationLog log);
}
