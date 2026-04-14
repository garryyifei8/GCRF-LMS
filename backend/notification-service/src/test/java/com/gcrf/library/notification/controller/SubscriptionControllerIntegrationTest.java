package com.gcrf.library.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.entity.NotificationSubscription;
import com.gcrf.library.notification.mapper.NotificationSubscriptionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SubscriptionController集成测试
 *
 * 测试覆盖范围：
 * - 获取用户订阅配置（首次访问返回默认值）
 * - 更新用户订阅配置
 * - 检查用户是否订阅某类型通知
 * - 验证数据持久化
 * - 验证校验规则
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SubscriptionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationSubscriptionMapper subscriptionMapper;

    private static final Long TEST_USER_ID = 9999L;
    private static final Long EXISTING_USER_ID = 9998L;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        LambdaQueryWrapper<NotificationSubscription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(NotificationSubscription::getUserId, TEST_USER_ID, EXISTING_USER_ID);
        subscriptionMapper.delete(queryWrapper);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        LambdaQueryWrapper<NotificationSubscription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(NotificationSubscription::getUserId, TEST_USER_ID, EXISTING_USER_ID);
        subscriptionMapper.delete(queryWrapper);
    }

    /**
     * 测试1：首次获取用户订阅配置，应返回默认值
     * 期望：emailEnabled/smsEnabled/notificationEnabled都为true，subscribedTypes包含SYSTEM、USER、ACTIVITY
     */
    @Test
    void getUserSubscription_whenNotExists_shouldReturnDefaults() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/subscriptions/user/{userId}", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.emailEnabled").value(true))
                .andExpect(jsonPath("$.data.smsEnabled").value(true))
                .andExpect(jsonPath("$.data.notificationEnabled").value(true))
                .andExpect(jsonPath("$.data.subscribedTypes").isNotEmpty())
                .andExpect(jsonPath("$.data.id").doesNotExist()); // 未持久化，没有ID
    }

    /**
     * 测试2：获取已存在的用户订阅配置，应返回存储的数据
     * 期望：返回数据库中保存的值，包括ID和时间戳
     */
    @Test
    void getUserSubscription_whenExists_shouldReturnStored() throws Exception {
        // Arrange: 创建一个已存在的订阅配置
        NotificationSubscription subscription = new NotificationSubscription();
        subscription.setUserId(EXISTING_USER_ID);
        subscription.setEmailEnabled(false);
        subscription.setSmsEnabled(true);
        subscription.setNotificationEnabled(true);
        subscription.setSubscribedTypes("[\"SYSTEM\",\"USER\"]");
        subscriptionMapper.insert(subscription);

        // Act & Assert
        mockMvc.perform(get("/api/v1/subscriptions/user/{userId}", EXISTING_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(EXISTING_USER_ID))
                .andExpect(jsonPath("$.data.emailEnabled").value(false))
                .andExpect(jsonPath("$.data.smsEnabled").value(true))
                .andExpect(jsonPath("$.data.notificationEnabled").value(true))
                .andExpect(jsonPath("$.data.subscribedTypes").value("[\"SYSTEM\",\"USER\"]"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    /**
     * 测试3：更新用户订阅配置，数据应成功持久化
     * 期望：返回更新后的配置，数据库中保存的值正确
     */
    @Test
    void updateSubscription_success_shouldPersist() throws Exception {
        // Arrange
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(TEST_USER_ID);
        request.setEmailEnabled(false);
        request.setSmsEnabled(true);
        request.setNotificationEnabled(true);
        request.setSubscribedTypes(Arrays.asList("SYSTEM", "ACTIVITY"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/subscriptions/user/{userId}", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.emailEnabled").value(false))
                .andExpect(jsonPath("$.data.smsEnabled").value(true))
                .andExpect(jsonPath("$.data.notificationEnabled").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty());

        // 验证数据被持久化到数据库
        NotificationSubscription persisted = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<NotificationSubscription>()
                        .eq(NotificationSubscription::getUserId, TEST_USER_ID)
        );
        assert persisted != null : "订阅配置应被保存到数据库";
        assert !persisted.getEmailEnabled() : "emailEnabled应为false";
        assert persisted.getSmsEnabled() : "smsEnabled应为true";
        assert persisted.getNotificationEnabled() : "notificationEnabled应为true";
    }

    /**
     * 测试4：使用无效数据更新订阅配置，应返回400错误
     * 期望：missing userId字段时返回400 Bad Request
     */
    @Test
    void updateSubscription_withInvalidData_shouldReturn400() throws Exception {
        // Arrange: userId为null的无效请求
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(null); // 违反@NotNull约束
        request.setEmailEnabled(false);

        // Act & Assert
        mockMvc.perform(put("/api/v1/subscriptions/user/{userId}", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 测试5：检查用户是否订阅了某类型通知（已订阅）
     * 期望：存储的订阅中包含的类型返回true
     */
    @Test
    void checkSubscription_whenSubscribed_shouldReturnTrue() throws Exception {
        // Arrange: 创建一个订阅了SYSTEM和USER的配置
        NotificationSubscription subscription = new NotificationSubscription();
        subscription.setUserId(EXISTING_USER_ID);
        subscription.setEmailEnabled(true);
        subscription.setSmsEnabled(true);
        subscription.setNotificationEnabled(true);
        subscription.setSubscribedTypes("[\"SYSTEM\",\"USER\"]");
        subscriptionMapper.insert(subscription);

        // Act & Assert: 检查订阅了的类型
        mockMvc.perform(get("/api/v1/subscriptions/check")
                        .param("userId", EXISTING_USER_ID.toString())
                        .param("notificationType", "SYSTEM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    /**
     * 测试6：检查用户是否订阅了某类型通知（未订阅）
     * 期望：未在订阅中的类型返回false，或notificationEnabled为false时返回false
     */
    @Test
    void checkSubscription_whenNotSubscribed_shouldReturnFalse() throws Exception {
        // Arrange: 创建一个只订阅SYSTEM的配置
        NotificationSubscription subscription = new NotificationSubscription();
        subscription.setUserId(EXISTING_USER_ID);
        subscription.setEmailEnabled(true);
        subscription.setSmsEnabled(true);
        subscription.setNotificationEnabled(true);
        subscription.setSubscribedTypes("[\"SYSTEM\"]");
        subscriptionMapper.insert(subscription);

        // Act & Assert: 检查未订阅的类型应返回false
        mockMvc.perform(get("/api/v1/subscriptions/check")
                        .param("userId", EXISTING_USER_ID.toString())
                        .param("notificationType", "ACTIVITY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
