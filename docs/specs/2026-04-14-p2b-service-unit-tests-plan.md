# P2B: 4 个 Service 层单元测试补全 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Mockito-based unit tests for 4 Services that currently have zero test coverage: DepartmentService, NotificationService, NotificationSubscriptionService, NotificationTemplateService.

**Architecture:** Pure JUnit 5 + Mockito unit tests. No Spring context, no database. Follow existing pattern from `RoleServiceTest`. Each test class mocks its Mapper dependencies.

**Tech Stack:** JUnit 5, Mockito (MockitoExtension), AssertJ

**Spec:** `docs/specs/2026-04-14-p2b-service-unit-tests-design.md`

---

## File Map

### New Files

| File                                                                                                                        | Tests |
| --------------------------------------------------------------------------------------------------------------------------- | ----- |
| `backend/system-service/src/test/java/com/gcrf/library/system/service/DepartmentServiceTest.java`                           | ~15   |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationServiceTest.java`             | ~25   |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationSubscriptionServiceTest.java` | ~15   |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationTemplateServiceTest.java`     | ~22   |

### No Source Modifications

P2B only adds test files. Service implementations are not modified.

---

## Pattern Reference

**Template for all 4 test classes:**

```java
package com.gcrf.library.<service>.service;

import com.gcrf.library.<service>.entity.<Entity>;
import com.gcrf.library.<service>.mapper.<Entity>Mapper;
import com.gcrf.library.<service>.service.impl.<Entity>ServiceImpl;
import com.gcrf.library.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class <Entity>ServiceTest {

    @Mock
    private <Entity>Mapper mapper;

    @InjectMocks
    private <Entity>ServiceImpl service;

    private <Entity> testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new <Entity>();
        // Set common fields
    }

    @Test
    @DisplayName("methodName_scenario_expected")
    void methodName_scenario_expected() {
        // Arrange
        when(mapper.selectById(1L)).thenReturn(testEntity);

        // Act
        Result result = service.someMethod(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(mapper).selectById(1L);
    }
}
```

---

## Task 1: DepartmentServiceTest

**Files:**

- Create: `backend/system-service/src/test/java/com/gcrf/library/system/service/DepartmentServiceTest.java`

- [ ] **Step 1: Read DepartmentServiceImpl and DepartmentMapper**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/system-service/src/main/java/com/gcrf/library/system/service/impl/DepartmentServiceImpl.java
cat backend/system-service/src/main/java/com/gcrf/library/system/service/DepartmentService.java
cat backend/system-service/src/main/java/com/gcrf/library/system/mapper/DepartmentMapper.java
```

Note all public methods, their logic branches (duplicate check, parent level calculation, child count check), and DTO class names (DepartmentCreateRequest, DepartmentUpdateRequest, DepartmentQueryRequest, DepartmentResponse, Department entity).

- [ ] **Step 2: Read an existing pattern**

```bash
cat backend/system-service/src/test/java/com/gcrf/library/system/service/RoleServiceTest.java | head -80
```

- [ ] **Step 3: Create DepartmentServiceTest with ALL 15 test cases**

Create the file with these tests (adjust method names/DTO fields based on Step 1 findings):

Test cases required:

1. `createDepartment_whenDeptCodeExists_shouldThrowException`
2. `createDepartment_withParent_shouldCalculateLevelFromParent` (level = parent.deptLevel + 1)
3. `createDepartment_withParent_shouldBuildDeptPathWithAncestors`
4. `createDepartment_withoutParent_shouldSetLevelToOne`
5. `createDepartment_success_shouldInsertAndReturnResponse`
6. `updateDepartment_whenDeptNotFound_shouldThrowException`
7. `updateDepartment_withPartialFields_shouldOnlyUpdateNonNullFields`
8. `getDepartmentById_whenNotFound_shouldThrowException`
9. `getDepartmentById_success_shouldReturnResponse`
10. `queryDepartments_withFilters_shouldApplyAllFilters` (deptCode, deptName, status)
11. `queryDepartments_withPagination_shouldUsePageNumAndSize`
12. `queryDepartments_default_shouldOrderBySortOrderAndId`
13. `deleteDepartment_whenNotFound_shouldThrowException`
14. `deleteDepartment_whenHasChildren_shouldThrowException`
15. `deleteDepartment_success_shouldDelete`

Example of the "duplicate check" test to illustrate style:

```java
@Test
@DisplayName("createDepartment_whenDeptCodeExists_shouldThrowException")
void createDepartment_whenDeptCodeExists_shouldThrowException() {
    // Arrange
    DepartmentCreateRequest request = new DepartmentCreateRequest();
    request.setDeptCode("R001");
    request.setDeptName("研发部");

    when(departmentMapper.selectCount(any())).thenReturn(1L);

    // Act & Assert
    assertThatThrownBy(() -> service.createDepartment(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("部门编码已存在");

    verify(departmentMapper).selectCount(any());
}
```

Use `LambdaQueryWrapper` mocking pattern from RoleServiceTest when needed.

- [ ] **Step 4: Run the test**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl system-service -Dtest=DepartmentServiceTest
```

Expected: All 15 tests pass. If any fail due to discovery about the service implementation (e.g., actual exception is different type or message), adjust test assertions — do NOT modify the service.

- [ ] **Step 5: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/system-service/src/test/java/com/gcrf/library/system/service/DepartmentServiceTest.java
git commit -m "test(system): add DepartmentServiceTest unit tests

- 15 tests covering createDepartment, updateDepartment, getDepartmentById,
  queryDepartments, deleteDepartment
- Mockito + AssertJ + JUnit 5 following RoleServiceTest pattern
- Tests cover duplicate code check, parent level/path calculation,
  child dept constraint on delete"
```

---

## Task 2: NotificationServiceTest

**Files:**

- Create: `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationServiceTest.java`

- [ ] **Step 1: Read NotificationServiceImpl and NotificationMapper**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationServiceImpl.java
cat backend/notification-service/src/main/java/com/gcrf/library/notification/service/NotificationService.java
```

Note the 11 methods: sendNotification, queryNotifications, markAsRead, markAllAsRead, getUnreadCount, getNotificationById, deleteNotification, batchDeleteNotifications, getLatestNotifications, batchMarkAsRead, clearAllNotifications.

Note DTO types: NotificationSendRequest, NotificationQueryRequest, NotificationMarkReadRequest, UnreadCountVO, NotificationVO.

- [ ] **Step 2: Create NotificationServiceTest with 25 test cases**

Required tests:

**sendNotification (3)**

1. `sendNotification_withNullPriority_shouldDefaultToNormal`
2. `sendNotification_shouldSetIsReadFalse`
3. `sendNotification_success_shouldInsertAndReturnVO`

**queryNotifications (3)** 4. `queryNotifications_withFilters_shouldApplyAllFilters` (type/isRead/priority) 5. `queryNotifications_shouldExcludeSoftDeleted` 6. `queryNotifications_shouldOrderByCreatedAtDesc`

**markAsRead (4)** 7. `markAsRead_whenMarkAllTrue_shouldCallMarkAllAsRead` 8. `markAsRead_whenNotificationIdNull_shouldThrowException` 9. `markAsRead_whenNotFound_shouldThrowException` 10. `markAsRead_whenAlreadyRead_shouldNotUpdate` 11. `markAsRead_success_shouldSetIsReadAndReadAt`

**markAllAsRead (1)** 12. `markAllAsRead_success_shouldUpdateAllUnread`

**getUnreadCount (1)** 13. `getUnreadCount_shouldReturnTotalAndUrgentCounts`

**getNotificationById (2)** 14. `getNotificationById_whenNotFound_shouldThrowException` 15. `getNotificationById_success_shouldReturnVO`

**deleteNotification (2)** 16. `deleteNotification_whenNotFound_shouldThrowException` 17. `deleteNotification_success_shouldSoftDelete`

**batchDeleteNotifications (2)** 18. `batchDeleteNotifications_whenEmpty_shouldThrowException` 19. `batchDeleteNotifications_success_shouldCallDeleteForEach`

**getLatestNotifications (2)** 20. `getLatestNotifications_withNullLimit_shouldUseDefault10` 21. `getLatestNotifications_shouldOrderByCreatedAtDesc`

**batchMarkAsRead (2)** 22. `batchMarkAsRead_withEmptyList_shouldReturnWithoutError` 23. `batchMarkAsRead_success_shouldUpdateAllToRead`

**clearAllNotifications (1)** 24. `clearAllNotifications_success_shouldSoftDeleteAll`

Example for the unread count test:

```java
@Test
@DisplayName("getUnreadCount_shouldReturnTotalAndUrgentCounts")
void getUnreadCount_shouldReturnTotalAndUrgentCounts() {
    // Arrange
    Long userId = 1L;
    // First call: total unread count
    // Second call: urgent unread count
    when(notificationMapper.selectCount(any(LambdaQueryWrapper.class)))
        .thenReturn(10L)  // total
        .thenReturn(2L);  // urgent

    // Act
    UnreadCountVO result = notificationService.getUnreadCount(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getUnreadCount()).isEqualTo(10L);
    assertThat(result.getUrgentCount()).isEqualTo(2L);
    verify(notificationMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
}
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl notification-service -Dtest=NotificationServiceTest
```

Expected: All 24 tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationServiceTest.java
git commit -m "test(notification): add NotificationServiceTest unit tests

- 24 tests covering 11 methods including P0-added getLatestNotifications,
  batchMarkAsRead, clearAllNotifications
- Tests cover priority defaults, soft delete, markAll flow,
  unread+urgent count tracking, batch operations"
```

---

## Task 3: NotificationSubscriptionServiceTest

**Files:**

- Create: `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationSubscriptionServiceTest.java`

- [ ] **Step 1: Read NotificationSubscriptionServiceImpl**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationSubscriptionServiceImpl.java
```

Note: Uses comma-separated string for `subscribedTypes` (not JSON). The Impl has a private `createDefaultSubscription` helper that inserts a default record and returns it.

Confirm 3 public methods: getUserSubscription, updateSubscription, isSubscribed.

- [ ] **Step 2: Create test file with 15 tests**

Required tests:

**getUserSubscription (2)**

1. `getUserSubscription_whenNotExists_shouldCreateDefaultAndReturn` (all enabled, empty types)
2. `getUserSubscription_whenExists_shouldReturnVO`

**updateSubscription (7)** 3. `updateSubscription_whenNotExists_shouldCreateNewWithRequestValues` 4. `updateSubscription_whenExists_shouldUpdateOnlyNonNullFields` 5. `updateSubscription_shouldUpdateEmailEnabled` 6. `updateSubscription_shouldUpdateSmsEnabled` 7. `updateSubscription_shouldUpdateNotificationEnabled` 8. `updateSubscription_withSubscribedTypesList_shouldJoinToCommaString` 9. `updateSubscription_shouldRefreshUpdatedAt`

**isSubscribed (5)** 10. `isSubscribed_whenNotExists_shouldReturnTrue` (default allow) 11. `isSubscribed_whenNotificationEnabledFalse_shouldReturnFalse` 12. `isSubscribed_withEmptyTypes_shouldReturnTrue` (empty means all allowed) 13. `isSubscribed_whenTypeInList_shouldReturnTrue` 14. `isSubscribed_whenTypeNotInList_shouldReturnFalse`

Example for the "create default" test:

```java
@Test
@DisplayName("getUserSubscription_whenNotExists_shouldCreateDefaultAndReturn")
void getUserSubscription_whenNotExists_shouldCreateDefaultAndReturn() {
    // Arrange
    Long userId = 1L;
    when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(subscriptionMapper.insert(any(NotificationSubscription.class))).thenReturn(1);

    // Act
    SubscriptionVO result = service.getUserSubscription(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getEmailEnabled()).isTrue();
    assertThat(result.getSmsEnabled()).isTrue();
    assertThat(result.getNotificationEnabled()).isTrue();
    assertThat(result.getSubscribedTypes()).isEqualTo("");

    ArgumentCaptor<NotificationSubscription> captor =
        ArgumentCaptor.forClass(NotificationSubscription.class);
    verify(subscriptionMapper).insert(captor.capture());
    NotificationSubscription inserted = captor.getValue();
    assertThat(inserted.getUserId()).isEqualTo(userId);
    assertThat(inserted.getCreatedAt()).isNotNull();
}
```

Example for "join to comma string":

```java
@Test
@DisplayName("updateSubscription_withSubscribedTypesList_shouldJoinToCommaString")
void updateSubscription_withSubscribedTypesList_shouldJoinToCommaString() {
    // Arrange
    Long userId = 1L;
    NotificationSubscription existing = new NotificationSubscription();
    existing.setUserId(userId);
    existing.setEmailEnabled(true);
    existing.setSmsEnabled(true);
    existing.setNotificationEnabled(true);

    when(subscriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

    SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
    request.setSubscribedTypes(List.of("SYSTEM", "USER", "ACTIVITY"));

    // Act
    service.updateSubscription(userId, request);

    // Assert
    ArgumentCaptor<NotificationSubscription> captor =
        ArgumentCaptor.forClass(NotificationSubscription.class);
    verify(subscriptionMapper).updateById(captor.capture());
    assertThat(captor.getValue().getSubscribedTypes()).isEqualTo("SYSTEM,USER,ACTIVITY");
}
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl notification-service -Dtest=NotificationSubscriptionServiceTest
```

Expected: All 14 tests pass.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationSubscriptionServiceTest.java
git commit -m "test(notification): add NotificationSubscriptionServiceTest

- 14 tests covering getUserSubscription, updateSubscription, isSubscribed
- Tests cover default subscription creation, partial field updates,
  comma-separated types serialization, default-allow behavior"
```

---

## Task 4: NotificationTemplateServiceTest

**Files:**

- Create: `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationTemplateServiceTest.java`

- [ ] **Step 1: Read NotificationTemplateServiceImpl**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
cat backend/notification-service/src/main/java/com/gcrf/library/notification/service/impl/NotificationTemplateServiceImpl.java
```

Note 10 public methods: queryTemplates (2 overloads), getTemplateById, getTemplateByCode, createTemplate, updateTemplate (2 overloads), changeTemplateStatus, deleteTemplate, renderTemplate.

Note DTOs: TemplateCreateRequest, TemplateUpdateRequest, TemplateQueryRequest, NotificationTemplateVO.

- [ ] **Step 2: Create test file with 22 tests**

Required tests:

**queryTemplates (4)**

1. `queryTemplates_withTemplateType_shouldFilter`
2. `queryTemplates_withoutTemplateType_shouldReturnAll`
3. `queryTemplates_shouldExcludeSoftDeleted`
4. `queryTemplates_withRequest_shouldDelegateToThreeArgVersion`

**getTemplateById (2)** 5. `getTemplateById_whenNotFound_shouldThrowException` 6. `getTemplateById_success_shouldReturnVO`

**getTemplateByCode (2)** 7. `getTemplateByCode_whenNotFound_shouldThrowException` 8. `getTemplateByCode_success_shouldReturnVO`

**createTemplate (3)** 9. `createTemplate_whenCodeExists_shouldThrowException` 10. `createTemplate_withVariablesList_shouldJoinToCommaString` 11. `createTemplate_success_shouldInsertAndReturnVO`

**updateTemplate (4)** 12. `updateTemplate_withCreateRequest_whenNotFound_shouldThrowException` 13. `updateTemplate_withCreateRequest_partialUpdate_shouldOnlyUpdateNonNullFields` 14. `updateTemplate_withUpdateRequest_whenNotFound_shouldThrowException` 15. `updateTemplate_withUpdateRequest_partialUpdate_shouldOnlyUpdateNonNullFields`

**changeTemplateStatus (3)** 16. `changeTemplateStatus_whenNotFound_shouldThrowException` 17. `changeTemplateStatus_whenEnabledTrue_shouldSetActive` 18. `changeTemplateStatus_whenEnabledFalse_shouldSetInactive`

**deleteTemplate (2)** 19. `deleteTemplate_whenNotFound_shouldThrowException` 20. `deleteTemplate_success_shouldSoftDelete`

**renderTemplate (4)** 21. `renderTemplate_whenTemplateNotFound_shouldThrowException` 22. `renderTemplate_whenContentEmpty_shouldThrowException` 23. `renderTemplate_withoutVariables_shouldReturnOriginalContent` 24. `renderTemplate_withVariables_shouldReplacePlaceholders` 25. `renderTemplate_withNullVariableValue_shouldReplaceWithEmptyString`

Example for renderTemplate:

```java
@Test
@DisplayName("renderTemplate_withVariables_shouldReplacePlaceholders")
void renderTemplate_withVariables_shouldReplacePlaceholders() {
    // Arrange
    NotificationTemplate template = new NotificationTemplate();
    template.setId(1L);
    template.setContent("您好 {username}, 您的订单 {orderId} 已发货");

    when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

    Map<String, Object> variables = Map.of(
        "username", "张三",
        "orderId", "ORD-001"
    );

    // Act
    String result = service.renderTemplate(1L, variables);

    // Assert
    assertThat(result).isEqualTo("您好 张三, 您的订单 ORD-001 已发货");
}

@Test
@DisplayName("renderTemplate_withNullVariableValue_shouldReplaceWithEmptyString")
void renderTemplate_withNullVariableValue_shouldReplaceWithEmptyString() {
    // Arrange
    NotificationTemplate template = new NotificationTemplate();
    template.setId(1L);
    template.setContent("Hello {name}");

    when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(template);

    Map<String, Object> variables = new HashMap<>();
    variables.put("name", null);

    // Act
    String result = service.renderTemplate(1L, variables);

    // Assert
    assertThat(result).isEqualTo("Hello ");
}
```

- [ ] **Step 3: Run**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl notification-service -Dtest=NotificationTemplateServiceTest
```

Expected: All 25 tests pass. If method signatures or exception messages differ from Step 1 discovery, adjust tests — do NOT modify the service.

- [ ] **Step 4: Commit**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationTemplateServiceTest.java
git commit -m "test(notification): add NotificationTemplateServiceTest

- 25 tests covering all CRUD operations plus renderTemplate
- Tests cover duplicate code check, status toggle (ACTIVE/INACTIVE),
  variable substitution with null handling, partial updates"
```

---

## Task 5: Final verification

- [ ] **Step 1: Run all 4 tests together**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend
mvn test -pl system-service -Dtest=DepartmentServiceTest
mvn test -pl notification-service -Dtest='NotificationServiceTest,NotificationSubscriptionServiceTest,NotificationTemplateServiceTest'
```

Expected: All ~80 tests pass across 4 test classes.

- [ ] **Step 2: Commit spec and plan**

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem
git add docs/specs/2026-04-14-p2b-service-unit-tests-design.md docs/specs/2026-04-14-p2b-service-unit-tests-plan.md
git commit -m "docs(docs): add P2B Service unit tests spec and plan"
```
