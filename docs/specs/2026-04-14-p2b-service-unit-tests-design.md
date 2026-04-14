# P2B: 4 个 Service 层单元测试补全设计

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P2（测试覆盖补全）
**背景：** 生产就绪审计发现 4 个 Service 无单元测试 — DepartmentService、NotificationService、NotificationSubscriptionService、NotificationTemplateService。P2B 为这些服务补充 Mockito-based 单元测试。

---

## 目标

为 4 个无测试覆盖的 Service 补充单元测试，达到与现有 RoleServiceTest/BorrowServiceTest 等同的测试密度。

---

## 测试风格（统一）

所有 P2B 测试沿用现有项目的 Service 层单测模式：

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @Mock
    private XxxMapper xxxMapper;

    @InjectMocks
    private XxxServiceImpl xxxService;

    @BeforeEach
    void setUp() {
        // Common test data setup
    }

    @Test
    @DisplayName("方法名_场景_预期结果")
    void methodName_whenScenario_shouldExpectedResult() {
        // Arrange
        when(xxxMapper.selectById(anyLong())).thenReturn(someEntity);

        // Act
        Result result = xxxService.someMethod(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getField()).isEqualTo(expected);
        verify(xxxMapper).updateById(any(Xxx.class));
    }
}
```

**依赖库：**

- JUnit 5
- Mockito (`org.mockito.junit.jupiter.MockitoExtension`)
- AssertJ (`org.assertj.core.api.Assertions.assertThat`)

---

## Service 1: DepartmentServiceTest

**文件：** `backend/system-service/src/test/java/com/gcrf/library/system/service/DepartmentServiceTest.java`

**Mock：** `DepartmentMapper`

**测试方法数：** ~15

### 覆盖清单

| 方法                | 测试场景                                           |
| ------------------- | -------------------------------------------------- |
| `createDepartment`  | 重复 deptCode 抛异常                               |
| `createDepartment`  | 有父部门 — 正确计算 deptLevel = parent.level + 1   |
| `createDepartment`  | 有父部门 — 正确构建 deptPath（如 "/1/2"）          |
| `createDepartment`  | 无父部门 — deptLevel = 1                           |
| `createDepartment`  | 成功插入并返回 response                            |
| `updateDepartment`  | 部门不存在抛异常                                   |
| `updateDepartment`  | 仅更新非 null 字段（name/phone/email/description） |
| `getDepartmentById` | 部门不存在抛异常                                   |
| `getDepartmentById` | 成功返回 response                                  |
| `queryDepartments`  | 应用 deptCode/deptName/status 过滤                 |
| `queryDepartments`  | 分页参数正确                                       |
| `queryDepartments`  | 默认按 sortOrder 和 id 排序                        |
| `deleteDepartment`  | 部门不存在抛异常                                   |
| `deleteDepartment`  | 存在子部门抛异常                                   |
| `deleteDepartment`  | 成功删除                                           |

---

## Service 2: NotificationServiceTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationServiceTest.java`

**Mock：** `NotificationMapper`

**测试方法数：** ~25

### 覆盖清单

| 方法                       | 测试场景                              |
| -------------------------- | ------------------------------------- |
| `sendNotification`         | 默认 priority = NORMAL                |
| `sendNotification`         | isRead 默认 false                     |
| `sendNotification`         | 插入成功返回 VO                       |
| `queryNotifications`       | 过滤 notificationType/isRead/priority |
| `queryNotifications`       | 过滤日期范围                          |
| `queryNotifications`       | 排除 deletedAt != null 的软删除记录   |
| `queryNotifications`       | 按 createdAt DESC 排序                |
| `queryNotifications`       | 分页参数正确                          |
| `markAsRead`               | markAll=true 时调用 markAllAsRead     |
| `markAsRead`               | notificationId 为 null 抛异常         |
| `markAsRead`               | 通知不存在抛异常                      |
| `markAsRead`               | 已读状态跳过更新但不抛错              |
| `markAsRead`               | 成功将 isRead=true, readAt=now        |
| `markAllAsRead`            | 批量更新所有未读                      |
| `getUnreadCount`           | 返回总数 + 紧急数（PRIORITY=URGENT）  |
| `getNotificationById`      | 不存在抛异常                          |
| `getNotificationById`      | 成功返回 VO                           |
| `deleteNotification`       | 不存在抛异常                          |
| `deleteNotification`       | 软删除（deletedAt=now）               |
| `batchDeleteNotifications` | 空列表抛异常                          |
| `batchDeleteNotifications` | 循环调用 deleteNotification           |
| `getLatestNotifications`   | 默认 limit=10                         |
| `getLatestNotifications`   | 按 createdAt DESC 排序                |
| `batchMarkAsRead`          | 空列表直接返回                        |
| `batchMarkAsRead`          | 批量更新 isRead=true, readAt=now      |
| `clearAllNotifications`    | 软删除所有用户通知                    |

---

## Service 3: NotificationSubscriptionServiceTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationSubscriptionServiceTest.java`

**Mock：** `NotificationSubscriptionMapper`

**注意：** 基于 P0 实现的 `NotificationSubscriptionServiceImpl`，使用逗号分隔字符串存储 subscribedTypes（非 JSON）。

**测试方法数：** ~18

### 覆盖清单

| 方法                  | 测试场景                                        |
| --------------------- | ----------------------------------------------- |
| `getUserSubscription` | 订阅不存在 — 创建默认订阅并返回（全部 enabled） |
| `getUserSubscription` | 订阅存在 — 返回对应 VO                          |
| `updateSubscription`  | 订阅不存在 — 插入新订阅并应用 request           |
| `updateSubscription`  | 订阅存在 — 仅更新非 null 字段                   |
| `updateSubscription`  | emailEnabled 字段更新                           |
| `updateSubscription`  | smsEnabled 字段更新                             |
| `updateSubscription`  | notificationEnabled 字段更新                    |
| `updateSubscription`  | subscribedTypes 从 List 转为逗号分隔字符串      |
| `updateSubscription`  | updatedAt 自动刷新为 now                        |
| `updateSubscription`  | 成功返回 VO                                     |
| `isSubscribed`        | 订阅不存在返回 true（默认允许）                 |
| `isSubscribed`        | notificationEnabled=false 返回 false            |
| `isSubscribed`        | subscribedTypes 为空/null 返回 true             |
| `isSubscribed`        | 类型包含在订阅列表中返回 true                   |
| `isSubscribed`        | 类型不在订阅列表中返回 false                    |

---

## Service 4: NotificationTemplateServiceTest

**文件：** `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationTemplateServiceTest.java`

**Mock：** `NotificationTemplateMapper`

**测试方法数：** ~22

### 覆盖清单

| 方法                                      | 测试场景                        |
| ----------------------------------------- | ------------------------------- |
| `queryTemplates(pageNum, pageSize, type)` | 带 templateType 过滤            |
| `queryTemplates(pageNum, pageSize, type)` | 无 templateType 返回全部非删除  |
| `queryTemplates(pageNum, pageSize, type)` | 分页正确                        |
| `queryTemplates(TemplateQueryRequest)`    | 委托给 3-arg 版本               |
| `getTemplateById`                         | 不存在抛异常                    |
| `getTemplateById`                         | 成功返回 VO                     |
| `getTemplateByCode`                       | 不存在抛异常                    |
| `getTemplateByCode`                       | 成功返回 VO                     |
| `createTemplate`                          | 重复 code 抛异常                |
| `createTemplate`                          | variables List 转逗号分隔字符串 |
| `createTemplate`                          | variables 为 null 时不报错      |
| `createTemplate`                          | 成功插入并返回 VO               |
| `updateTemplate(id, CreateRequest)`       | 不存在抛异常                    |
| `updateTemplate(id, CreateRequest)`       | 仅更新非 null 字段              |
| `updateTemplate(id, UpdateRequest)`       | 不存在抛异常                    |
| `updateTemplate(id, UpdateRequest)`       | 仅更新非 null/非空字段          |
| `changeTemplateStatus`                    | 不存在抛异常                    |
| `changeTemplateStatus`                    | enabled=true → status=ACTIVE    |
| `changeTemplateStatus`                    | enabled=false → status=INACTIVE |
| `deleteTemplate`                          | 不存在抛异常                    |
| `deleteTemplate`                          | 软删除成功                      |
| `renderTemplate`                          | 模板不存在抛异常                |
| `renderTemplate`                          | content 为空抛异常              |
| `renderTemplate`                          | 无 variables 返回原 content     |
| `renderTemplate`                          | 变量替换成功（{key} → value）   |
| `renderTemplate`                          | 变量值为 null 替换为空字符串    |

---

## 执行约束

### TDD 要求

遵循 Red-Green-Refactor：

1. **Red：** 先写测试，运行确认失败（因为测试类本身不存在，编译就失败）
2. **Green：** 补充测试代码，使测试通过（实现代码已存在，无需修改）
3. **Refactor：** 如测试发现 Impl 代码有 bug，独立 commit 修复

### 不要修改 Service 实现

P2B 范围是**补测试**，不是重构。如果发现 bug：

1. 记录下来
2. 测试文档化（标注 `@Disabled("bug in service, reason: ...")`）
3. 在 commit message 中标注
4. 修复留给后续任务

### 单元测试不触数据库

**所有 P2B 测试都是纯单元测试，不继承 BaseIntegrationTest，不启动 Spring context，不启动数据库。** 使用 Mockito mock 所有依赖。

---

## 修改文件清单

### 新建（4 个测试文件）

| 文件                                                                                                                        | 测试数量 |
| --------------------------------------------------------------------------------------------------------------------------- | -------- |
| `backend/system-service/src/test/java/com/gcrf/library/system/service/DepartmentServiceTest.java`                           | ~15      |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationServiceTest.java`             | ~25      |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationSubscriptionServiceTest.java` | ~18      |
| `backend/notification-service/src/test/java/com/gcrf/library/notification/service/NotificationTemplateServiceTest.java`     | ~22      |

### 无修改文件

P2B 不修改任何源码。依赖已有：Mockito + AssertJ 来自 `spring-boot-starter-test`（已被 common-core 包含）。

---

## 验证方法

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend

# DepartmentServiceTest
mvn test -pl system-service -Dtest=DepartmentServiceTest

# 3 个 notification tests
mvn test -pl notification-service -Dtest='Notification*ServiceTest,NotificationTemplateServiceTest'
```

**预期：** 所有 ~80 个测试通过。
