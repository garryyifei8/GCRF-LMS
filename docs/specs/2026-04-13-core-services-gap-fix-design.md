# 核心服务缺口修复设计

**日期：** 2026-04-13
**状态：** Approved
**背景：** 5 个核心服务（book, circulation, reader, system, notification）实际完成度远高于预期（90-100%），但存在 5 类具体缺口需要修复。

---

## 缺口总览

| #   | 服务                 | 问题                                                       | 严重度 |
| --- | -------------------- | ---------------------------------------------------------- | ------ |
| 1   | notification-service | Controller/Service 签名不匹配 + SubscriptionService 无实现 | 高     |
| 2   | system-service       | 6 个 Controller 路由路径不匹配前端                         | 高     |
| 3   | reader-service       | 删除/注销读者时未校验未还书                                | 中     |
| 4   | system-service       | MenuService.getUserMenus() 简化实现                        | 中     |
| 5   | web-admin            | processReservation 未导出导致构建失败                      | 高     |

---

## 缺口 #1：notification-service 签名不匹配 + SubscriptionService

### 问题分析

**NotificationController (10 个方法，7 个有问题)：**

| Controller 方法                         | 问题                                                                                |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| `queryNotifications(request)`           | 缺少 userId 参数，Service 接口需要 `(Long userId, request)`                         |
| `getNotificationById(id)`               | 缺少 userId，Service 需要 `(Long userId, Long notificationId)`                      |
| `markAsRead(id)`                        | Service 接口无此方法，接口有 `markAsRead(Long userId, NotificationMarkReadRequest)` |
| `batchMarkAsRead(ids)`                  | Service 接口无此方法                                                                |
| `deleteNotification(id)`                | 缺少 userId                                                                         |
| `batchDeleteNotifications(ids)`         | 缺少 userId                                                                         |
| `getUnreadCount(userId)`                | 返回 Long，但 Service 返回 UnreadCountVO                                            |
| `getLatestNotifications(userId, limit)` | Service 接口无此方法                                                                |
| `clearAllNotifications(userId)`         | Service 接口无此方法                                                                |

**SubscriptionController：**

- 导入 `SubscriptionService`（不存在）
- 应导入 `NotificationSubscriptionService`（接口存在，但无实现类）

### 修复方案

**1. 重写 NotificationController** — 对齐所有方法签名：

- 所有需要 userId 的方法，从 `@RequestParam Long userId` 获取
- `markAsRead` 改为接收 `NotificationMarkReadRequest`
- `getUnreadCount` 返回类型改为 `UnreadCountVO`

**2. 补充 NotificationService 接口 + Impl** — 添加 3 个缺失方法：

- `List<NotificationVO> getLatestNotifications(Long userId, Integer limit)` — 查询最近 N 条通知
- `void clearAllNotifications(Long userId)` — 软删除用户所有通知
- `void batchMarkAsRead(Long userId, List<Long> notificationIds)` — 批量标记已读

**3. 实现 NotificationSubscriptionServiceImpl：**

- `getUserSubscription(Long userId)` — 查询用户订阅配置
- `updateSubscription(Long userId, SubscriptionUpdateRequest)` — 更新订阅偏好
- `isSubscribed(Long userId, String notificationType)` — 检查是否订阅

**4. 修复 SubscriptionController：**

- 导入改为 `NotificationSubscriptionService`
- 对齐方法调用到实际接口

---

## 缺口 #2：system-service 路由不匹配

### 问题分析

前端统一使用 `/api/v1/system/` 前缀，但后端 Controller 路径不一致：

| Controller             | 当前路径                 | 前端期望路径                    |
| ---------------------- | ------------------------ | ------------------------------- |
| DepartmentController   | `/api/departments`       | `/api/v1/system/departments`    |
| RoleController         | `/api/v1/roles`          | `/api/v1/system/roles`          |
| PermissionController   | `/api/v1/permissions`    | `/api/v1/system/permissions`    |
| MenuController         | `/api/v1/menus`          | `/api/v1/system/menus`          |
| LoginLogController     | `/api/v1/login-logs`     | `/api/v1/system/login-logs`     |
| OperationLogController | `/api/v1/operation-logs` | `/api/v1/system/operation-logs` |

前端还调用 `/api/v1/system/users` 相关 API — 这些由 auth-service 处理，通过 Gateway 路由，不在 system-service 中新增。

### 修复方案

修改 6 个 Controller 的 `@RequestMapping` 注解，统一加上 `/api/v1/system/` 前缀。同时更新对应的测试文件中的 URL 路径。

---

## 缺口 #3：reader-service 借阅校验

### 问题分析

`ReaderServiceImpl` 中两处 TODO：

- `deleteReader()`: "Check if borrowed books exist (needs circulation-service integration)"
- `cancelCard()`: "Check if unreturned books exist (needs circulation-service integration)"

`CirculationServiceClient` Feign 接口已定义 `getCurrentBorrowCount(readerId)` 方法。

### 修复方案

在 `deleteReader()` 和 `cancelCard()` 方法开头，调用 `circulationServiceClient.getCurrentBorrowCount(readerId)`：

- 如果返回 count > 0，抛出 `BusinessException("该读者有 N 本未还图书，无法执行此操作")`
- 如果 Feign 调用失败（服务不可用），降级为允许操作但记录 warn 日志

---

## 缺口 #4：system-service MenuService 简化实现

### 问题分析

`MenuServiceImpl.getUserMenus(userId)` 当前返回所有菜单，未根据用户角色过滤。代码注释："needs Feign to auth-service"。

### 修复方案

1. 创建 `AuthServiceClient` Feign 接口，定义 `getUserRoleIds(Long userId)` 方法
2. 在 `getUserMenus()` 中：
   - 调用 `authServiceClient.getUserRoleIds(userId)` 获取角色 ID 列表
   - 查询 `role_menus` 表获取这些角色对应的 menuId 集合
   - 用 menuId 集合过滤菜单树
   - Feign 调用失败时降级返回空列表

---

## 缺口 #5：前端构建错误

### 问题分析

`web-admin/src/views/circulation/reservations.vue` 导入了 `processReservation`，但 `web-admin/src/api/circulation.js` 未导出该函数。已导出的类似函数是 `pickupReservation`（调用 `POST /api/v1/reserves/{id}/pickup`）。

### 修复方案

**方案：在 circulation.js 中添加 processReservation**

```javascript
export function processReservation(id) {
  return request({
    url: `/api/v1/reserves/${id}/pickup`,
    method: "post",
  });
}
```

语义上"处理预约"等同于"确认取书"，对应后端已实现的 `ReserveController.pickupReserve()` endpoint。

同时检查 `reservations.vue` 中是否还导入了其他未导出的函数（`notifyReservation`），一并补全。

---

## 修改文件清单

### notification-service

| 文件                                                    | 操作                     |
| ------------------------------------------------------- | ------------------------ |
| `controller/NotificationController.java`                | 重写 — 对齐签名          |
| `controller/SubscriptionController.java`                | 修改 — 改导入，对齐调用  |
| `service/NotificationService.java`                      | 修改 — 添加 3 个方法声明 |
| `service/impl/NotificationServiceImpl.java`             | 修改 — 实现 3 个新方法   |
| `service/impl/NotificationSubscriptionServiceImpl.java` | 新建 — 实现订阅服务      |
| 对应测试文件                                            | 修改/新建 — 覆盖新逻辑   |

### system-service

| 文件                                     | 操作                        |
| ---------------------------------------- | --------------------------- |
| `controller/DepartmentController.java`   | 修改 — @RequestMapping 路径 |
| `controller/RoleController.java`         | 修改 — @RequestMapping 路径 |
| `controller/PermissionController.java`   | 修改 — @RequestMapping 路径 |
| `controller/MenuController.java`         | 修改 — @RequestMapping 路径 |
| `controller/LoginLogController.java`     | 修改 — @RequestMapping 路径 |
| `controller/OperationLogController.java` | 修改 — @RequestMapping 路径 |
| `service/impl/MenuServiceImpl.java`      | 修改 — getUserMenus 实现    |
| `client/AuthServiceClient.java`          | 新建 — Feign 客户端         |
| 对应测试文件                             | 修改 — URL 路径更新         |

### reader-service

| 文件                                  | 操作                                          |
| ------------------------------------- | --------------------------------------------- |
| `service/impl/ReaderServiceImpl.java` | 修改 — deleteReader + cancelCard 添加借阅校验 |
| 对应测试文件                          | 修改 — 补充校验测试                           |

### web-admin

| 文件                                     | 操作                                               |
| ---------------------------------------- | -------------------------------------------------- |
| `src/api/circulation.js`                 | 修改 — 添加 processReservation + notifyReservation |
| `src/views/circulation/reservations.vue` | 验证 — 确认导入修复后构建通过                      |
