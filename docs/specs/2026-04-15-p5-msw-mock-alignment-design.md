# P5: MSW Mock 与前端 API 契约对齐

**日期：** 2026-04-15
**状态：** Approved
**优先级：** P5（前后端契约防漂移）
**背景：** 前端 94 个 API URL，MSW 仅 64 个 handler URL；system/chat/department 三个模块完全无 mock。生产前需补齐覆盖并建立自动化校验防回归。

---

## 目标

1. 新建 3 个缺失 handler 文件（system、chat、department）
2. 补齐现有 handler 文件中缺失端点
3. 写覆盖率校验脚本 `check-mock-coverage.mjs`，输出 missing/orphan/method-mismatch 报告
4. 注册到 `npm run mock:check`，可选接入 CI

---

## MSW Handler 模式

```javascript
// src/mock/handlers/system.js
import { http, HttpResponse } from "msw";

const users = [
  {
    id: 1,
    username: "admin",
    realName: "管理员",
    status: 1,
    email: "admin@x.com",
  },
  // ...
];

export const systemHandlers = [
  // 列表（分页）
  http.get("/api/v1/system/users", ({ request }) => {
    const url = new URL(request.url);
    const pageNum = Number(url.searchParams.get("pageNum") || 1);
    const pageSize = Number(url.searchParams.get("pageSize") || 10);
    return HttpResponse.json({
      code: 200,
      message: "success",
      data: {
        records: users.slice((pageNum - 1) * pageSize, pageNum * pageSize),
        total: users.length,
        pageNum,
        pageSize,
      },
    });
  }),

  // 详情
  http.get("/api/v1/system/users/:id", ({ params }) => {
    const user = users.find((u) => u.id === Number(params.id));
    return user
      ? HttpResponse.json({ code: 200, data: user })
      : HttpResponse.json({ code: 404, message: "Not found" }, { status: 404 });
  }),

  // 写操作
  http.post("/api/v1/system/users", async ({ request }) => {
    const body = await request.json();
    const newUser = { id: users.length + 1, ...body };
    users.push(newUser);
    return HttpResponse.json({ code: 200, message: "success", data: newUser });
  }),
];
```

### 响应约定（与后端 `Result<T>` 对齐）

- 列表：`{ code: 200, data: { records, total, pageNum, pageSize } }`
- 详情：`{ code: 200, data: {...} }`
- 写操作：`{ code: 200, message: 'success' }` 或 `{ code: 200, data: <id> }`
- 错误：`{ code: 4xx/5xx, message: '...' }`（带对应 HTTP status）

---

## 三个新 handler 文件

| 文件                     | 主要端点（基于 `src/api/*.js`）                                                                                                   |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| `handlers/system.js`     | `/api/v1/system/users/*`、`/system/roles/*`、`/system/permissions/*`、`/system/departments/*`                                     |
| `handlers/chat.js`       | `/api/v1/chat/message`、`/chat/history/:sessionId`、`/chat/feedback`、`/chat/hot-questions`、`/chat/stats`、`/chat/cache/refresh` |
| `handlers/department.js` | `/api/v1/departments`、`/departments/:id`（CRUD）                                                                                 |

修改 `src/mock/browser.js` 引入这 3 个 handler 数组。

---

## 覆盖率校验脚本

**新建：** `web-admin/scripts/check-mock-coverage.mjs`

### 输入

- `web-admin/src/api/*.js` — 提取所有 `request({ url, method })` 调用
- `web-admin/src/mock/handlers/*.js` — 提取所有 `http.get/post/put/delete/patch(url, ...)` 注册

### 算法

1. **API 提取：** 正则匹配 `url:\s*['"`]([^'"`]+)['"`]`与`method:\s\*['"]([^'"]+)['"]`。模板字符串 `${id}`归一化为`:id`。
2. **Handler 提取：** 正则匹配 `http\.(get|post|put|delete|patch)\(['"`]([^'"`]+)['"`]`。
3. **比对：** 用 `${METHOD} ${URL}` 字符串作 key，求差集。
4. **输出三类：**
   - **Missing**: 前端有调用、MSW 无 handler（红）
   - **Orphan**: MSW 有 handler、前端无调用（黄）
   - **Method mismatch**: 同 URL 不同 method（红）
5. **退出码：** missing 或 method-mismatch 非空 → exit 1；否则 exit 0。

### 输出格式

```
=== MSW Mock Coverage Report ===
Frontend API endpoints: 94
MSW handlers:           105

❌ Missing handlers (3):
  GET    /api/v1/circulation/overdue           (used by api/circulation.js)
  POST   /api/v1/chat/feedback                 (used by api/chat.js)
  DELETE /api/v1/system/users/:id              (used by api/system.js)

⚠ Orphan handlers (1):
  GET    /api/v1/books/legacy/list             (handler in books.js, no frontend caller)

✓ Method matches: all good

Coverage: 91/94 = 96.8%
```

### 已知限制（明确写入脚本注释）

- 不解析动态拼接的 URL（`/api/v1/${prefix}/foo`）— 跳过并 warn
- 不验证响应 schema（仅 URL+method）— schema 校验留给未来扩展

---

## NPM 脚本

`web-admin/package.json`：

```json
"scripts": {
  "mock:check": "node scripts/check-mock-coverage.mjs"
}
```

CI 可选：把 `npm run mock:check` 加到 frontend lint workflow，missing 阻塞合并。

---

## 文件修改清单

### 新建

| 文件                                        | 用途                                  |
| ------------------------------------------- | ------------------------------------- |
| `web-admin/scripts/check-mock-coverage.mjs` | 覆盖率校验脚本                        |
| `web-admin/src/mock/handlers/system.js`     | 25+ 端点（user/role/permission/dept） |
| `web-admin/src/mock/handlers/chat.js`       | ~6 端点                               |
| `web-admin/src/mock/handlers/department.js` | ~5 端点 CRUD                          |

### 修改

| 文件                                                                 | 变更                              |
| -------------------------------------------------------------------- | --------------------------------- |
| `web-admin/src/mock/browser.js`                                      | import 并合并 3 个新 handler 数组 |
| `web-admin/src/mock/handlers/circulation.js`                         | 补 ~9 个缺失端点                  |
| `web-admin/src/mock/handlers/analytics.js`                           | 补 ~3 个缺失端点                  |
| `web-admin/src/mock/handlers/inventory.js`                           | 补 ~1 个缺失端点（视报告）        |
| `web-admin/src/mock/handlers/books.js`、`readers.js`、`recommend.js` | 视脚本报告补齐                    |
| `web-admin/package.json`                                             | 添加 `mock:check` script          |

实际缺口由 `check-mock-coverage` 首次运行的报告确定。

---

## 验证

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm run mock:check
```

**预期最终状态：** Missing 0、Method mismatch 0、Coverage = 100%（Orphan 可保留为 known acceptable，不阻断）。

---

## 执行策略

### Phase 1 — 基线脚本

- **T1**：写 `check-mock-coverage.mjs`，添加 npm script，初次运行得到缺口报告

### Phase 2 — 并行补缺（基于 T1 报告）

- **T2**：新建 `handlers/system.js`
- **T3**：新建 `handlers/chat.js`
- **T4**：新建 `handlers/department.js`
- **T5**：补齐 `handlers/circulation.js`
- **T6**：补齐 `handlers/analytics.js` + `handlers/inventory.js`（其他视情况）
- **T7**：更新 `browser.js` 注册新 handlers

### Phase 3 — 收尾

- **T8**：重跑 `npm run mock:check`，确认 missing=0，提交最终结果

T1 必须先完成（其他依赖其报告），T2-T7 可并行，T8 串行最后。
