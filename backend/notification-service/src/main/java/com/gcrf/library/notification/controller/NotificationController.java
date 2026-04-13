package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.NotificationMarkReadRequest;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.dto.response.UnreadCountVO;
import com.gcrf.library.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public Result<NotificationVO> sendNotification(@Valid @RequestBody NotificationSendRequest request) {
        log.info("发送通知: userId={}", request.getUserId());
        NotificationVO notification = notificationService.sendNotification(request);
        return Result.success(notification);
    }

    @GetMapping
    public Result<PageResult<NotificationVO>> queryNotifications(
            @RequestParam Long userId,
            @Valid NotificationQueryRequest request
    ) {
        log.info("查询通知列表: userId={}, pageNum={}", userId, request.getPageNum());
        PageResult<NotificationVO> result = notificationService.queryNotifications(userId, request);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<NotificationVO> getNotificationById(
            @RequestParam Long userId,
            @PathVariable Long id
    ) {
        NotificationVO notification = notificationService.getNotificationById(userId, id);
        return Result.success(notification);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @RequestParam Long userId,
            @PathVariable Long id
    ) {
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setNotificationId(id);
        request.setUserId(userId);
        notificationService.markAsRead(userId, request);
        return Result.success();
    }

    @PutMapping("/batch-read")
    public Result<Void> batchMarkAsRead(
            @RequestParam Long userId,
            @RequestBody List<Long> ids
    ) {
        notificationService.batchMarkAsRead(userId, ids);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(
            @RequestParam Long userId,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(userId, id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDeleteNotifications(
            @RequestParam Long userId,
            @RequestBody List<Long> ids
    ) {
        notificationService.batchDeleteNotifications(userId, ids);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount(@RequestParam Long userId) {
        UnreadCountVO count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @GetMapping("/latest")
    public Result<List<NotificationVO>> getLatestNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<NotificationVO> notifications = notificationService.getLatestNotifications(userId, limit);
        return Result.success(notifications);
    }

    @DeleteMapping("/clear")
    public Result<Void> clearAllNotifications(@RequestParam Long userId) {
        notificationService.clearAllNotifications(userId);
        return Result.success();
    }
}
