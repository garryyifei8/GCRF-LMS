package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 发送通知
     *
     * POST /api/v1/notifications
     */
    @PostMapping
    public Result<NotificationVO> sendNotification(@Valid @RequestBody NotificationSendRequest request) {
        log.info("发送通知请求: targetType={}, targetIds={}",
                request.getTargetType(), request.getTargetIds());
        NotificationVO notification = notificationService.sendNotification(request);
        return Result.success(notification);
    }

    /**
     * 查询通知列表(分页)
     *
     * GET /api/v1/notifications?pageNum=1&pageSize=20
     */
    @GetMapping
    public Result<PageResult<NotificationVO>> queryNotifications(
            @Valid NotificationQueryRequest request
    ) {
        log.info("查询通知列表: pageNum={}, pageSize={}", request.getPageNum(), request.getPageSize());
        PageResult<NotificationVO> result = notificationService.queryNotifications(request);
        return Result.success(result);
    }

    /**
     * 获取通知详情
     *
     * GET /api/v1/notifications/{id}
     */
    @GetMapping("/{id}")
    public Result<NotificationVO> getNotificationById(@PathVariable Long id) {
        log.info("获取通知详情: id={}", id);
        NotificationVO notification = notificationService.getNotificationById(id);
        return Result.success(notification);
    }

    /**
     * 标记通知为已读
     *
     * PUT /api/v1/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public Result<NotificationVO> markAsRead(@PathVariable Long id) {
        log.info("标记通知为已读: id={}", id);
        NotificationVO notification = notificationService.markAsRead(id);
        return Result.success(notification);
    }

    /**
     * 批量标记通知为已读
     *
     * PUT /api/v1/notifications/batch-read
     */
    @PutMapping("/batch-read")
    public Result<Void> batchMarkAsRead(@RequestBody List<Long> ids) {
        log.info("批量标记通知为已读: count={}", ids.size());
        notificationService.batchMarkAsRead(ids);
        return Result.success();
    }

    /**
     * 删除通知
     *
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        log.info("删除通知: id={}", id);
        notificationService.deleteNotification(id);
        return Result.success();
    }

    /**
     * 批量删除通知
     *
     * DELETE /api/v1/notifications/batch
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteNotifications(@RequestBody List<Long> ids) {
        log.info("批量删除通知: count={}", ids.size());
        notificationService.batchDeleteNotifications(ids);
        return Result.success();
    }

    /**
     * 获取用户未读通知数量
     *
     * GET /api/v1/notifications/unread-count?userId=123
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        log.info("获取用户未读通知数量: userId={}", userId);
        long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 获取用户最新通知列表
     *
     * GET /api/v1/notifications/latest?userId=123&limit=10
     */
    @GetMapping("/latest")
    public Result<List<NotificationVO>> getLatestNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        log.info("获取用户最新通知: userId={}, limit={}", userId, limit);
        List<NotificationVO> notifications = notificationService.getLatestNotifications(userId, limit);
        return Result.success(notifications);
    }

    /**
     * 清空用户所有通知
     *
     * DELETE /api/v1/notifications/clear?userId=123
     */
    @DeleteMapping("/clear")
    public Result<Void> clearAllNotifications(@RequestParam Long userId) {
        log.info("清空用户所有通知: userId={}", userId);
        notificationService.clearAllNotifications(userId);
        return Result.success();
    }
}
