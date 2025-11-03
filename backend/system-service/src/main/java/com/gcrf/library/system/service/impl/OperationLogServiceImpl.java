package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.OperationLogQueryRequest;
import com.gcrf.library.system.dto.response.OperationLogVO;
import com.gcrf.library.system.entity.OperationLog;
import com.gcrf.library.system.mapper.OperationLogMapper;
import com.gcrf.library.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLogVO> queryLogs(OperationLogQueryRequest request) {
        Page<OperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getUserId() != null, OperationLog::getUserId, request.getUserId())
               .like(StringUtils.hasText(request.getUsername()), OperationLog::getUsername, request.getUsername())
               .eq(StringUtils.hasText(request.getOperationType()), OperationLog::getOperationType, request.getOperationType())
               .eq(StringUtils.hasText(request.getBusinessType()), OperationLog::getBusinessType, request.getBusinessType())
               .eq(StringUtils.hasText(request.getStatus()), OperationLog::getStatus, request.getStatus())
               .eq(StringUtils.hasText(request.getIpAddress()), OperationLog::getIpAddress, request.getIpAddress())
               .ge(request.getStartTime() != null, OperationLog::getCreatedAt, request.getStartTime())
               .le(request.getEndTime() != null, OperationLog::getCreatedAt, request.getEndTime())
               .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);

        List<OperationLogVO> logVOList = logPage.getRecords().stream()
                .map(OperationLogVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                logPage.getTotal(),
                (int) logPage.getCurrent(),
                (int) logPage.getSize(),
                logVOList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createLog(OperationLog log) {
        operationLogMapper.insert(log);
        OperationLogServiceImpl.log.debug("记录操作日志成功, userId: {}, operation: {}", log.getUserId(), log.getOperation());
    }
}
