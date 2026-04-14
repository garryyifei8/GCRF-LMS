package com.gcrf.library.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.NotificationCreateRequest;
import com.gcrf.library.notification.dto.request.NotificationPushRequest;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.entity.Notification;
import com.gcrf.library.notification.mapper.NotificationMapper;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationController集成测试
 *
 * 测试覆盖范围：
 * - 创建通知
 * - 推送通知
 * - 查询通知（分页、条件过滤）
 * - 获取通知详情
 * - 更新通知
 * - 删除通知
 * - 标记为已读
 * - 批量操作
 *
 * @author GCRF Team
 * @since 2025-10-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getTitle, "集成测试通知");
        notificationMapper.delete(queryWrapper);

        // 创建测试通知
        testNotification = new Notification();
        testNotification.setTitle("集成测试通知");
        testNotification.setContent("这是一条集成测试通知内容");
        testNotification.setNotificationType("SYSTEM");
        testNotification.setPriority("MEDIUM");
        testNotification.setTargetType("USER");
        testNotification.setTargetId(1L);
        testNotification.setStatus("PENDING");
        testNotification.setReadStatus(false);
        testNotification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(testNotification);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testNotification != null && testNotification.getId() != null) {
            notificationMapper.deleteById(testNotification.getId());
        }
    }

    @Test
    void testCreateNotification_Success() throws Exception {
        // Arrange
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle("新通知");
        request.setContent("新通知内容");
        request.setNotificationType("BUSINESS");
        request.setPriority("HIGH");
        request.setTargetType("USER");
        request.setTargetId(2L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("新通知"))
                .andExpect(jsonPath("$.data.content").value("新通知内容"))
                .andExpect(jsonPath("$.data.notificationType").value("BUSINESS"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void testCreateNotification_InvalidRequest() throws Exception {
        // Arrange
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle(""); // 空标题
        request.setContent("内容");
        request.setNotificationType("SYSTEM");

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPushNotification_ToUser() throws Exception {
        // Arrange
        NotificationPushRequest request = new NotificationPushRequest();
        request.setTitle("推送通知");
        request.setContent("推送内容");
        request.setNotificationType("SYSTEM");
        request.setPriority("HIGH");
        request.setTargetType("USER");
        request.setTargetIds(Arrays.asList(1L, 2L, 3L));
        request.setChannels("WEBSOCKET,EMAIL");

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testPushNotification_ToAll() throws Exception {
        // Arrange
        NotificationPushRequest request = new NotificationPushRequest();
        request.setTitle("广播通知");
        request.setContent("广播内容");
        request.setNotificationType("ANNOUNCEMENT");
        request.setPriority("HIGH");
        request.setTargetType("ALL");
        request.setChannels("WEBSOCKET");

        // Act & Assert
        mockMvc.perform(post("/api/v1/notifications/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testQueryNotifications_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("notificationType", "SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void testQueryNotifications_WithFilters() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("targetType", "USER")
                        .param("targetId", "1")
                        .param("readStatus", "false")
                        .param("priority", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetNotificationById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/{id}", testNotification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(testNotification.getId()))
                .andExpect(jsonPath("$.data.title").value("集成测试通知"))
                .andExpect(jsonPath("$.data.content").value("这是一条集成测试通知内容"));
    }

    @Test
    void testGetNotificationById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testUpdateNotification_Success() throws Exception {
        // Arrange
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle("更新后的标题");
        request.setContent("更新后的内容");
        request.setNotificationType("SYSTEM");
        request.setPriority("HIGH");
        request.setTargetType("USER");
        request.setTargetId(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/{id}", testNotification.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("更新后的标题"))
                .andExpect(jsonPath("$.data.content").value("更新后的内容"));
    }

    @Test
    void testUpdateNotification_NotFound() throws Exception {
        // Arrange
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle("更新标题");
        request.setContent("更新内容");
        request.setNotificationType("SYSTEM");
        request.setPriority("MEDIUM");
        request.setTargetType("USER");
        request.setTargetId(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testDeleteNotification_Success() throws Exception {
        // Arrange - 创建一个用于删除的通知
        Notification toDelete = new Notification();
        toDelete.setTitle("待删除通知");
        toDelete.setContent("内容");
        toDelete.setNotificationType("SYSTEM");
        toDelete.setPriority("LOW");
        toDelete.setTargetType("USER");
        toDelete.setTargetId(1L);
        toDelete.setStatus("PENDING");
        toDelete.setReadStatus(false);
        toDelete.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(toDelete);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notifications/{id}", toDelete.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify
        Notification deleted = notificationMapper.selectById(toDelete.getId());
        assert deleted == null; // 应该被删除
    }

    @Test
    void testDeleteNotification_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/notifications/{id}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testMarkAsRead_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/{id}/read", testNotification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify
        Notification updated = notificationMapper.selectById(testNotification.getId());
        assert updated.getReadStatus() == true;
        assert updated.getReadAt() != null;
    }

    @Test
    void testMarkAsRead_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/{id}/read", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void testBatchMarkAsRead_Success() throws Exception {
        // Arrange - 创建多个未读通知
        Notification notif1 = createTestNotification("通知1", 1L);
        Notification notif2 = createTestNotification("通知2", 1L);
        Notification notif3 = createTestNotification("通知3", 1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/batch-read")
                        .param("ids", notif1.getId().toString(), notif2.getId().toString(), notif3.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Cleanup
        notificationMapper.deleteById(notif1.getId());
        notificationMapper.deleteById(notif2.getId());
        notificationMapper.deleteById(notif3.getId());
    }

    @Test
    void testBatchDelete_Success() throws Exception {
        // Arrange - 创建多个待删除通知
        Notification notif1 = createTestNotification("待删除1", 1L);
        Notification notif2 = createTestNotification("待删除2", 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notifications/batch")
                        .param("ids", notif1.getId().toString(), notif2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify - 通知应该被删除
        assert notificationMapper.selectById(notif1.getId()) == null;
        assert notificationMapper.selectById(notif2.getId()) == null;
    }

    @Test
    void testGetUnreadCount_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    /**
     * 辅助方法: 创建测试通知
     */
    private Notification createTestNotification(String title, Long targetId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent("测试内容");
        notification.setNotificationType("SYSTEM");
        notification.setPriority("MEDIUM");
        notification.setTargetType("USER");
        notification.setTargetId(targetId);
        notification.setStatus("PENDING");
        notification.setReadStatus(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }
}
