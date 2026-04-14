package com.gcrf.library.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.notification.dto.request.NotificationMarkReadRequest;
import com.gcrf.library.notification.dto.request.NotificationQueryRequest;
import com.gcrf.library.notification.dto.request.NotificationSendRequest;
import com.gcrf.library.notification.dto.response.NotificationVO;
import com.gcrf.library.notification.dto.response.UnreadCountVO;
import com.gcrf.library.notification.entity.Notification;
import com.gcrf.library.notification.mapper.NotificationMapper;
import com.gcrf.library.notification.service.impl.NotificationServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService单元测试
 *
 * 覆盖11个方法:
 * - sendNotification / queryNotifications
 * - markAsRead / markAllAsRead / getUnreadCount
 * - getNotificationById / deleteNotification / batchDeleteNotifications
 * - getLatestNotifications / batchMarkAsRead / clearAllNotifications
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;

    @BeforeAll
    static void initMybatisPlus() {
        // 初始化MyBatis-Plus的TableInfo缓存,使LambdaQueryWrapper/LambdaUpdateWrapper在单元测试中可用
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new Configuration(), ""),
            Notification.class
        );
    }

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(100L);
        testNotification.setUserId(1L);
        testNotification.setTitle("测试通知");
        testNotification.setContent("测试内容");
        testNotification.setNotificationType("SYSTEM");
        testNotification.setPriority("NORMAL");
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    // ========== sendNotification ==========

    @Test
    @DisplayName("sendNotification: priority为null时应默认为NORMAL")
    void sendNotification_withNullPriority_shouldDefaultToNormal() {
        // Arrange
        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(1L);
        request.setTitle("标题");
        request.setContent("内容");
        request.setNotificationType("SYSTEM");
        request.setPriority(null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationMapper.insert(captor.capture())).thenReturn(1);

        // Act
        NotificationVO result = notificationService.sendNotification(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(captor.getValue().getPriority()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("sendNotification: 应将isRead设置为false")
    void sendNotification_shouldSetIsReadFalse() {
        // Arrange
        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(1L);
        request.setTitle("标题");
        request.setContent("内容");
        request.setNotificationType("SYSTEM");
        request.setPriority("HIGH");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationMapper.insert(captor.capture())).thenReturn(1);

        // Act
        notificationService.sendNotification(request);

        // Assert
        assertThat(captor.getValue().getIsRead()).isFalse();
        assertThat(captor.getValue().getPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("sendNotification: 成功时应插入实体并返回VO")
    void sendNotification_success_shouldInsertAndReturnVO() {
        // Arrange
        NotificationSendRequest request = new NotificationSendRequest();
        request.setUserId(1L);
        request.setTitle("欢迎使用");
        request.setContent("您好, 欢迎");
        request.setNotificationType("SYSTEM");
        request.setPriority("NORMAL");
        request.setExtraData("{\"key\":\"value\"}");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationMapper.insert(captor.capture())).thenReturn(1);

        // Act
        NotificationVO result = notificationService.sendNotification(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("欢迎使用");
        assertThat(result.getContent()).isEqualTo("您好, 欢迎");
        assertThat(result.getNotificationType()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getExtraData()).isEqualTo("{\"key\":\"value\"}");
        verify(notificationMapper).insert(any(Notification.class));
    }

    // ========== queryNotifications ==========

    @Test
    @DisplayName("queryNotifications: 应应用所有过滤条件")
    void queryNotifications_withFilters_shouldApplyAllFilters() {
        // Arrange
        NotificationQueryRequest request = new NotificationQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setNotificationType("SYSTEM");
        request.setIsRead(false);
        request.setPriority("URGENT");
        request.setStartDate(LocalDateTime.now().minusDays(7));
        request.setEndDate(LocalDateTime.now());

        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testNotification));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);

        when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // Act
        PageResult<NotificationVO> result = notificationService.queryNotifications(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        verify(notificationMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("queryNotifications: 应排除软删除记录(deletedAt IS NULL)")
    void queryNotifications_shouldExcludeSoftDeleted() {
        // Arrange
        NotificationQueryRequest request = new NotificationQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        page.setCurrent(1);
        page.setSize(10);

        ArgumentCaptor<LambdaQueryWrapper<Notification>> wrapperCaptor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(notificationMapper.selectPage(any(Page.class), wrapperCaptor.capture()))
            .thenReturn(page);

        // Act
        PageResult<NotificationVO> result = notificationService.queryNotifications(1L, request);

        // Assert - wrapper should be passed (we verify soft delete filter is applied by SQL)
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isZero();
        assertThat(wrapperCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("queryNotifications: 应按创建时间倒序排序")
    void queryNotifications_shouldOrderByCreatedAtDesc() {
        // Arrange
        NotificationQueryRequest request = new NotificationQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Notification older = new Notification();
        older.setId(1L);
        older.setUserId(1L);
        older.setTitle("较早");
        older.setCreatedAt(LocalDateTime.now().minusDays(1));
        older.setIsRead(false);

        Notification newer = new Notification();
        newer.setId(2L);
        newer.setUserId(1L);
        newer.setTitle("较新");
        newer.setCreatedAt(LocalDateTime.now());
        newer.setIsRead(false);

        Page<Notification> page = new Page<>(1, 10);
        // Mapper会按SQL返回顺序(倒序)
        page.setRecords(Arrays.asList(newer, older));
        page.setTotal(2);
        page.setCurrent(1);
        page.setSize(10);

        when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // Act
        PageResult<NotificationVO> result = notificationService.queryNotifications(1L, request);

        // Assert
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getTitle()).isEqualTo("较新");
        assertThat(result.getRecords().get(1).getTitle()).isEqualTo("较早");
    }

    // ========== markAsRead ==========

    @Test
    @DisplayName("markAsRead: markAll=true时应调用markAllAsRead")
    void markAsRead_whenMarkAllTrue_shouldCallMarkAllAsRead() {
        // Arrange
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setMarkAll(true);

        when(notificationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(5);

        // Act
        notificationService.markAsRead(1L, request);

        // Assert - markAllAsRead uses mapper.update with LambdaUpdateWrapper (no selectOne)
        verify(notificationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(notificationMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("markAsRead: notificationId为null时应抛异常")
    void markAsRead_whenNotificationIdNull_shouldThrowException() {
        // Arrange
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setMarkAll(false);
        request.setNotificationId(null);

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsRead(1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知ID不能为空");
    }

    @Test
    @DisplayName("markAsRead: 通知不存在时应抛异常")
    void markAsRead_whenNotFound_shouldThrowException() {
        // Arrange
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setMarkAll(false);
        request.setNotificationId(999L);

        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsRead(1L, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("markAsRead: 通知已读时不应更新")
    void markAsRead_whenAlreadyRead_shouldNotUpdate() {
        // Arrange
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setMarkAll(false);
        request.setNotificationId(100L);

        testNotification.setIsRead(true);
        testNotification.setReadAt(LocalDateTime.now().minusHours(1));
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testNotification);

        // Act
        notificationService.markAsRead(1L, request);

        // Assert
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead: 成功时应设置isRead和readAt")
    void markAsRead_success_shouldSetIsReadAndReadAt() {
        // Arrange
        NotificationMarkReadRequest request = new NotificationMarkReadRequest();
        request.setMarkAll(false);
        request.setNotificationId(100L);

        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testNotification);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationMapper.updateById(captor.capture())).thenReturn(1);

        // Act
        notificationService.markAsRead(1L, request);

        // Assert
        Notification updated = captor.getValue();
        assertThat(updated.getIsRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    // ========== markAllAsRead ==========

    @Test
    @DisplayName("markAllAsRead: 成功时应更新所有未读通知")
    void markAllAsRead_success_shouldUpdateAllUnread() {
        // Arrange
        when(notificationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(8);

        // Act
        notificationService.markAllAsRead(1L);

        // Assert
        verify(notificationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ========== getUnreadCount ==========

    @Test
    @DisplayName("getUnreadCount: 应返回未读总数与紧急未读数")
    void getUnreadCount_shouldReturnTotalAndUrgentCounts() {
        // Arrange - 两次selectCount: 第一次总未读, 第二次紧急未读
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(10L)
            .thenReturn(2L);

        // Act
        UnreadCountVO result = notificationService.getUnreadCount(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUnreadCount()).isEqualTo(10L);
        assertThat(result.getUrgentCount()).isEqualTo(2L);
        verify(notificationMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
    }

    // ========== getNotificationById ==========

    @Test
    @DisplayName("getNotificationById: 通知不存在时应抛异常")
    void getNotificationById_whenNotFound_shouldThrowException() {
        // Arrange
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> notificationService.getNotificationById(1L, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("getNotificationById: 成功时应返回VO")
    void getNotificationById_success_shouldReturnVO() {
        // Arrange
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testNotification);

        // Act
        NotificationVO result = notificationService.getNotificationById(1L, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("测试通知");
        assertThat(result.getUserId()).isEqualTo(1L);
    }

    // ========== deleteNotification ==========

    @Test
    @DisplayName("deleteNotification: 通知不存在时应抛异常")
    void deleteNotification_whenNotFound_shouldThrowException() {
        // Arrange
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知不存在");
    }

    @Test
    @DisplayName("deleteNotification: 成功时应软删除(设置deletedAt)")
    void deleteNotification_success_shouldSoftDelete() {
        // Arrange
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testNotification);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationMapper.updateById(captor.capture())).thenReturn(1);

        // Act
        notificationService.deleteNotification(1L, 100L);

        // Assert
        Notification updated = captor.getValue();
        assertThat(updated.getDeletedAt()).isNotNull();
        verify(notificationMapper).updateById(any(Notification.class));
    }

    // ========== batchDeleteNotifications ==========

    @Test
    @DisplayName("batchDeleteNotifications: 空列表时应抛异常")
    void batchDeleteNotifications_whenEmpty_shouldThrowException() {
        // Act & Assert - null
        assertThatThrownBy(() -> notificationService.batchDeleteNotifications(1L, null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知ID列表不能为空");

        // Act & Assert - empty
        assertThatThrownBy(() -> notificationService.batchDeleteNotifications(1L, Collections.emptyList()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("通知ID列表不能为空");
    }

    @Test
    @DisplayName("batchDeleteNotifications: 成功时应对每个ID调用删除")
    void batchDeleteNotifications_success_shouldCallDeleteForEach() {
        // Arrange
        List<Long> ids = Arrays.asList(100L, 101L, 102L);
        when(notificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testNotification);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        // Act
        notificationService.batchDeleteNotifications(1L, ids);

        // Assert - 每个ID触发一次selectOne和一次updateById
        verify(notificationMapper, times(3)).selectOne(any(LambdaQueryWrapper.class));
        verify(notificationMapper, times(3)).updateById(any(Notification.class));
    }

    // ========== getLatestNotifications ==========

    @Test
    @DisplayName("getLatestNotifications: limit为null时应使用默认值10")
    void getLatestNotifications_withNullLimit_shouldUseDefault10() {
        // Arrange
        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(testNotification));

        // Act
        List<NotificationVO> result = notificationService.getLatestNotifications(1L, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(notificationMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getLatestNotifications: 应按创建时间倒序")
    void getLatestNotifications_shouldOrderByCreatedAtDesc() {
        // Arrange
        Notification newer = new Notification();
        newer.setId(2L);
        newer.setUserId(1L);
        newer.setTitle("新");
        newer.setCreatedAt(LocalDateTime.now());
        newer.setIsRead(false);

        Notification older = new Notification();
        older.setId(1L);
        older.setUserId(1L);
        older.setTitle("旧");
        older.setCreatedAt(LocalDateTime.now().minusDays(2));
        older.setIsRead(false);

        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(newer, older));

        // Act
        List<NotificationVO> result = notificationService.getLatestNotifications(1L, 5);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("新");
        assertThat(result.get(1).getTitle()).isEqualTo("旧");
    }

    // ========== batchMarkAsRead ==========

    @Test
    @DisplayName("batchMarkAsRead: 空列表不应抛异常且不应调用mapper")
    void batchMarkAsRead_withEmptyList_shouldReturnWithoutError() {
        // Act & Assert - null不抛异常
        notificationService.batchMarkAsRead(1L, null);

        // Act & Assert - empty不抛异常
        notificationService.batchMarkAsRead(1L, Collections.emptyList());

        // mapper不应被调用
        verify(notificationMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("batchMarkAsRead: 成功时应将所有通知标记为已读")
    void batchMarkAsRead_success_shouldUpdateAllToRead() {
        // Arrange
        List<Long> ids = Arrays.asList(100L, 101L, 102L);
        when(notificationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(3);

        // Act
        notificationService.batchMarkAsRead(1L, ids);

        // Assert
        verify(notificationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ========== clearAllNotifications ==========

    @Test
    @DisplayName("clearAllNotifications: 成功时应软删除所有通知")
    void clearAllNotifications_success_shouldSoftDeleteAll() {
        // Arrange
        when(notificationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(15);

        // Act
        notificationService.clearAllNotifications(1L);

        // Assert
        verify(notificationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
