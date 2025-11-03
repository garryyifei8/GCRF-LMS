package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.dto.request.LoginLogQueryRequest;
import com.gcrf.library.system.dto.response.LoginLogVO;
import com.gcrf.library.system.entity.LoginLog;
import com.gcrf.library.system.mapper.LoginLogMapper;
import com.gcrf.library.system.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录日志服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    public PageResult<LoginLogVO> queryLogs(LoginLogQueryRequest request) {
        Page<LoginLog> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getUserId() != null, LoginLog::getUserId, request.getUserId())
               .like(StringUtils.hasText(request.getUsername()), LoginLog::getUsername, request.getUsername())
               .eq(StringUtils.hasText(request.getLoginType()), LoginLog::getLoginType, request.getLoginType())
               .eq(StringUtils.hasText(request.getLoginMethod()), LoginLog::getLoginMethod, request.getLoginMethod())
               .eq(StringUtils.hasText(request.getStatus()), LoginLog::getStatus, request.getStatus())
               .eq(StringUtils.hasText(request.getIpAddress()), LoginLog::getIpAddress, request.getIpAddress())
               .ge(request.getStartTime() != null, LoginLog::getCreatedAt, request.getStartTime())
               .le(request.getEndTime() != null, LoginLog::getCreatedAt, request.getEndTime())
               .orderByDesc(LoginLog::getCreatedAt);

        Page<LoginLog> logPage = loginLogMapper.selectPage(page, wrapper);

        List<LoginLogVO> logVOList = logPage.getRecords().stream()
                .map(LoginLogVO::from)
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
    public void recordLogin(LoginLog log) {
        loginLogMapper.insert(log);
        LoginLogServiceImpl.log.debug("记录登录日志成功, userId: {}, username: {}, status: {}",
            log.getUserId(), log.getUsername(), log.getStatus());
    }
}
