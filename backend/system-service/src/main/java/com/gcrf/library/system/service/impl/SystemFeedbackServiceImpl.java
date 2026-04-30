package com.gcrf.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.entity.SystemFeedback;
import com.gcrf.library.system.mapper.SystemFeedbackMapper;
import com.gcrf.library.system.service.SystemFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户反馈服务实现类
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemFeedbackServiceImpl implements SystemFeedbackService {

    private final SystemFeedbackMapper feedbackMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemFeedback submit(SystemFeedback feedback) {
        if (feedback.getFeedbackType() == null) {
            feedback.setFeedbackType("OTHER");
        }
        feedback.setStatus("PENDING");
        feedbackMapper.insert(feedback);
        log.info("用户反馈提交成功, id: {}, userId: {}", feedback.getId(), feedback.getUserId());
        return feedback;
    }

    @Override
    public PageResult<SystemFeedback> listByUser(Long userId, int pageNum, int pageSize) {
        Page<SystemFeedback> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemFeedback> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SystemFeedback::getUserId, userId);
        }
        wrapper.orderByDesc(SystemFeedback::getCreatedAt);
        Page<SystemFeedback> resultPage = feedbackMapper.selectPage(page, wrapper);

        List<SystemFeedback> records = resultPage.getRecords();
        return PageResult.ofRecords(
                resultPage.getTotal(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                records
        );
    }
}
