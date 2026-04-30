package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.entity.SystemMessage;
import com.gcrf.library.system.mapper.SystemMessageMapper;
import com.gcrf.library.system.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统消息服务实现类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageServiceImpl implements SystemMessageService {

    private final SystemMessageMapper messageMapper;

    @Override
    public PageResult<SystemMessage> listByUser(Long userId, int pageNum, int pageSize) {
        Page<SystemMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemMessage> wrapper = new LambdaQueryWrapper<SystemMessage>()
                .eq(SystemMessage::getUserId, userId)
                .orderByDesc(SystemMessage::getCreatedAt);
        Page<SystemMessage> resultPage = messageMapper.selectPage(page, wrapper);

        List<SystemMessage> records = resultPage.getRecords();
        return PageResult.ofRecords(
                resultPage.getTotal(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                records
        );
    }

    @Override
    public long countUnread(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SystemMessage>()
                .eq(SystemMessage::getUserId, userId)
                .eq(SystemMessage::getIsRead, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id) {
        SystemMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("消息不存在, id: " + id);
        }
        message.setIsRead(true);
        messageMapper.updateById(message);
        log.info("消息已标记为已读, id: {}", id);
    }
}
