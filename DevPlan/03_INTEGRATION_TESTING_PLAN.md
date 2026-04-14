# GCRF图书馆管理系统 - 集成测试计划

**版本**: 1.0.0
**创建日期**: 2025-11-03
**测试目标**: 通过完整的集成测试确保前后端系统达到生产部署标准
**参考文档**: `01_SYSTEM_STATUS_ANALYSIS.md`, `02_API_INTEGRATION_STRATEGY.md`

---

## 📋 执行摘要

### 测试目标

本集成测试计划旨在:

1. **验证API契约**: 确保前端Mock API与后端实现100%一致
2. **端到端测试**: 验证完整业务流程 (登录→借书→归还)
3. **性能测试**: 确保核心API响应时间<500ms, 并发支持100用户
4. **安全测试**: 验证JWT认证、权限控制、SQL注入防护
5. **生产就绪**: 达到90%+测试覆盖,0个Critical Bug

### 测试范围

| 测试类型        | 覆盖范围        | 目标覆盖率 | 预计工作量 |
| --------------- | --------------- | ---------- | ---------- |
| **API契约测试** | 128个API端点    | 100%       | 1周        |
| **端到端测试**  | 10+核心业务流程 | 100%       | 2周        |
| **集成测试**    | 7个微服务交互   | 80%        | 1周        |
| **性能测试**    | 核心API (20+个) | 100%       | 1周        |
| **安全测试**    | 认证/授权/注入  | 100%       | 3天        |
| **总计**        | -               | -          | **5-6周**  |

---

## 🎯 第一部分: API契约测试

### 1.1 契约测试概述

**目标**: 验证前端Mock API与后端实现的响应格式、状态码、数据结构100%一致

**工具选型**: Spring Cloud Contract (推荐) / Pact

**测试方式**:

- **Consumer (前端)**: 定义期望的API契约
- **Provider (后端)**: 验证是否满足契约
- **自动化**: 集成到CI/CD Pipeline

---

### 1.2 契约测试清单

#### Auth Service (11个API)

| API端点                                  | 方法   | 契约要点             | 优先级 |
| ---------------------------------------- | ------ | -------------------- | ------ |
| `/api/v1/auth/login`                     | POST   | 返回token + 用户信息 | P0     |
| `/api/v1/auth/logout`                    | POST   | 清除Redis Session    | P0     |
| `/api/v1/auth/refresh`                   | POST   | 返回新token          | P0     |
| `/api/v1/auth/register`                  | POST   | 创建用户 + 返回ID    | P1     |
| `/api/v1/auth/users`                     | GET    | 分页返回用户列表     | P1     |
| `/api/v1/auth/users/{id}`                | GET    | 返回用户详情         | P1     |
| `/api/v1/auth/users`                     | POST   | 创建用户             | P1     |
| `/api/v1/auth/users/{id}`                | PUT    | 更新用户             | P1     |
| `/api/v1/auth/users/{id}`                | DELETE | 逻辑删除用户         | P1     |
| `/api/v1/auth/users/{id}/reset-password` | POST   | 重置密码             | P1     |
| `/api/v1/auth/health`                    | GET    | 返回健康状态         | P2     |

**契约示例** (login):

```yaml
# contracts/auth/login.yml
request:
  method: POST
  url: /api/v1/auth/login
  headers:
    Content-Type: application/json
  body:
    username: admin
    password: password123
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    code: 200
    message: "success"
    data:
      token: "$(anyNonBlankString())"
      userId: "$(anyPositiveInt())"
      username: "admin"
      userType: "ADMIN"
    timestamp: "$(iso8601WithOffset())"
```

---

#### Book Service (15个API)

| API端点                           | 方法   | 契约要点           | 优先级 |
| --------------------------------- | ------ | ------------------ | ------ |
| `/api/v1/books`                   | GET    | 分页 + 搜索 + 筛选 | P0     |
| `/api/v1/books/{id}`              | GET    | 返回图书详情       | P0     |
| `/api/v1/books`                   | POST   | 创建图书 + 返回ID  | P0     |
| `/api/v1/books/{id}`              | PUT    | 更新图书           | P0     |
| `/api/v1/books/{id}`              | DELETE | 逻辑删除           | P0     |
| `/api/v1/books/categories`        | GET    | 返回分类树         | P1     |
| `/api/v1/books/search`            | GET    | 全文检索           | P1     |
| `/api/v1/books/{id}/stock`        | PUT    | 更新库存           | P1     |
| `/api/v1/books/statistics`        | GET    | 统计数据           | P2     |
| `/api/v1/books/{id}/upload-cover` | POST   | 上传封面 (MinIO)   | P2     |
| `/api/v1/books/{id}/upload-file`  | POST   | 上传电子书         | P2     |
| `/api/v1/books/{id}/download`     | GET    | 下载电子书         | P2     |
| `/api/v1/books/import`            | POST   | 批量导入           | P2     |
| `/api/v1/books/export`            | GET    | 导出Excel          | P2     |
| `/api/v1/books/health`            | GET    | 健康检查           | P2     |

---

#### Circulation Service (17个API)

| API端点                                         | 方法   | 契约要点      | 优先级 |
| ----------------------------------------------- | ------ | ------------- | ------ |
| `/api/v1/circulation/borrow`                    | POST   | 借阅 + 扣库存 | P0     |
| `/api/v1/circulation/return/{recordId}`         | POST   | 归还 + 加库存 | P0     |
| `/api/v1/circulation/renew/{recordId}`          | POST   | 续借 + 延期   | P0     |
| `/api/v1/circulation/reader/{readerId}`         | GET    | 查询借阅记录  | P0     |
| `/api/v1/circulation/reserve`                   | POST   | 预约图书      | P1     |
| `/api/v1/circulation/reserve/{id}`              | DELETE | 取消预约      | P1     |
| `/api/v1/circulation/reserves`                  | GET    | 查询预约列表  | P1     |
| `/api/v1/circulation/overdue`                   | GET    | 查询逾期记录  | P1     |
| `/api/v1/circulation/overdue/{recordId}/notify` | POST   | 发送逾期通知  | P1     |
| `/api/v1/circulation/overdue/{recordId}/fine`   | POST   | 计算罚金      | P1     |
| `/api/v1/circulation/fine/{fineId}/pay`         | POST   | 支付罚金      | P1     |
| `/api/v1/circulation/statistics/daily`          | GET    | 每日统计      | P2     |
| `/api/v1/circulation/statistics/monthly`        | GET    | 月度统计      | P2     |
| `/api/v1/circulation/statistics/popular-books`  | GET    | 热门图书      | P2     |
| `/api/v1/circulation/statistics/reader-ranking` | GET    | 读者排名      | P2     |
| `/api/v1/circulation/batch-return`              | POST   | 批量归还      | P2     |
| `/api/v1/circulation/health`                    | GET    | 健康检查      | P2     |

---

#### Reader Service (20个API)

| API端点                               | 方法   | 契约要点                | 优先级 |
| ------------------------------------- | ------ | ----------------------- | ------ |
| `/api/v1/readers`                     | GET    | 分页 + 筛选             | P0     |
| `/api/v1/readers/{id}`                | GET    | 返回读者详情            | P0     |
| `/api/v1/readers/readerId/{readerId}` | GET    | 按读者证号查询          | P0     |
| `/api/v1/readers`                     | POST   | 创建读者 + 生成读者证号 | P0     |
| `/api/v1/readers/{id}`                | PUT    | 更新读者                | P0     |
| `/api/v1/readers/{id}`                | DELETE | 逻辑删除                | P0     |
| `/api/v1/readers/{id}/activate`       | POST   | 激活借书卡              | P0     |
| `/api/v1/readers/{id}/suspend`        | POST   | 挂失借书卡              | P0     |
| `/api/v1/readers/{id}/cancel`         | POST   | 注销借书卡              | P0     |
| `/api/v1/readers/types`               | GET    | 查询读者分类            | P1     |
| `/api/v1/readers/{id}/type`           | PUT    | 修改读者分类            | P1     |
| `/api/v1/readers/{id}/borrow-limit`   | PUT    | 设置借阅限额            | P1     |
| `/api/v1/readers/{id}/face-register`  | POST   | 注册人脸                | P2     |
| `/api/v1/readers/face-verify`         | POST   | 人脸验证                | P2     |
| `/api/v1/readers/{id}/borrow-history` | GET    | 借阅历史                | P2     |
| `/api/v1/readers/{id}/statistics`     | GET    | 读者统计                | P2     |
| `/api/v1/readers/statistics/by-type`  | GET    | 按类型统计              | P2     |
| `/api/v1/readers/import`              | POST   | 批量导入                | P2     |
| `/api/v1/readers/export`              | GET    | 导出数据                | P2     |
| `/api/v1/readers/health`              | GET    | 健康检查                | P2     |

---

#### System Service (40个API)

由于System Service API数量较多,这里列举核心API:

**部门管理** (7个):

- GET `/api/v1/system/departments` - 查询部门列表
- GET `/api/v1/system/departments/tree` - 查询部门树
- POST `/api/v1/system/departments` - 创建部门
- PUT `/api/v1/system/departments/{id}` - 更新部门
- DELETE `/api/v1/system/departments/{id}` - 删除部门

**角色管理** (8个):

- GET `/api/v1/system/roles` - 查询角色列表
- POST `/api/v1/system/roles` - 创建角色
- POST `/api/v1/system/roles/{id}/permissions` - 分配权限

**权限管理** (7个):

- GET `/api/v1/system/permissions/tree` - 查询权限树
- GET `/api/v1/system/permissions/user/{userId}` - 查询用户权限

**日志管理** (11个):

- GET `/api/v1/system/operation-logs` - 查询操作日志
- GET `/api/v1/system/login-logs` - 查询登录日志
- GET `/api/v1/system/login-logs/statistics` - 登录统计

**其他** (7个):

- GET `/api/v1/system/menus/tree` - 查询菜单树
- GET `/api/v1/system/menus/user/{userId}` - 查询用户菜单
- GET `/api/v1/system/health` - 健康检查

---

#### Notification Service (25个API)

**通知管理** (9个):

- GET `/api/v1/notifications` - 查询通知列表
- POST `/api/v1/notifications` - 创建通知
- POST `/api/v1/notifications/{id}/read` - 标记已读
- GET `/api/v1/notifications/unread-count` - 未读数量

**邮件通知** (5个):

- POST `/api/v1/notifications/email/send` - 发送邮件
- GET `/api/v1/notifications/email/logs` - 邮件日志

**短信通知** (5个):

- POST `/api/v1/notifications/sms/send` - 发送短信
- GET `/api/v1/notifications/sms/logs` - 短信日志

**模板管理** (6个):

- GET `/api/v1/notifications/templates` - 查询模板列表
- POST `/api/v1/notifications/templates` - 创建模板

---

### 1.3 契约测试实施

#### Spring Cloud Contract集成

**1. 添加依赖** (各服务pom.xml):

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-verifier</artifactId>
    <scope>test</scope>
</dependency>
```

**2. 配置插件**:

```xml
<plugin>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-contract-maven-plugin</artifactId>
    <version>4.1.0</version>
    <extensions>true</extensions>
    <configuration>
        <testFramework>JUNIT5</testFramework>
        <packageWithBaseClasses>com.gcrf.library.auth.contract</packageWithBaseClasses>
    </configuration>
</plugin>
```

**3. 定义契约** (src/test/resources/contracts/):

```
contracts/
├── auth/
│   ├── login.yml
│   ├── logout.yml
│   └── refresh.yml
├── books/
│   ├── query-books.yml
│   ├── get-book.yml
│   └── create-book.yml
└── ...
```

**4. 生成测试**: `mvn clean test` → 自动生成契约测试类

**5. 前端验证**: 使用生成的stub服务器验证前端

---

### 1.4 契约测试执行计划

| 阶段       | 任务                                 | 预计时间 | 负责人   |
| ---------- | ------------------------------------ | -------- | -------- |
| **Week 1** | Auth + Book Service契约测试          | 2天      | 后端开发 |
| **Week 1** | Circulation + Reader Service契约测试 | 2天      | 后端开发 |
| **Week 2** | System Service契约测试               | 3天      | 后端开发 |
| **Week 2** | 集成到CI/CD Pipeline                 | 2天      | DevOps   |

---

## 🔄 第二部分: 端到端测试 (E2E Testing)

### 2.1 E2E测试概述

**目标**: 验证完整业务流程在真实环境中的正确性

**工具选型**: Playwright (推荐) / Cypress

**测试环境**:

- 后端: Docker Compose (所有微服务)
- 前端: Vite Dev Server / 生产构建
- 数据库: PostgreSQL测试数据

---

### 2.2 核心业务流程测试

#### 流程1: 用户登录与权限验证 (P0)

**测试步骤**:

1. 访问登录页 `http://localhost:3011/login`
2. 输入用户名密码 (`admin` / `password123`)
3. 点击登录按钮
4. 验证: 跳转到Dashboard, localStorage存储token
5. 验证: 左侧菜单显示正确 (根据用户权限)
6. 点击登出
7. 验证: token被清除, 跳转回登录页

**验收标准**:

- ✅ 登录成功后获得token
- ✅ Dashboard数据正常加载
- ✅ 菜单权限正确
- ✅ 登出清除session

**Playwright示例**:

```javascript
// e2e/auth/login.spec.js
import { test, expect } from "@playwright/test";

test("用户登录流程", async ({ page }) => {
  // 1. 访问登录页
  await page.goto("http://localhost:3011/login");

  // 2. 输入用户名密码
  await page.fill('input[name="username"]', "admin");
  await page.fill('input[name="password"]', "password123");

  // 3. 点击登录
  await page.click('button[type="submit"]');

  // 4. 验证跳转到Dashboard
  await expect(page).toHaveURL("http://localhost:3011/dashboard");

  // 5. 验证token存储
  const token = await page.evaluate(() => localStorage.getItem("access_token"));
  expect(token).toBeTruthy();

  // 6. 验证菜单显示
  await expect(page.locator("text=图书管理")).toBeVisible();
  await expect(page.locator("text=流通管理")).toBeVisible();

  // 7. 登出
  await page.click('button:has-text("登出")');
  await expect(page).toHaveURL("http://localhost:3011/login");
});
```

---

#### 流程2: 图书管理 - 创建图书 (P0)

**测试步骤**:

1. 登录系统
2. 进入图书管理页面
3. 点击"新增图书"按钮
4. 填写表单 (ISBN, 标题, 作者, 出版社, 库存等)
5. 点击"保存"
6. 验证: 图书列表显示新图书
7. 验证: 后端数据库插入成功

**验收标准**:

- ✅ 表单验证正确 (ISBN格式, 必填字段)
- ✅ 创建成功提示
- ✅ 列表刷新显示新图书
- ✅ 数据库记录存在

---

#### 流程3: 完整借阅流程 (P0 - 核心流程)

**测试步骤**:

1. 登录系统
2. 进入流通管理 → 图书借阅
3. 搜索图书 (输入书名或ISBN)
4. 选择图书
5. 扫描/输入读者证号
6. 确认借阅
7. 验证: 借阅成功提示
8. 验证: 图书库存减1
9. 验证: 借阅记录创建
10. 进入图书归还
11. 扫描/输入借阅记录ID
12. 确认归还
13. 验证: 归还成功提示
14. 验证: 图书库存加1
15. 验证: 借阅记录状态更新为"已归还"

**验收标准**:

- ✅ 借阅扣库存正确
- ✅ 归还加库存正确
- ✅ 借阅记录状态正确
- ✅ 通知发送 (可选)

**Playwright示例**:

```javascript
// e2e/circulation/borrow-return.spec.js
import { test, expect } from "@playwright/test";

test("完整借还书流程", async ({ page }) => {
  // 1. 登录
  await page.goto("http://localhost:3011/login");
  await page.fill('input[name="username"]', "admin");
  await page.fill('input[name="password"]', "password123");
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/dashboard/);

  // 2. 进入借阅页面
  await page.click("text=流通管理");
  await page.click("text=图书借阅");
  await expect(page).toHaveURL(/circulation\/borrow/);

  // 3. 搜索图书
  await page.fill('input[placeholder*="搜索"]', "深入理解计算机系统");
  await page.press('input[placeholder*="搜索"]', "Enter");
  await page.waitForTimeout(1000);

  // 4. 选择图书
  const bookRow = page.locator('tr:has-text("深入理解计算机系统")').first();
  const initialStock = await bookRow.locator("td:nth-child(6)").innerText();
  await bookRow.locator('button:has-text("借阅")').click();

  // 5. 输入读者证号
  await page.fill('input[placeholder*="读者证号"]', "R2025001");
  await page.click('button:has-text("确认借阅")');

  // 6. 验证借阅成功
  await expect(page.locator(".el-message--success")).toContainText("借阅成功");

  // 7. 验证库存减1
  await page.reload();
  const newStock = await bookRow.locator("td:nth-child(6)").innerText();
  expect(parseInt(newStock)).toBe(parseInt(initialStock) - 1);

  // 8. 进入归还页面
  await page.click("text=图书归还");
  await expect(page).toHaveURL(/circulation\/return/);

  // 9. 扫描借阅记录ID (假设自动填充)
  await page.fill('input[placeholder*="借阅记录"]', "latest"); // 或实际ID
  await page.click('button:has-text("查询")');

  // 10. 确认归还
  await page.click('button:has-text("确认归还")');

  // 11. 验证归还成功
  await expect(page.locator(".el-message--success")).toContainText("归还成功");

  // 12. 验证库存恢复
  await page.click("text=图书借阅");
  await page.fill('input[placeholder*="搜索"]', "深入理解计算机系统");
  await page.press('input[placeholder*="搜索"]', "Enter");
  const finalStock = await bookRow.locator("td:nth-child(6)").innerText();
  expect(parseInt(finalStock)).toBe(parseInt(initialStock));
});
```

---

#### 流程4: 读者管理 - 完整生命周期 (P1)

**测试步骤**:

1. 登录系统
2. 进入读者管理
3. 创建新读者 (学生)
4. 验证: 自动生成读者证号
5. 激活借书卡
6. 验证: 卡状态为"激活"
7. 挂失借书卡
8. 验证: 卡状态为"挂失", 无法借阅
9. 注销借书卡
10. 验证: 卡状态为"注销"

---

#### 流程5: 系统管理 - 角色权限 (P1)

**测试步骤**:

1. 以管理员登录
2. 创建新角色 "图书管理员"
3. 分配权限: 图书管理 (增删改查)
4. 创建新用户 "librarian1"
5. 分配角色 "图书管理员"
6. 登出管理员
7. 以"librarian1"登录
8. 验证: 仅显示图书管理菜单
9. 尝试访问系统管理
10. 验证: 403权限不足

---

### 2.3 E2E测试清单

| 优先级   | 测试场景             | 流程步骤 | 预计时间  |
| -------- | -------------------- | -------- | --------- |
| **P0**   | 用户登录与登出       | 7步      | 0.5天     |
| **P0**   | 图书创建与查询       | 8步      | 0.5天     |
| **P0**   | 完整借还书流程       | 15步     | 1天       |
| **P0**   | 图书续借流程         | 8步      | 0.5天     |
| **P1**   | 读者生命周期管理     | 10步     | 1天       |
| **P1**   | 图书预约流程         | 8步      | 0.5天     |
| **P1**   | 逾期处理流程         | 10步     | 1天       |
| **P1**   | 角色权限验证         | 10步     | 1天       |
| **P2**   | 批量操作 (导入/导出) | 8步      | 1天       |
| **P2**   | 统计报表生成         | 6步      | 0.5天     |
| **总计** | 10个场景             | -        | **7-8天** |

---

### 2.4 E2E测试环境搭建

#### Docker Compose测试环境

**配置文件**: `deployment/docker-compose.test.yml`

```yaml
version: "3.8"

services:
  # PostgreSQL测试数据库
  postgres-test:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: test_password
    ports:
      - "5433:5432"
    volumes:
      - ./test-data/init.sql:/docker-entrypoint-initdb.d/init.sql

  # Redis
  redis-test:
    image: redis:7
    ports:
      - "6380:6379"

  # Nacos
  nacos-test:
    image: nacos/nacos-server:v2.3.0
    environment:
      MODE: standalone
    ports:
      - "8849:8848"

  # Gateway
  gateway-test:
    image: gcrf-gateway:test
    environment:
      SPRING_PROFILES_ACTIVE: test
    ports:
      - "8080:8080"
    depends_on:
      - nacos-test
      - redis-test

  # 其他微服务...
```

**启动测试环境**:

```bash
# 构建测试镜像
docker-compose -f docker-compose.test.yml build

# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 初始化测试数据
./test-data/seed-data.sh

# 运行E2E测试
npm run test:e2e

# 停止测试环境
docker-compose -f docker-compose.test.yml down
```

---

## ⚡ 第三部分: 性能测试

### 3.1 性能测试目标

| 指标          | 目标值      | 说明                   |
| ------------- | ----------- | ---------------------- |
| **响应时间**  | P95 < 500ms | 95%的请求在500ms内完成 |
| **并发用户**  | 100         | 支持100个并发用户      |
| **TPS**       | 500+        | 每秒处理500+个事务     |
| **错误率**    | < 0.1%      | 错误率低于0.1%         |
| **CPU使用率** | < 70%       | CPU使用率不超过70%     |
| **内存使用**  | < 80%       | 内存使用不超过80%      |

---

### 3.2 性能测试场景

#### 场景1: 登录并发测试

**目标**: 验证100个用户同时登录系统的性能

**JMeter配置**:

- 线程数: 100
- Ramp-Up时间: 10秒 (每秒10个用户)
- 循环次数: 10
- 总请求数: 1000

**测试步骤**:

1. POST `/api/v1/auth/login`
2. 验证返回token
3. 使用token GET `/api/v1/auth/users` (验证token有效)

**验收标准**:

- ✅ P95响应时间 < 300ms
- ✅ 错误率 < 0.1%
- ✅ TPS > 100

---

#### 场景2: 图书查询性能测试

**目标**: 验证分页查询图书的性能

**配置**:

- 并发用户: 50
- 测试时间: 5分钟
- 数据量: 10万条图书记录

**测试步骤**:

1. 随机pageNum (1-1000)
2. GET `/api/v1/books?pageNum={random}&pageSize=20`
3. 验证返回20条记录

**验收标准**:

- ✅ P95响应时间 < 500ms
- ✅ TPS > 200
- ✅ 数据库连接池不耗尽

---

#### 场景3: 借还书高并发测试

**目标**: 验证借还书核心流程的并发性能

**配置**:

- 并发用户: 100
- 测试时间: 10分钟
- 场景: 50%借书, 50%还书

**测试步骤**:

1. 50个用户执行借书流程
2. 50个用户执行还书流程
3. 验证库存一致性

**验收标准**:

- ✅ P95响应时间 < 800ms
- ✅ 错误率 < 0.1%
- ✅ 库存数据一致 (无超卖)

---

### 3.3 性能测试工具

**推荐工具**: JMeter / k6

**JMeter脚本示例** (`performance/login-test.jmx`):

```xml
<jmeterTestPlan>
  <hashTree>
    <ThreadGroup>
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">10</stringProp>
      <intProp name="ThreadGroup.loop_count">10</intProp>
    </ThreadGroup>
    <HTTPSamplerProxy>
      <stringProp name="HTTPSampler.domain">localhost</stringProp>
      <stringProp name="HTTPSampler.port">8080</stringProp>
      <stringProp name="HTTPSampler.path">/api/v1/auth/login</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
      <elementProp name="HTTPsampler.Arguments">
        <stringProp name="Argument.value">
          {"username":"admin","password":"password123"}
        </stringProp>
      </elementProp>
    </HTTPSamplerProxy>
  </hashTree>
</jmeterTestPlan>
```

**k6脚本示例** (`performance/login-test.js`):

```javascript
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 50 }, // Ramp-up
    { duration: "2m", target: 100 }, // Peak
    { duration: "30s", target: 0 }, // Ramp-down
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"], // P95 < 500ms
    http_req_failed: ["rate<0.01"], // 错误率 < 1%
  },
};

export default function () {
  const payload = JSON.stringify({
    username: "admin",
    password: "password123",
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  const res = http.post(
    "http://localhost:8080/api/v1/auth/login",
    payload,
    params,
  );

  check(res, {
    "status is 200": (r) => r.status === 200,
    "response has token": (r) => r.json().data.token !== undefined,
  });

  sleep(1);
}
```

---

### 3.4 性能优化建议

**数据库优化**:

- ✅ 添加索引 (书名, ISBN, 读者证号)
- ✅ 查询优化 (避免N+1)
- ✅ 连接池配置 (HikariCP)
- ✅ 分页查询优化 (延迟加载)

**缓存优化**:

- ✅ Redis缓存热点数据 (图书信息)
- ✅ 本地缓存 (Caffeine)
- ✅ CDN缓存静态资源

**应用优化**:

- ✅ 异步处理 (通知发送)
- ✅ 批量操作 (批量导入)
- ✅ 限流熔断 (Sentinel)

---

## 🔒 第四部分: 安全测试

### 4.1 认证测试

| 测试项          | 测试方法                 | 验收标准    |
| --------------- | ------------------------ | ----------- |
| **Token过期**   | 等待24小时后访问API      | 返回401     |
| **Token篡改**   | 修改Token内容            | 返回401     |
| **无Token访问** | 不带Authorization Header | 返回401     |
| **Token刷新**   | 调用refresh接口          | 返回新Token |

---

### 4.2 授权测试

| 测试项         | 测试方法             | 验收标准 |
| -------------- | -------------------- | -------- |
| **角色权限**   | 普通用户访问管理功能 | 返回403  |
| **资源所有权** | 用户A删除用户B的数据 | 返回403  |
| **越权访问**   | 跳过前端直接调用API  | 返回403  |

---

### 4.3 SQL注入测试

**测试用例**:

```bash
# 登录SQL注入测试
POST /api/v1/auth/login
{
  "username": "admin' OR '1'='1",
  "password": "anything"
}
# 预期: 登录失败, 不返回数据

# 图书查询SQL注入测试
GET /api/v1/books?keyword=test' UNION SELECT * FROM users --
# 预期: 参数验证失败或安全查询
```

**验收标准**:

- ✅ 使用PreparedStatement (MyBatis Plus自动防护)
- ✅ 输入验证 (`@Valid` + JSR-303)
- ✅ 不返回敏感错误信息

---

### 4.4 XSS测试

**测试用例**:

```javascript
// 创建图书时注入XSS
POST /api/v1/books
{
  "title": "<script>alert('XSS')</script>",
  "author": "Test"
}
// 预期: HTML转义后存储

// 查询图书
GET /api/v1/books
// 预期: 前端显示转义后的文本, 不执行脚本
```

**验收标准**:

- ✅ 后端存储前转义
- ✅ 前端显示时转义 (Vue默认)
- ✅ CSP (Content Security Policy) 配置

---

## 📊 第五部分: 测试报告与持续集成

### 5.1 测试报告模板

**测试执行报告** (每次测试后生成):

```markdown
# GCRF系统集成测试报告

**测试日期**: 2025-11-10
**测试版本**: v1.0.0-RC1
**测试环境**: Docker Compose (本地)

## 测试概览

| 测试类型    | 总数    | 通过    | 失败  | 跳过  | 通过率    |
| ----------- | ------- | ------- | ----- | ----- | --------- |
| API契约测试 | 128     | 125     | 3     | 0     | 97.7%     |
| E2E测试     | 10      | 9       | 1     | 0     | 90%       |
| 性能测试    | 5       | 5       | 0     | 0     | 100%      |
| 安全测试    | 8       | 8       | 0     | 0     | 100%      |
| **总计**    | **151** | **147** | **4** | **0** | **97.4%** |

## 失败用例

### 1. API契约测试失败 (3个)

- `POST /api/v1/circulation/borrow` - 响应格式不一致
- `GET /api/v1/system/roles` - 分页参数缺失
- `GET /api/v1/books/statistics` - 统计数据字段缺失

### 2. E2E测试失败 (1个)

- 完整借还书流程 - 库存更新延迟导致验证失败

## 修复建议

1. 修复3个API契约不一致问题
2. 优化库存更新机制 (使用事务)
3. 补充缺失的统计API

## 下次测试计划

**时间**: 2025-11-15
**重点**: 修复失败用例后回归测试
```

---

### 5.2 CI/CD集成

**GitHub Actions配置** (`.github/workflows/integration-test.yml`):

```yaml
name: Integration Test

on:
  pull_request:
    branches: [develop, main]
  push:
    branches: [develop]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Java 21
        uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Start test environment
        run: |
          docker-compose -f deployment/docker-compose.test.yml up -d
          sleep 30  # Wait for services to be ready

      - name: Run API contract tests
        run: |
          cd backend
          mvn clean test -Dtest=**/*ContractTest

      - name: Run E2E tests
        run: |
          cd web-admin
          npm install
          npm run test:e2e

      - name: Run performance tests (k6)
        run: |
          k6 run performance/login-test.js

      - name: Generate test report
        run: |
          ./scripts/generate-test-report.sh

      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: test-results/

      - name: Stop test environment
        if: always()
        run: |
          docker-compose -f deployment/docker-compose.test.yml down
```

---

## 🎯 第六部分: 测试执行计划

### 6.1 Phase 1: API契约测试 (Week 1)

**任务**:

- [ ] 为128个API端点创建契约定义
- [ ] 集成Spring Cloud Contract到各服务
- [ ] 运行契约测试并修复不一致问题
- [ ] 达到100%契约一致性

**交付物**:

- 128个契约定义文件 (`.yml`)
- 契约测试报告
- 修复后的API代码

---

### 6.2 Phase 2: E2E测试 (Week 2-3)

**任务**:

- [ ] 搭建Playwright测试环境
- [ ] 编写10+个E2E测试场景
- [ ] 运行E2E测试并修复Bug
- [ ] 达到90%+ E2E覆盖

**交付物**:

- 10个E2E测试脚本 (`.spec.js`)
- E2E测试报告
- Bug修复记录

---

### 6.3 Phase 3: 性能测试 (Week 4)

**任务**:

- [ ] 配置JMeter/k6测试脚本
- [ ] 执行性能基准测试
- [ ] 识别性能瓶颈
- [ ] 优化并达到性能目标

**交付物**:

- 性能测试报告
- 性能优化建议
- 优化后的代码

---

### 6.4 Phase 4: 安全测试 (Week 4)

**任务**:

- [ ] 执行认证授权测试
- [ ] 执行SQL注入测试
- [ ] 执行XSS测试
- [ ] 修复安全漏洞

**交付物**:

- 安全测试报告
- 安全漏洞修复记录

---

### 6.5 Phase 5: CI/CD集成 (Week 5)

**任务**:

- [ ] 集成所有测试到GitHub Actions
- [ ] 配置测试报告自动生成
- [ ] 配置测试失败通知
- [ ] 验证CI/CD流程

**交付物**:

- CI/CD配置文件
- 自动化测试Pipeline

---

## 📋 总结

### 测试覆盖总览

| 测试类型    | 覆盖范围       | 目标      | 工具                  | 预计工作量 |
| ----------- | -------------- | --------- | --------------------- | ---------- |
| **API契约** | 128个端点      | 100%      | Spring Cloud Contract | 1周        |
| **E2E**     | 10+核心流程    | 90%       | Playwright            | 2周        |
| **性能**    | 核心API        | P95<500ms | JMeter/k6             | 1周        |
| **安全**    | 认证/授权/注入 | 0漏洞     | 手动+工具             | 3天        |
| **CI/CD**   | 自动化Pipeline | 100%      | GitHub Actions        | 2天        |
| **总计**    | -              | -         | -                     | **5-6周**  |

### 成功标准

达到生产部署标准需满足:

- ✅ API契约一致性 100%
- ✅ E2E测试通过率 > 95%
- ✅ 性能测试达标 (P95 < 500ms, 并发100)
- ✅ 安全测试无Critical漏洞
- ✅ CI/CD自动化运行

### 下一步

1. **立即执行**: 创建API契约测试 (Week 1)
2. **并行开发**: 完成后端API (Week 1-4)
3. **集成测试**: E2E + 性能 + 安全 (Week 2-5)
4. **生产上线**: 达标后部署 (Week 6)

---

**创建人**: Claude Code Agent
**创建日期**: 2025-11-03
**下一步**: 创建生产部署路线图 (`04_PRODUCTION_DEPLOYMENT_ROADMAP.md`)
