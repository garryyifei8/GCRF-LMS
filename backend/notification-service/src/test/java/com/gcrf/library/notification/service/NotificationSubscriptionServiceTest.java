package com.gcrf.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;
import com.gcrf.library.notification.entity.NotificationSubscription;
import com.gcrf.library.notification.mapper.NotificationSubscriptionMapper;
import com.gcrf.library.notification.service.impl.NotificationSubscriptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationSubscriptionService单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionServiceTest {

    @Mock
    private NotificationSubscriptionMapper subscriptionMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationSubscriptionServiceImpl service;

    // ========== getUserSubscription ==========

    @Test
    @DisplayName("getUserSubscription_whenNotExists_shouldCreateDefaultAndReturn")
    void getUserSubscription_whenNotExists_shouldCreateDefaultAndReturn() {
        Long userId = 1L;
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SubscriptionVO result = service.getUserSubscription(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isTrue();
        assertThat(result.getSmsEnabled()).isTrue();
        assertThat(result.getNotificationEnabled()).isTrue();
        assertThat(result.getSubscribedTypes()).contains("SYSTEM", "USER", "ACTIVITY");
    }

    @Test
    @DisplayName("getUserSubscription_whenExists_shouldReturnVO")
    void getUserSubscription_whenExists_shouldReturnVO() {
        Long userId = 2L;
        NotificationSubscription existing = new NotificationSubscription();
        existing.setId(100L);
        existing.setUserId(userId);
        existing.setEmailEnabled(false);
        existing.setSmsEnabled(true);
        existing.setNotificationEnabled(true);
        existing.setSubscribedTypes("[\"SYSTEM\"]");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionVO result = service.getUserSubscription(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isFalse();
        assertThat(result.getSmsEnabled()).isTrue();
        assertThat(result.getNotificationEnabled()).isTrue();
        assertThat(result.getSubscribedTypes()).isEqualTo("[\"SYSTEM\"]");
    }

    // ========== updateSubscription ==========

    @Test
    @DisplayName("updateSubscription_whenNotExists_shouldCreateNewWithRequestValues")
    void updateSubscription_whenNotExists_shouldCreateNewWithRequestValues() {
        Long userId = 3L;
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(subscriptionMapper.insert(any(NotificationSubscription.class))).thenReturn(1);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setEmailEnabled(false);
        request.setSmsEnabled(false);
        request.setNotificationEnabled(true);
        request.setSubscribedTypes(List.of("SYSTEM"));

        SubscriptionVO result = service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).insert(captor.capture());
        NotificationSubscription saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEmailEnabled()).isFalse();
        assertThat(saved.getSmsEnabled()).isFalse();
        assertThat(saved.getNotificationEnabled()).isTrue();
        assertThat(saved.getSubscribedTypes()).contains("SYSTEM");
        assertThat(result).isNotNull();
        verify(subscriptionMapper, never()).updateById(any(NotificationSubscription.class));
    }

    @Test
    @DisplayName("updateSubscription_whenExists_shouldUpdateOnlyNonNullFields")
    void updateSubscription_whenExists_shouldUpdateOnlyNonNullFields() {
        Long userId = 4L;
        NotificationSubscription existing = new NotificationSubscription();
        existing.setId(10L);
        existing.setUserId(userId);
        existing.setEmailEnabled(true);
        existing.setSmsEnabled(true);
        existing.setNotificationEnabled(true);
        existing.setSubscribedTypes("[\"SYSTEM\"]");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setEmailEnabled(false);
        // smsEnabled, notificationEnabled, subscribedTypes => null

        service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        NotificationSubscription saved = captor.getValue();
        assertThat(saved.getEmailEnabled()).isFalse();
        assertThat(saved.getSmsEnabled()).isTrue(); // unchanged
        assertThat(saved.getNotificationEnabled()).isTrue(); // unchanged
        assertThat(saved.getSubscribedTypes()).isEqualTo("[\"SYSTEM\"]"); // unchanged
    }

    @Test
    @DisplayName("updateSubscription_shouldUpdateEmailEnabled")
    void updateSubscription_shouldUpdateEmailEnabled() {
        Long userId = 5L;
        NotificationSubscription existing = baseExisting(userId);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setEmailEnabled(false);

        service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getEmailEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateSubscription_shouldUpdateSmsEnabled")
    void updateSubscription_shouldUpdateSmsEnabled() {
        Long userId = 6L;
        NotificationSubscription existing = baseExisting(userId);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setSmsEnabled(false);

        service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getSmsEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateSubscription_shouldUpdateNotificationEnabled")
    void updateSubscription_shouldUpdateNotificationEnabled() {
        Long userId = 7L;
        NotificationSubscription existing = baseExisting(userId);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setNotificationEnabled(false);

        service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateSubscription_withSubscribedTypesList_shouldSerializeToJsonString")
    void updateSubscription_withSubscribedTypesList_shouldSerializeToJsonString() {
        Long userId = 8L;
        NotificationSubscription existing = baseExisting(userId);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setSubscribedTypes(List.of("SYSTEM", "USER", "ACTIVITY"));

        service.updateSubscription(userId, request);

        ArgumentCaptor<NotificationSubscription> captor =
                ArgumentCaptor.forClass(NotificationSubscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        String stored = captor.getValue().getSubscribedTypes();
        assertThat(stored).contains("SYSTEM").contains("USER").contains("ACTIVITY");
        assertThat(stored).startsWith("[").endsWith("]");
    }

    @Test
    @DisplayName("updateSubscription_shouldReturnVOReflectingSavedValues")
    void updateSubscription_shouldReturnVOReflectingSavedValues() {
        Long userId = 9L;
        NotificationSubscription existing = baseExisting(userId);
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setUserId(userId);
        request.setEmailEnabled(false);
        request.setSmsEnabled(false);

        SubscriptionVO vo = service.updateSubscription(userId, request);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isEqualTo(userId);
        assertThat(vo.getEmailEnabled()).isFalse();
        assertThat(vo.getSmsEnabled()).isFalse();
        verify(subscriptionMapper).updateById(any(NotificationSubscription.class));
    }

    // ========== isSubscribed ==========

    @Test
    @DisplayName("isSubscribed_whenNotExists_shouldReturnTrueForDefaultTypes")
    void isSubscribed_whenNotExists_shouldReturnTrueForDefaultTypes() {
        Long userId = 10L;
        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThat(service.isSubscribed(userId, "SYSTEM")).isTrue();
        assertThat(service.isSubscribed(userId, "USER")).isTrue();
        assertThat(service.isSubscribed(userId, "ACTIVITY")).isTrue();
    }

    @Test
    @DisplayName("isSubscribed_whenNotificationEnabledFalse_shouldReturnFalse")
    void isSubscribed_whenNotificationEnabledFalse_shouldReturnFalse() {
        Long userId = 11L;
        NotificationSubscription existing = baseExisting(userId);
        existing.setNotificationEnabled(false);
        existing.setSubscribedTypes("[\"SYSTEM\",\"USER\"]");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThat(service.isSubscribed(userId, "SYSTEM")).isFalse();
    }

    @Test
    @DisplayName("isSubscribed_withEmptyTypes_shouldReturnFalse")
    void isSubscribed_withEmptyTypes_shouldReturnFalse() {
        Long userId = 12L;
        NotificationSubscription existing = baseExisting(userId);
        existing.setSubscribedTypes("");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThat(service.isSubscribed(userId, "SYSTEM")).isFalse();
    }

    @Test
    @DisplayName("isSubscribed_whenTypeInList_shouldReturnTrue")
    void isSubscribed_whenTypeInList_shouldReturnTrue() {
        Long userId = 13L;
        NotificationSubscription existing = baseExisting(userId);
        existing.setSubscribedTypes("[\"SYSTEM\",\"USER\",\"ACTIVITY\"]");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThat(service.isSubscribed(userId, "USER")).isTrue();
    }

    @Test
    @DisplayName("isSubscribed_whenTypeNotInList_shouldReturnFalse")
    void isSubscribed_whenTypeNotInList_shouldReturnFalse() {
        Long userId = 14L;
        NotificationSubscription existing = baseExisting(userId);
        existing.setSubscribedTypes("[\"SYSTEM\"]");

        when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThat(service.isSubscribed(userId, "ACTIVITY")).isFalse();
    }

    // ========== helpers ==========

    private NotificationSubscription baseExisting(Long userId) {
        NotificationSubscription s = new NotificationSubscription();
        s.setId(userId);
        s.setUserId(userId);
        s.setEmailEnabled(true);
        s.setSmsEnabled(true);
        s.setNotificationEnabled(true);
        s.setSubscribedTypes("[\"SYSTEM\",\"USER\",\"ACTIVITY\"]");
        return s;
    }
}
