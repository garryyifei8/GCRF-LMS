package com.gcrf.library.notification.service;

import com.gcrf.library.notification.dto.request.NotificationPushRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.service.impl.WebSocketNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketNotificationServiceImpl单元测试
 *
 * @author GCRF Team
 * @since 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpUserRegistry userRegistry;

    @InjectMocks
    private WebSocketNotificationServiceImpl webSocketService;

    private NotificationVO notification;

    @BeforeEach
    void setUp() {
        notification = new NotificationVO();
        notification.setId(1L);
        notification.setTitle("测试通知");
        notification.setContent("这是一条测试通知");
        notification.setNotificationType("SYSTEM");
        notification.setPriority("MEDIUM");
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testPushToUser_UserOnline() {
        // Arrange
        Long userId = 1L;
        SimpUser user = mock(SimpUser.class);
        when(userRegistry.getUser(userId.toString())).thenReturn(user);
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Act
        webSocketService.pushToUser(userId, notification);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/notifications"),
                eq(notification)
        );
    }

    @Test
    void testPushToUser_UserOffline() {
        // Arrange
        Long userId = 1L;
        when(userRegistry.getUser(userId.toString())).thenReturn(null);

        // Act
        webSocketService.pushToUser(userId, notification);

        // Assert
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void testBroadcastToAll_Success() {
        // Arrange
        when(userRegistry.getUserCount()).thenReturn(10);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        // Act
        webSocketService.broadcastToAll(notification);

        // Assert
        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications"),
                (Object) eq(notification)
        );
    }

    @Test
    void testPushToUsers_Success() {
        // Arrange
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        SimpUser user = mock(SimpUser.class);
        when(userRegistry.getUser(anyString())).thenReturn(user);
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Act
        webSocketService.pushToUsers(userIds, notification);

        // Assert
        verify(messagingTemplate, times(3)).convertAndSendToUser(
                anyString(),
                eq("/queue/notifications"),
                eq(notification)
        );
    }

    @Test
    void testPushToTopic_Success() {
        // Arrange
        String topic = "announcements";
        doNothing().when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        // Act
        webSocketService.pushToTopic(topic, notification);

        // Assert
        verify(messagingTemplate).convertAndSend(
                eq("/topic/" + topic),
                (Object) eq(notification)
        );
    }

    @Test
    void testPushAsync_UserTarget() {
        // Arrange
        NotificationPushRequest request = new NotificationPushRequest();
        request.setTargetType("USER");
        request.setTargetIds(Arrays.asList(1L, 2L));
        request.setTitle("测试通知");
        request.setContent("测试内容");
        request.setNotificationType("SYSTEM");
        request.setPriority("HIGH");

        SimpUser user = mock(SimpUser.class);
        when(userRegistry.getUser(anyString())).thenReturn(user);
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Act
        webSocketService.pushAsync(request);

        // Assert
        // 由于是异步,可能需要等待或使用其他方式验证
        verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                anyString(),
                eq("/queue/notifications"),
                any(NotificationVO.class)
        );
    }

    @Test
    void testPushAsync_AllTarget() {
        // Arrange
        NotificationPushRequest request = new NotificationPushRequest();
        request.setTargetType("ALL");
        request.setTitle("广播通知");
        request.setContent("这是一条广播通知");
        request.setNotificationType("ANNOUNCEMENT");
        request.setPriority("HIGH");

        when(userRegistry.getUserCount()).thenReturn(10);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        // Act
        webSocketService.pushAsync(request);

        // Assert
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/notifications"),
                (Object) any(NotificationVO.class)
        );
    }

    @Test
    void testPushAsync_TopicTarget() {
        // Arrange
        NotificationPushRequest request = new NotificationPushRequest();
        request.setTargetType("TOPIC");
        request.setTopic("announcements");
        request.setTitle("主题通知");
        request.setContent("主题内容");
        request.setNotificationType("BUSINESS");
        request.setPriority("MEDIUM");

        doNothing().when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        // Act
        webSocketService.pushAsync(request);

        // Assert
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/announcements"),
                (Object) any(NotificationVO.class)
        );
    }

    @Test
    void testIsUserOnline_Online() {
        // Arrange
        Long userId = 1L;
        SimpUser user = mock(SimpUser.class);
        when(userRegistry.getUser(userId.toString())).thenReturn(user);

        // Act
        boolean result = webSocketService.isUserOnline(userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsUserOnline_Offline() {
        // Arrange
        Long userId = 1L;
        when(userRegistry.getUser(userId.toString())).thenReturn(null);

        // Act
        boolean result = webSocketService.isUserOnline(userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetOnlineUserCount() {
        // Arrange
        when(userRegistry.getUserCount()).thenReturn(15);

        // Act
        long count = webSocketService.getOnlineUserCount();

        // Assert
        assertEquals(15, count);
    }

    @Test
    void testUserConnected() {
        // Arrange
        Long userId = 1L;
        String sessionId = "session123";

        // Act
        webSocketService.userConnected(userId, sessionId);

        // Assert
        assertTrue(webSocketService.isUserOnline(userId));
    }

    @Test
    void testUserDisconnected() {
        // Arrange
        Long userId = 1L;
        String sessionId = "session123";
        webSocketService.userConnected(userId, sessionId);

        // Act
        webSocketService.userDisconnected(userId);

        // Assert - 需要通过userRegistry检查,因为缓存已清除
        when(userRegistry.getUser(userId.toString())).thenReturn(null);
        assertFalse(webSocketService.isUserOnline(userId));
    }
}
