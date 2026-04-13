# 核心服务缺口修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 5 specific gaps across notification-service, system-service, reader-service, and web-admin to bring all core services to production-ready state.

**Architecture:** Targeted fixes to existing code — no new services or major refactoring. Each task is independent and produces a compilable, testable result.

**Tech Stack:** Spring Boot 3.2.2, MyBatis-Plus 3.5.9, Spring Cloud OpenFeign, Vue 3, Java 21

**Spec:** `docs/specs/2026-04-13-core-services-gap-fix-design.md`

---

## File Map

### New Files

| File                                                                                                                             | Responsibility                              |
| -------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| `backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationSubscriptionServiceImpl.java` | Subscription CRUD implementation            |
| `backend/system-service/src/main/java/com/gcrf/library/system/client/AuthServiceClient.java`                                     | Feign client to auth-service for user roles |

### Modified Files

| File                                                                         | Change                                              |
| ---------------------------------------------------------------------------- | --------------------------------------------------- |
| `backend/notification-service/.../controller/NotificationController.java`    | Rewrite to match service interface signatures       |
| `backend/notification-service/.../service/NotificationService.java`          | Add 3 missing method declarations                   |
| `backend/notification-service/.../service/impl/NotificationServiceImpl.java` | Implement 3 new methods                             |
| `backend/notification-service/.../controller/SubscriptionController.java`    | Rewrite to use NotificationSubscriptionService      |
| `backend/system-service/.../controller/DepartmentController.java`            | Change @RequestMapping path                         |
| `backend/system-service/.../controller/RoleController.java`                  | Change @RequestMapping path                         |
| `backend/system-service/.../controller/PermissionController.java`            | Change @RequestMapping path                         |
| `backend/system-service/.../controller/MenuController.java`                  | Change @RequestMapping path                         |
| `backend/system-service/.../controller/LoginLogController.java`              | Change @RequestMapping path                         |
| `backend/system-service/.../controller/OperationLogController.java`          | Change @RequestMapping path                         |
| `backend/system-service/.../service/impl/MenuServiceImpl.java`               | Fix getUserMenus with Feign + role filtering        |
| `backend/reader-service/.../service/impl/ReaderServiceImpl.java`             | Add borrow count check in deleteReader + cancelCard |
| `web-admin/src/api/circulation.js`                                           | Add processReservation + notifyReservation exports  |

---

## Task 1: Fix frontend build error — add missing circulation API exports

**Files:**

- Modify: `web-admin/src/api/circulation.js`

This is the quickest win and unblocks frontend development.

- [ ] **Step 1: Add `processReservation` and `notifyReservation` to circulation.js**

At the end of `web-admin/src/api/circulation.js` (after the `getOverdueBorrows` function at line 185), add:

```javascript
/**
 * 处理预约（确认取书）
 * @param {number} reservationId - 预约ID
 */
export function processReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/pickup`,
    method: "post",
  });
}

/**
 * 发送预约通知
 * @param {number} reservationId - 预约ID
 */
export function notifyReservation(reservationId) {
  return request({
    url: `/api/v1/reserves/${reservationId}/notify`,
    method: "post",
  });
}
```

- [ ] **Step 2: Verify frontend build passes**

Run:

```bash
cd web-admin && npm run build
```

Expected: Build succeeds (or fails on a different, pre-existing issue — not the `processReservation` import error).

- [ ] **Step 3: Commit**

```bash
git add web-admin/src/api/circulation.js
git commit -m "fix(web-admin): add missing processReservation and notifyReservation API exports

- processReservation calls POST /api/v1/reserves/{id}/pickup
- notifyReservation calls POST /api/v1/reserves/{id}/notify
- Fixes build error in reservations.vue"
```

---

## Task 2: Fix system-service route paths

**Files:**

- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/DepartmentController.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/RoleController.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/PermissionController.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/MenuController.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/LoginLogController.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/controller/OperationLogController.java`

- [ ] **Step 1: Update DepartmentController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/DepartmentController.java`, change line 20:

From: `@RequestMapping("/api/departments")`
To: `@RequestMapping("/api/v1/system/departments")`

- [ ] **Step 2: Update RoleController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/RoleController.java`, change line 30:

From: `@RequestMapping("/api/v1/roles")`
To: `@RequestMapping("/api/v1/system/roles")`

- [ ] **Step 3: Update PermissionController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/PermissionController.java`, change line 27:

From: `@RequestMapping("/api/v1/permissions")`
To: `@RequestMapping("/api/v1/system/permissions")`

- [ ] **Step 4: Update MenuController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/MenuController.java`, change line 26:

From: `@RequestMapping("/api/v1/menus")`
To: `@RequestMapping("/api/v1/system/menus")`

- [ ] **Step 5: Update LoginLogController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/LoginLogController.java`, change line 23:

From: `@RequestMapping("/api/v1/login-logs")`
To: `@RequestMapping("/api/v1/system/login-logs")`

- [ ] **Step 6: Update OperationLogController path**

In `backend/system-service/src/main/java/com/gcrf/library/system/controller/OperationLogController.java`, change line 23:

From: `@RequestMapping("/api/v1/operation-logs")`
To: `@RequestMapping("/api/v1/system/operation-logs")`

- [ ] **Step 7: Update test files to match new paths**

Search all test files in `backend/system-service/src/test/` for the old URL paths and update them. For example, in integration tests that use `mockMvc.perform(get("/api/v1/roles"))`, change to `mockMvc.perform(get("/api/v1/system/roles"))`.

Use grep to find all occurrences:

```bash
cd backend/system-service && grep -rn '"/api/v1/roles\|"/api/v1/permissions\|"/api/v1/menus\|"/api/v1/login-logs\|"/api/v1/operation-logs\|"/api/departments' src/test/
```

Update each occurrence to include the `/system` prefix.

- [ ] **Step 8: Compile and run tests**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile -pl system-service -am
cd backend && mvn test -pl system-service
```

Expected: Compilation succeeds, tests pass with updated paths.

- [ ] **Step 9: Commit**

```bash
git add backend/system-service/
git commit -m "fix(system): align controller route paths with frontend API calls

- DepartmentController: /api/departments -> /api/v1/system/departments
- RoleController: /api/v1/roles -> /api/v1/system/roles
- PermissionController: /api/v1/permissions -> /api/v1/system/permissions
- MenuController: /api/v1/menus -> /api/v1/system/menus
- LoginLogController: /api/v1/login-logs -> /api/v1/system/login-logs
- OperationLogController: /api/v1/operation-logs -> /api/v1/system/operation-logs
- Updated all integration test URLs accordingly"
```

---

## Task 3: Fix NotificationController signatures + add missing service methods

**Files:**

- Modify: `backend/notification-service/src/main/java/com/gcrf/library/notification/service/NotificationService.java`
- Modify: `backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationServiceImpl.java`
- Modify: `backend/notification-service/src/main/java/com/gcrf/library/notification/controller/NotificationController.java`

- [ ] **Step 1: Add 3 missing methods to NotificationService interface**

In `backend/notification-service/src/main/java/com/gcrf/library/notification/service/NotificationService.java`, add before the closing `}`:

```java
    /**
     * 获取用户最新通知
     */
    List<NotificationVO> getLatestNotifications(Long userId, Integer limit);

    /**
     * 批量标记通知为已读
     */
    void batchMarkAsRead(Long userId, List<Long> notificationIds);

    /**
     * 清空用户所有通知（软删除）
     */
    void clearAllNotifications(Long userId);
```

Also add `import java.util.List;` to the imports if not already present.

- [ ] **Step 2: Implement the 3 new methods in NotificationServiceImpl**

In `backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationServiceImpl.java`, add before the closing `}`:

```java
    @Override
    public List<NotificationVO> getLatestNotifications(Long userId, Integer limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .isNull(Notification::getDeletedAt)
               .orderByDesc(Notification::getCreatedAt)
               .last("LIMIT " + (limit != null ? limit : 10));

        List<Notification> notifications = notificationMapper.selectList(wrapper);
        return notifications.stream()
                .map(NotificationVO::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .in(Notification::getId, notificationIds)
               .eq(Notification::getIsRead, false)
               .isNull(Notification::getDeletedAt)
               .set(Notification::getIsRead, true)
               .set(Notification::getReadAt, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
        log.info("批量标记通知已读, userId: {}, count: {}", userId, notificationIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllNotifications(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .isNull(Notification::getDeletedAt)
               .set(Notification::getDeletedAt, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
        log.info("清空用户所有通知, userId: {}", userId);
    }
```

- [ ] **Step 3: Rewrite NotificationController to align with service interface**

Replace the ENTIRE content of `backend/notification-service/src/main/java/com/gcrf/library/notification/controller/NotificationController.java` with:

```java
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
```

- [ ] **Step 4: Compile notification-service**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile -pl notification-service -am
```

Expected: Compilation succeeds.

- [ ] **Step 5: Run tests**

Run:

```bash
cd backend && mvn test -pl notification-service
```

Expected: Existing tests may need URL/signature updates. Fix any failures related to changed method signatures.

- [ ] **Step 6: Commit**

```bash
git add backend/notification-service/
git commit -m "fix(notification): align NotificationController with service interface

- Added getLatestNotifications, batchMarkAsRead, clearAllNotifications to service
- Rewrote controller: all methods now pass userId parameter
- getUnreadCount returns UnreadCountVO instead of Long
- markAsRead uses NotificationMarkReadRequest"
```

---

## Task 4: Implement NotificationSubscriptionService + fix SubscriptionController

**Files:**

- Create: `backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationSubscriptionServiceImpl.java`
- Modify: `backend/notification-service/src/main/java/com/gcrf/library/notification/controller/SubscriptionController.java`

- [ ] **Step 1: Create NotificationSubscriptionServiceImpl**

Create `backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationSubscriptionServiceImpl.java`:

```java
package com.gcrf.library.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSubscriptionServiceImpl implements NotificationSubscriptionService {

    private final NotificationSubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionVO getUserSubscription(Long userId) {
        NotificationSubscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<NotificationSubscription>()
                .eq(NotificationSubscription::getUserId, userId)
        );

        if (subscription == null) {
            // Return default subscription (all enabled)
            subscription = createDefaultSubscription(userId);
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
            subscription = createDefaultSubscription(userId);
        }

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
            subscription.setSubscribedTypes(String.join(",", request.getSubscribedTypes()));
        }
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionMapper.updateById(subscription);
        log.info("更新用户订阅配置, userId: {}", userId);

        return SubscriptionVO.from(subscription);
    }

    @Override
    public boolean isSubscribed(Long userId, String notificationType) {
        NotificationSubscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<NotificationSubscription>()
                .eq(NotificationSubscription::getUserId, userId)
        );

        if (subscription == null) {
            return true; // Default: all subscribed
        }

        if (!Boolean.TRUE.equals(subscription.getNotificationEnabled())) {
            return false;
        }

        String subscribedTypes = subscription.getSubscribedTypes();
        if (subscribedTypes == null || subscribedTypes.isEmpty()) {
            return true; // Empty means all types
        }

        return subscribedTypes.contains(notificationType);
    }

    private NotificationSubscription createDefaultSubscription(Long userId) {
        NotificationSubscription subscription = new NotificationSubscription();
        subscription.setUserId(userId);
        subscription.setEmailEnabled(true);
        subscription.setSmsEnabled(true);
        subscription.setNotificationEnabled(true);
        subscription.setSubscribedTypes("");
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.insert(subscription);
        return subscription;
    }
}
```

- [ ] **Step 2: Rewrite SubscriptionController**

Replace the ENTIRE content of `backend/notification-service/src/main/java/com/gcrf/library/notification/controller/SubscriptionController.java` with:

```java
package com.gcrf.library.notification.controller;

import com.gcrf.library.common.result.Result;
import com.gcrf.library.notification.dto.request.SubscriptionUpdateRequest;
import com.gcrf.library.notification.dto.response.SubscriptionVO;
import com.gcrf.library.notification.service.NotificationSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    @GetMapping("/user/{userId}")
    public Result<SubscriptionVO> getUserSubscription(@PathVariable Long userId) {
        log.info("获取用户订阅配置: userId={}", userId);
        SubscriptionVO subscription = subscriptionService.getUserSubscription(userId);
        return Result.success(subscription);
    }

    @PutMapping("/user/{userId}")
    public Result<SubscriptionVO> updateSubscription(
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionUpdateRequest request
    ) {
        log.info("更新用户订阅配置: userId={}", userId);
        SubscriptionVO subscription = subscriptionService.updateSubscription(userId, request);
        return Result.success(subscription);
    }

    @GetMapping("/check")
    public Result<Boolean> checkSubscription(
            @RequestParam Long userId,
            @RequestParam String notificationType
    ) {
        boolean subscribed = subscriptionService.isSubscribed(userId, notificationType);
        return Result.success(subscribed);
    }
}
```

- [ ] **Step 3: Compile and test**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile -pl notification-service -am
cd backend && mvn test -pl notification-service
```

Expected: Compilation succeeds, tests pass (may need test updates for SubscriptionController).

- [ ] **Step 4: Commit**

```bash
git add backend/notification-service/
git commit -m "feat(notification): implement NotificationSubscriptionService + fix SubscriptionController

- Created NotificationSubscriptionServiceImpl with getUserSubscription, updateSubscription, isSubscribed
- Default subscription created on first access (all enabled)
- Rewrote SubscriptionController to use NotificationSubscriptionService (was referencing non-existent SubscriptionService)"
```

---

## Task 5: Add borrow count validation to reader-service

**Files:**

- Modify: `backend/reader-service/src/main/java/com/gcrf/library/reader/service/impl/ReaderServiceImpl.java`

- [ ] **Step 1: Update deleteReader() to check borrow count**

In `backend/reader-service/src/main/java/com/gcrf/library/reader/service/impl/ReaderServiceImpl.java`, replace the `deleteReader` method (lines 171-184) with:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReader(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        // 检查是否有未归还的图书
        try {
            Result<Integer> result = circulationServiceClient.getCurrentBorrowCount(id);
            if (result != null && result.getData() != null && result.getData() > 0) {
                throw new BusinessException("该读者有 " + result.getData() + " 本未还图书，无法删除");
            }
        } catch (BusinessException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.warn("调用circulation-service失败，跳过借阅检查, readerId: {}, error: {}", id, e.getMessage());
        }

        readerMapper.deleteById(id);
        log.info("删除读者成功, id: {}", id);
    }
```

- [ ] **Step 2: Update cancelCard() to check borrow count**

In the same file, replace the `cancelCard` method (lines 229-244) with:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO cancelCard(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        // 检查是否有未归还的图书
        try {
            Result<Integer> result = circulationServiceClient.getCurrentBorrowCount(id);
            if (result != null && result.getData() != null && result.getData() > 0) {
                throw new BusinessException("该读者有 " + result.getData() + " 本未还图书，无法注销借书卡");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用circulation-service失败，跳过借阅检查, readerId: {}, error: {}", id, e.getMessage());
        }

        reader.setStatus("EXPIRED");
        readerMapper.updateById(reader);
        log.info("注销借书卡成功, id: {}", id);

        return ReaderDetailVO.from(reader);
    }
```

- [ ] **Step 3: Ensure CirculationServiceClient is injected**

Check that `ReaderServiceImpl` has `CirculationServiceClient` as a field. If not, add it. The class uses `@RequiredArgsConstructor`, so just adding a `private final CirculationServiceClient circulationServiceClient;` field is sufficient.

Verify the import exists:

```java
import com.gcrf.library.reader.client.CirculationServiceClient;
```

- [ ] **Step 4: Compile and test**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile -pl reader-service -am
cd backend && mvn test -pl reader-service
```

Expected: Compilation succeeds. Some existing tests may need mock updates for the new circulationServiceClient dependency. In test classes, add a `@Mock CirculationServiceClient circulationServiceClient;` field and configure it to return `Result.success(0)` by default.

- [ ] **Step 5: Commit**

```bash
git add backend/reader-service/
git commit -m "fix(reader): add borrow count validation before delete/cancel

- deleteReader checks circulation-service for unreturned books
- cancelCard checks circulation-service for unreturned books
- Graceful degradation: if circulation-service unavailable, logs warning and allows operation"
```

---

## Task 6: Fix MenuService.getUserMenus with Feign integration

**Files:**

- Create: `backend/system-service/src/main/java/com/gcrf/library/system/client/AuthServiceClient.java`
- Modify: `backend/system-service/src/main/java/com/gcrf/library/system/service/impl/MenuServiceImpl.java`

- [ ] **Step 1: Create AuthServiceClient Feign interface**

Create directory and file `backend/system-service/src/main/java/com/gcrf/library/system/client/AuthServiceClient.java`:

```java
package com.gcrf.library.system.client;

import com.gcrf.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthServiceClient {

    @GetMapping("/users/{userId}/role-ids")
    Result<List<Long>> getUserRoleIds(@PathVariable("userId") Long userId);
}
```

- [ ] **Step 2: Update MenuServiceImpl.getUserMenus()**

In `backend/system-service/src/main/java/com/gcrf/library/system/service/impl/MenuServiceImpl.java`:

Add the field (after `private final RoleMenuMapper roleMenuMapper;`):

```java
    private final AuthServiceClient authServiceClient;
```

Add the import:

```java
import com.gcrf.library.system.client.AuthServiceClient;
```

Replace the `getUserMenus` method (lines 52-91) with:

```java
    @Override
    public List<MenuTreeVO> getUserMenus(Long userId) {
        // 从auth-service获取用户角色ID列表
        List<Long> roleIds;
        try {
            Result<List<Long>> result = authServiceClient.getUserRoleIds(userId);
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                log.warn("用户无角色, userId: {}", userId);
                return List.of();
            }
            roleIds = result.getData();
        } catch (Exception e) {
            log.warn("调用auth-service获取用户角色失败, userId: {}, error: {}", userId, e.getMessage());
            return List.of();
        }

        // 根据角色ID查询关联的菜单ID
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(
            new LambdaQueryWrapper<RoleMenu>()
                .in(RoleMenu::getRoleId, roleIds)
        );

        if (roleMenus.isEmpty()) {
            return List.of();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 查询菜单详情
        List<Menu> menus = menuMapper.selectBatchIds(menuIds);

        // 过滤已删除和不可见的菜单
        List<Menu> visibleMenus = menus.stream()
                .filter(m -> m.getDeletedAt() == null)
                .filter(m -> Boolean.TRUE.equals(m.getIsVisible()))
                .sorted((m1, m2) -> Integer.compare(
                    m1.getSortOrder() != null ? m1.getSortOrder() : 0,
                    m2.getSortOrder() != null ? m2.getSortOrder() : 0
                ))
                .collect(Collectors.toList());

        return buildMenuTree(visibleMenus, null);
    }
```

- [ ] **Step 3: Ensure @EnableFeignClients is on the Application class**

Check `backend/system-service/src/main/java/com/gcrf/library/system/SystemServiceApplication.java` — if it doesn't have `@EnableFeignClients`, add it. Also verify that `spring-cloud-starter-openfeign` is in `system-service/pom.xml`.

- [ ] **Step 4: Compile and test**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile -pl system-service -am
cd backend && mvn test -pl system-service
```

Expected: Compilation succeeds. Tests for MenuService may need a mock `AuthServiceClient`.

- [ ] **Step 5: Commit**

```bash
git add backend/system-service/
git commit -m "fix(system): implement role-based menu filtering via auth-service Feign

- Created AuthServiceClient Feign interface
- getUserMenus now fetches user roles from auth-service
- Filters menus by role_menus mapping
- Graceful degradation: returns empty list if auth-service unavailable"
```

---

## Task 7: Final verification

- [ ] **Step 1: Compile all modified services**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend && mvn clean compile
```

Expected: All services compile.

- [ ] **Step 2: Run all tests**

Run:

```bash
cd backend && mvn test
```

Expected: All tests pass (or pre-existing failures only — no new failures from our changes).

- [ ] **Step 3: Verify frontend build**

Run:

```bash
cd web-admin && npm run build
```

Expected: Build succeeds (processReservation import error fixed).

- [ ] **Step 4: Commit spec and plan docs**

```bash
git add docs/specs/2026-04-13-core-services-gap-fix-design.md docs/specs/2026-04-13-core-services-gap-fix-plan.md
git commit -m "docs(docs): add core services gap fix spec and implementation plan"
```
