package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.SubscriptionCreateRequest;
import com.gcrf.library.notification.dto.request.SubscriptionQueryRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;
import com.gcrf.library.notification.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    private final SubscriptionService subscriptionService;

    /**
     * 创建通知订阅
     *
     * POST /api/v1/subscriptions
     */
    @PostMapping
    public Result<SubscriptionVO> createSubscription(@Valid @RequestBody SubscriptionCreateRequest request) {
        log.info("创建通知订阅: userId={}, notificationType={}, channel={}",
                request.getUserId(), request.getNotificationType(), request.getChannel());
        SubscriptionVO subscription = subscriptionService.createSubscription(request);
        return Result.success(subscription);
    }

    /**
     * 删除通知订阅
     *
     * DELETE /api/v1/subscriptions/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSubscription(@PathVariable Long id) {
        log.info("删除通知订阅: id={}", id);
        subscriptionService.deleteSubscription(id);
        return Result.success();
    }

    /**
     * 获取订阅详情
     *
     * GET /api/v1/subscriptions/{id}
     */
    @GetMapping("/{id}")
    public Result<SubscriptionVO> getSubscriptionById(@PathVariable Long id) {
        log.info("获取订阅详情: id={}", id);
        SubscriptionVO subscription = subscriptionService.getSubscriptionById(id);
        return Result.success(subscription);
    }

    /**
     * 查询订阅列表(分页)
     *
     * GET /api/v1/subscriptions?pageNum=1&pageSize=20
     */
    @GetMapping
    public Result<PageResult<SubscriptionVO>> querySubscriptions(@Valid SubscriptionQueryRequest request) {
        log.info("查询订阅列表: pageNum={}, pageSize={}", request.getPageNum(), request.getPageSize());
        PageResult<SubscriptionVO> result = subscriptionService.querySubscriptions(request);
        return Result.success(result);
    }

    /**
     * 获取用户所有订阅
     *
     * GET /api/v1/subscriptions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<SubscriptionVO>> getUserSubscriptions(@PathVariable Long userId) {
        log.info("获取用户所有订阅: userId={}", userId);
        List<SubscriptionVO> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return Result.success(subscriptions);
    }

    /**
     * 启用/禁用订阅
     *
     * PUT /api/v1/subscriptions/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<SubscriptionVO> changeSubscriptionStatus(
            @PathVariable Long id,
            @RequestParam Boolean enabled
    ) {
        log.info("修改订阅状态: id={}, enabled={}", id, enabled);
        SubscriptionVO subscription = subscriptionService.changeSubscriptionStatus(id, enabled);
        return Result.success(subscription);
    }

    /**
     * 批量启用/禁用订阅
     *
     * PUT /api/v1/subscriptions/batch-status
     */
    @PutMapping("/batch-status")
    public Result<Void> batchChangeSubscriptionStatus(
            @RequestBody List<Long> ids,
            @RequestParam Boolean enabled
    ) {
        log.info("批量修改订阅状态: count={}, enabled={}", ids.size(), enabled);
        subscriptionService.batchChangeSubscriptionStatus(ids, enabled);
        return Result.success();
    }

    /**
     * 更新订阅偏好
     *
     * PUT /api/v1/subscriptions/{id}/preferences
     */
    @PutMapping("/{id}/preferences")
    public Result<SubscriptionVO> updateSubscriptionPreferences(
            @PathVariable Long id,
            @RequestBody SubscriptionCreateRequest request
    ) {
        log.info("更新订阅偏好: id={}", id);
        SubscriptionVO subscription = subscriptionService.updateSubscriptionPreferences(id, request);
        return Result.success(subscription);
    }

    /**
     * 检查用户是否订阅了某类型通知
     *
     * GET /api/v1/subscriptions/check?userId=123&notificationType=SYSTEM&channel=EMAIL
     */
    @GetMapping("/check")
    public Result<Boolean> checkSubscription(
            @RequestParam Long userId,
            @RequestParam String notificationType,
            @RequestParam String channel
    ) {
        log.info("检查用户订阅: userId={}, notificationType={}, channel={}",
                userId, notificationType, channel);
        boolean subscribed = subscriptionService.isUserSubscribed(userId, notificationType, channel);
        return Result.success(subscribed);
    }
}
