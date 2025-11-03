package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.NotificationMarkReadRequest;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.dto.response.UnreadCountVO;
import com.gcrf.library.notification.entity.Notification;
import com.gcrf.library.notification.mapper.NotificationMapper;
import com.gcrf.library.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 站内通知服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationVO sendNotification(NotificationSendRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setNotificationType(request.getNotificationType());
        notification.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        notification.setIsRead(false);
        notification.setExtraData(request.getExtraData());

        notificationMapper.insert(notification);
        log.info("发送通知成功, userId: {}, title: {}", request.getUserId(), request.getTitle());

        return NotificationVO.from(notification);
    }

    @Override
    public PageResult<NotificationVO> queryNotifications(Long userId, NotificationQueryRequest request) {
        Page<Notification> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .isNull(Notification::getDeletedAt)
               .eq(StringUtils.hasText(request.getNotificationType()),
                   Notification::getNotificationType, request.getNotificationType())
               .eq(request.getIsRead() != null, Notification::getIsRead, request.getIsRead())
               .eq(StringUtils.hasText(request.getPriority()),
                   Notification::getPriority, request.getPriority())
               .ge(request.getStartDate() != null, Notification::getCreatedAt, request.getStartDate())
               .le(request.getEndDate() != null, Notification::getCreatedAt, request.getEndDate())
               .orderByDesc(Notification::getCreatedAt);

        Page<Notification> result = notificationMapper.selectPage(page, wrapper);

        List<NotificationVO> voList = result.getRecords().stream()
                .map(NotificationVO::from)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                voList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, NotificationMarkReadRequest request) {
        // 支持两种模式: 1. 标记单个通知 2. 标记全部
        if (request.getMarkAll() != null && request.getMarkAll()) {
            // 标记全部已读
            markAllAsRead(userId);
            return;
        }

        // 标记单个通知
        if (request.getNotificationId() == null) {
            throw new BusinessException("通知ID不能为空");
        }

        Notification notification = notificationMapper.selectOne(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, request.getNotificationId())
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getDeletedAt)
        );

        if (notification == null) {
            throw new BusinessException("通知不存在或无权限, id: " + request.getNotificationId());
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }

        log.info("标记通知已读成功, userId: {}, notificationId: {}", userId, request.getNotificationId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false)
               .isNull(Notification::getDeletedAt)
               .set(Notification::getIsRead, true)
               .set(Notification::getReadAt, LocalDateTime.now());

        int count = notificationMapper.update(null, wrapper);
        log.info("标记全部通知已读成功, userId: {}, count: {}", userId, count);
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, false)
               .isNull(Notification::getDeletedAt);

        Long unreadCount = notificationMapper.selectCount(wrapper);

        // 统计紧急未读数量
        LambdaQueryWrapper<Notification> urgentWrapper = new LambdaQueryWrapper<>();
        urgentWrapper.eq(Notification::getUserId, userId)
                     .eq(Notification::getIsRead, false)
                     .eq(Notification::getPriority, "URGENT")
                     .isNull(Notification::getDeletedAt);

        Long urgentCount = notificationMapper.selectCount(urgentWrapper);

        UnreadCountVO vo = new UnreadCountVO();
        vo.setUserId(userId);
        vo.setUnreadCount(unreadCount);
        vo.setUrgentCount(urgentCount);

        return vo;
    }

    @Override
    public NotificationVO getNotificationById(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectOne(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getDeletedAt)
        );

        if (notification == null) {
            throw new BusinessException("通知不存在或无权限, id: " + notificationId);
        }

        return NotificationVO.from(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectOne(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getDeletedAt)
        );

        if (notification == null) {
            throw new BusinessException("通知不存在或无权限, id: " + notificationId);
        }

        notification.setDeletedAt(LocalDateTime.now());
        notificationMapper.updateById(notification);
        log.info("删除通知成功, id: {}", notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNotifications(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new BusinessException("通知ID列表不能为空");
        }

        for (Long notificationId : notificationIds) {
            deleteNotification(userId, notificationId);
        }

        log.info("批量删除通知成功, userId: {}, count: {}", userId, notificationIds.size());
    }
}
