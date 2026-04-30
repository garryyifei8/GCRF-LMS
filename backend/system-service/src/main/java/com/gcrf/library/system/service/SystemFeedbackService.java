package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.entity.SystemFeedback;

/**
 * 用户反馈服务接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
public interface SystemFeedbackService {

    /**
     * 提交用户反馈
     *
     * @param feedback 反馈信息
     * @return 保存后的反馈
     */
    SystemFeedback submit(SystemFeedback feedback);

    /**
     * 分页查询反馈列表
     *
     * @param userId   用户ID（null 表示查询全部）
     * @param pageNum  当前页
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<SystemFeedback> listByUser(Long userId, int pageNum, int pageSize);
}
