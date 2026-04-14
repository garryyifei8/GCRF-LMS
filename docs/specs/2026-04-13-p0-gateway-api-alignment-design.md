# P0: 网关路由 + API 对接修复设计

**日期：** 2026-04-13
**状态：** Approved
**优先级：** P0（阻塞 — 前端请求到不了后端）
**背景：** 生产就绪审计发现网关路由缺失、前端 API 路径不匹配、部分后端端点未实现。

---

## 缺口总览

| #   | 问题                     | 影响                                                                | 严重度 |
| --- | ------------------------ | ------------------------------------------------------------------- | ------ |
| 1   | 网关缺少 6 个路由        | 前端调用 borrows/reserves/fines/system/analytics/inventory 全部 404 | 阻塞   |
| 2   | department.js 路径错误   | 部门 CRUD 无法工作                                                  | 阻塞   |
| 3   | auth info 端点路径不匹配 | 登录后获取用户信息失败                                              | 阻塞   |
| 4   | 6 个后端端点缺失         | 读者批量删除/卡管理/预约通知等功能不可用                            | 高     |

---

## 缺口 #1：网关路由补全

### 当前状态

gateway-service `application.yml` 有 6 条路由：

- `/api/v1/auth/**` → auth-service
- `/api/v1/books/**` → book-service
- `/api/v1/readers/**` → reader-service
- `/api/v1/circulation/**` → circulation-service（前端不调这个路径）
- `/api/v1/recommend/**` → recommend-service
- `/api/v1/chat/**` → chat-service

### 需要添加 6 条路由

```yaml
# 流通服务 - 借阅管理（前端实际调用路径）
- id: circulation-borrows
  uri: lb://circulation-service
  predicates:
    - Path=/api/v1/borrows/**

# 流通服务 - 预约管理
- id: circulation-reserves
  uri: lb://circulation-service
  predicates:
    - Path=/api/v1/reserves/**

# 流通服务 - 罚款管理
- id: circulation-fines
  uri: lb://circulation-service
  predicates:
    - Path=/api/v1/fines/**

# 系统管理服务
- id: system-service
  uri: lb://system-service
  predicates:
    - Path=/api/v1/system/**

# 统计分析服务
- id: analytics-service
  uri: lb://analytics-service
  predicates:
    - Path=/api/v1/analytics/**

# 库存管理（在 book-service 下）
- id: book-inventory
  uri: lb://book-service
  predicates:
    - Path=/api/v1/inventory/**
```

保留现有的 `/api/v1/circulation/**` 路由以兼容旧 API。

---

## 缺口 #2：department.js 路径修复

### 问题

`web-admin/src/api/department.js` 中 5 个 URL 使用旧路径 `/api/departments`，后端已改为 `/api/v1/system/departments`。

### 修复

将 5 个 URL 全部改为 `/api/v1/system/departments`：

- `GET /api/departments` → `GET /api/v1/system/departments`
- `GET /api/departments/{id}` → `GET /api/v1/system/departments/{id}`
- `POST /api/departments` → `POST /api/v1/system/departments`
- `PUT /api/departments` → `PUT /api/v1/system/departments`
- `DELETE /api/departments/{id}` → `DELETE /api/v1/system/departments/{id}`

---

## 缺口 #3：auth info 端点路径不匹配

### 问题

- 前端 `auth.js` 调用：`GET /api/v1/auth/user/info`
- 后端 AuthController：`@GetMapping("/info")` → 实际路径 `GET /api/v1/auth/info`

### 修复

改前端：`/api/v1/auth/user/info` → `/api/v1/auth/info`

---

## 缺口 #4：缺失的后端端点

### 4.1 ReaderController 缺少 4 个端点

| 端点                                     | 前端调用   | 实现方案                                |
| ---------------------------------------- | ---------- | --------------------------------------- |
| `DELETE /api/v1/readers/batch?ids=X,Y,Z` | readers.js | 循环调用已有的 deleteReader，收集结果   |
| `POST /api/v1/readers/{id}/card`         | readers.js | 更新读者卡状态+过期日期+押金            |
| `PUT /api/v1/readers/{id}/status`        | readers.js | 调用已有的 activateCard/suspendCard     |
| `GET /api/v1/readers/card/{cardNumber}`  | readers.js | 按 readerId 字段查询（readerId 即卡号） |

### 4.2 BorrowController 缺少 batch-return

| 端点                                | 前端调用       | 实现方案                                                                  |
| ----------------------------------- | -------------- | ------------------------------------------------------------------------- |
| `POST /api/v1/borrows/batch-return` | circulation.js | 委托给已有的 FineController.batchReturn，或在 BorrowController 中添加转发 |

### 4.3 ReserveController 缺少 notify

| 端点                                | 前端调用       | 实现方案                                    |
| ----------------------------------- | -------------- | ------------------------------------------- |
| `POST /api/v1/reserves/{id}/notify` | circulation.js | 更新预约记录的 notifyCount + notifySentDate |

---

## 修改文件清单

### 网关

| 文件                                                         | 操作          |
| ------------------------------------------------------------ | ------------- |
| `backend/gateway-service/src/main/resources/application.yml` | 添加 6 条路由 |

### 前端

| 文件                              | 操作               |
| --------------------------------- | ------------------ |
| `web-admin/src/api/department.js` | 修复 5 个 URL 路径 |
| `web-admin/src/api/auth.js`       | 修复 info 端点路径 |

### 后端

| 文件                                                                | 操作                   |
| ------------------------------------------------------------------- | ---------------------- |
| `backend/reader-service/.../controller/ReaderController.java`       | 添加 4 个端点          |
| `backend/circulation-service/.../controller/BorrowController.java`  | 添加 batch-return 端点 |
| `backend/circulation-service/.../controller/ReserveController.java` | 添加 notify 端点       |
| 对应 Service 接口和实现                                             | 添加方法声明和实现     |
| 对应测试文件                                                        | TDD — 先写测试再实现   |
