package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;
import com.gcrf.library.notification.service.NotificationSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 通知订阅管理控制器
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    /**
     * 获取用户订阅配置
     *
     * GET /api/v1/subscriptions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<SubscriptionVO> getUserSubscription(@PathVariable Long userId) {
        log.info("获取用户订阅配置: userId={}", userId);
        SubscriptionVO subscription = subscriptionService.getUserSubscription(userId);
        return Result.success(subscription);
    }

    /**
     * 更新用户订阅配置
     *
     * PUT /api/v1/subscriptions/user/{userId}
     */
    @PutMapping("/user/{userId}")
    public Result<SubscriptionVO> updateSubscription(
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionUpdateRequest request
    ) {
        log.info("更新用户订阅配置: userId={}", userId);
        SubscriptionVO subscription = subscriptionService.updateSubscription(userId, request);
        return Result.success(subscription);
    }

    /**
     * 检查用户是否订阅了某类型通知
     *
     * GET /api/v1/subscriptions/check?userId=123&notificationType=SYSTEM
     */
    @GetMapping("/check")
    public Result<Boolean> checkSubscription(
            @RequestParam Long userId,
            @RequestParam String notificationType
    ) {
        boolean subscribed = subscriptionService.isSubscribed(userId, notificationType);
        return Result.success(subscribed);
    }
}
