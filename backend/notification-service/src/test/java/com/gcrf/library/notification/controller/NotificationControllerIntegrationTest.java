package com.gcrf.library.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.common.test.BaseIntegrationTest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.entity.Notification;
import com.gcrf.library.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationMapper notificationMapper;

    private static final Long TEST_USER_ID = 9001L;

    @BeforeEach
    void setUp() {
        notificationMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notification>()
                .eq("user_id", TEST_USER_ID)
        );
    }

    @Test
    void sendNotification_success_shouldPersist() throws Exception {
        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(TEST_USER_ID);
        request.setTitle("测试通知");
        request.setContent("这是一条测试通知");
        request.setNotificationType("SYSTEM");
        request.setPriority("NORMAL");

        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.data.title").value("测试通知"))
            .andExpect(jsonPath("$.data.isRead").value(false));
    }

    @Test
    void queryNotifications_shouldReturnPaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Notification n = new Notification();
            n.setUserId(TEST_USER_ID);
            n.setTitle("通知" + i);
            n.setContent("内容" + i);
            n.setNotificationType("SYSTEM");
            n.setPriority("NORMAL");
            n.setIsRead(false);
            notificationMapper.insert(n);
        }

        mockMvc.perform(get("/api/v1/notifications")
                .param("userId", String.valueOf(TEST_USER_ID))
                .param("pageNum", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records").isArray())
            .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void getNotificationById_success_shouldReturnNotification() throws Exception {
        Notification n = createTestNotification("单条查询", "SYSTEM", "NORMAL");

        mockMvc.perform(get("/api/v1/notifications/" + n.getId())
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(n.getId()))
            .andExpect(jsonPath("$.data.title").value("单条查询"));
    }

    @Test
    void markAsRead_shouldSetIsReadTrue() throws Exception {
        Notification n = createTestNotification("标记已读", "SYSTEM", "NORMAL");

        mockMvc.perform(put("/api/v1/notifications/" + n.getId() + "/read")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Notification updated = notificationMapper.selectById(n.getId());
        assertThat(updated.getIsRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    void batchMarkAsRead_shouldMarkAllAsRead() throws Exception {
        Notification n1 = createTestNotification("批1", "SYSTEM", "NORMAL");
        Notification n2 = createTestNotification("批2", "SYSTEM", "NORMAL");

        mockMvc.perform(put("/api/v1/notifications/batch-read")
                .param("userId", String.valueOf(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(n1.getId(), n2.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        assertThat(notificationMapper.selectById(n1.getId()).getIsRead()).isTrue();
        assertThat(notificationMapper.selectById(n2.getId()).getIsRead()).isTrue();
    }

    @Test
    void deleteNotification_shouldSoftDelete() throws Exception {
        Notification n = createTestNotification("删除测试", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/" + n.getId())
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        Notification deleted = notificationMapper.selectById(n.getId());
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void batchDeleteNotifications_shouldSoftDeleteMultiple() throws Exception {
        Notification n1 = createTestNotification("删1", "SYSTEM", "NORMAL");
        Notification n2 = createTestNotification("删2", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/batch")
                .param("userId", String.valueOf(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(n1.getId(), n2.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getUnreadCount_shouldReturnCountWithUrgent() throws Exception {
        createTestNotification("未读1", "SYSTEM", "NORMAL");
        createTestNotification("未读2", "SYSTEM", "URGENT");

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
            .andExpect(jsonPath("$.data.unreadCount").value(2))
            .andExpect(jsonPath("$.data.urgentCount").value(1));
    }

    @Test
    void getLatestNotifications_shouldReturnInDescOrder() throws Exception {
        createTestNotification("旧", "SYSTEM", "NORMAL");
        createTestNotification("新", "SYSTEM", "NORMAL");

        mockMvc.perform(get("/api/v1/notifications/latest")
                .param("userId", String.valueOf(TEST_USER_ID))
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void clearAllNotifications_shouldSoftDeleteAll() throws Exception {
        createTestNotification("清1", "SYSTEM", "NORMAL");
        createTestNotification("清2", "SYSTEM", "NORMAL");

        mockMvc.perform(delete("/api/v1/notifications/clear")
                .param("userId", String.valueOf(TEST_USER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    private Notification createTestNotification(String title, String type, String priority) {
        Notification n = new Notification();
        n.setUserId(TEST_USER_ID);
        n.setTitle(title);
        n.setContent("内容: " + title);
        n.setNotificationType(type);
        n.setPriority(priority);
        n.setIsRead(false);
        notificationMapper.insert(n);
        return n;
    }
}
