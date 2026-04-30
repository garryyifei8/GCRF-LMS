package com.gcrf.library.system.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.system.entity.SystemMessage;

/**
 * 系统消息服务接口
 *
 * @author GCRF Team
 * @since 2026-04-29
 */
public interface SystemMessageService {

    /**
     * 分页查询用户消息列表
     *
     * @param userId   用户ID
     * @param pageNum  当前页
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<SystemMessage> listByUser(Long userId, int pageNum, int pageSize);

    /**
     * 查询用户未读消息数量
     *
     * @param userId 用户ID
     * @return 未读数
     */
    long countUnread(Long userId);

    /**
     * 将指定消息标记为已读
     *
     * @param id 消息ID
     */
    void markAsRead(Long id);
}
