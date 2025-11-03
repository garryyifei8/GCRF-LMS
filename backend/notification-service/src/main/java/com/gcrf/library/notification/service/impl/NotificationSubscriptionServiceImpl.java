package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;
import com.gcrf.library.notification.entity.NotificationSubscription;
import com.gcrf.library.notification.mapper.NotificationSubscriptionMapper;
import com.gcrf.library.notification.service.NotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 用户通知订阅服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSubscriptionServiceImpl implements NotificationSubscriptionService {

    private final NotificationSubscriptionMapper subscriptionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SubscriptionVO getUserSubscription(Long userId) {
        NotificationSubscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<NotificationSubscription>()
                .eq(NotificationSubscription::getUserId, userId)
        );

        if (subscription == null) {
            // 返回默认配置
            SubscriptionVO defaultVO = new SubscriptionVO();
            defaultVO.setUserId(userId);
            defaultVO.setEmailEnabled(true);
            defaultVO.setSmsEnabled(true);
            defaultVO.setNotificationEnabled(true);
            defaultVO.setSubscribedTypes(convertListToJson(Arrays.asList("SYSTEM", "USER", "ACTIVITY")));
            return defaultVO;
        }

        return SubscriptionVO.from(subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscriptionVO updateSubscription(Long userId, SubscriptionUpdateRequest request) {
        NotificationSubscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<NotificationSubscription>()
                .eq(NotificationSubscription::getUserId, userId)
        );

        if (subscription == null) {
            // 创建新订阅配置
            subscription = new NotificationSubscription();
            subscription.setUserId(userId);
            subscription.setEmailEnabled(request.getEmailEnabled() != null ? request.getEmailEnabled() : true);
            subscription.setSmsEnabled(request.getSmsEnabled() != null ? request.getSmsEnabled() : true);
            subscription.setNotificationEnabled(request.getNotificationEnabled() != null ? request.getNotificationEnabled() : true);

            // 转换List为JSONB字符串
            if (request.getSubscribedTypes() != null) {
                subscription.setSubscribedTypes(convertListToJson(request.getSubscribedTypes()));
            } else {
                subscription.setSubscribedTypes(convertListToJson(Arrays.asList("SYSTEM", "USER", "ACTIVITY")));
            }

            subscriptionMapper.insert(subscription);
            log.info("创建用户订阅配置成功, userId: {}", userId);

        } else {
            // 更新订阅配置
            if (request.getEmailEnabled() != null) {
                subscription.setEmailEnabled(request.getEmailEnabled());
            }
            if (request.getSmsEnabled() != null) {
                subscription.setSmsEnabled(request.getSmsEnabled());
            }
            if (request.getNotificationEnabled() != null) {
                subscription.setNotificationEnabled(request.getNotificationEnabled());
            }
            if (request.getSubscribedTypes() != null) {
                subscription.setSubscribedTypes(convertListToJson(request.getSubscribedTypes()));
            }

            subscriptionMapper.updateById(subscription);
            log.info("更新用户订阅配置成功, userId: {}", userId);
        }

        return SubscriptionVO.from(subscription);
    }

    @Override
    public boolean isSubscribed(Long userId, String notificationType) {
        NotificationSubscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<NotificationSubscription>()
                .eq(NotificationSubscription::getUserId, userId)
        );

        if (subscription == null) {
            // 默认订阅 SYSTEM, USER, ACTIVITY
            List<String> defaultTypes = Arrays.asList("SYSTEM", "USER", "ACTIVITY");
            return defaultTypes.contains(notificationType);
        }

        // 检查站内通知是否启用
        if (!subscription.getNotificationEnabled()) {
            return false;
        }

        // 检查是否订阅该类型
        String subscribedTypesJson = subscription.getSubscribedTypes();
        if (subscribedTypesJson == null || subscribedTypesJson.isEmpty()) {
            return false;
        }

        List<String> subscribedTypes = convertJsonToList(subscribedTypesJson);
        return subscribedTypes.contains(notificationType);
    }

    /**
     * 将List转换为JSON字符串(用于JSONB存储)
     */
    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("转换List到JSON失败", e);
            throw new BusinessException("订阅类型格式错误");
        }
    }

    /**
     * 将JSON字符串转换为List(从JSONB读取)
     */
    private List<String> convertJsonToList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("转换JSON到List失败", e);
            return Arrays.asList();
        }
    }
}
