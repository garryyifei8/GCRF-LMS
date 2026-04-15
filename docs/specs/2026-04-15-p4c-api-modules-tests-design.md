# P4C: 10 个 API 模块单元测试设计

**日期：** 2026-04-15
**状态：** Approved
**优先级：** P4C（前端 API 层测试）
**背景：** P4A 已覆盖 stores + utils（含 request.js 拦截器），P4B 已覆盖 11 个 Vue 组件。P4C 为 `web-admin/src/api/` 下 10 个 API 模块的所有导出函数补齐调用形状测试。

---

## 目标

为 10 个 API 文件的所有导出函数各写 1 个测试，验证：

1. URL 正确（含路径参数插值）
2. HTTP method 正确
3. params/data 透传正确

**唯一目的：** 防止后端端点重命名后前端调用未同步、参数键名漂移、HTTP method 误改等签名级回归。

不在范围内：响应处理、错误处理、interceptor 行为（已在 P4A `request.test.js` 中覆盖）。

---

## 测试模式

### 标准模板

```javascript
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("@/utils/request", () => ({
  default: vi.fn(() => Promise.resolve({ code: 200, data: {} })),
}));

import request from "@/utils/request";
import { getReaders, getReaderById, createReader } from "@/api/readers";

describe("api/readers", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getReaders_shouldCallCorrectEndpoint", () => {
    const params = { pageNum: 1, pageSize: 10 };
    getReaders(params);
    expect(request).toHaveBeenCalledWith({
      url: "/api/v1/readers",
      method: "get",
      params,
    });
  });

  it("getReaderById_shouldInterpolateId", () => {
    getReaderById(42);
    expect(request).toHaveBeenCalledWith({
      url: "/api/v1/readers/42",
      method: "get",
    });
  });

  it("createReader_shouldPostToReaders", () => {
    const data = {
      realName: "张三",
      phone: "13800000000",
      readerType: "student",
    };
    createReader(data);
    expect(request).toHaveBeenCalledWith({
      url: "/api/v1/readers",
      method: "post",
      data,
    });
  });
});
```

### 关键约定

- `vi.mock('@/utils/request')` 顶层 hoist，避免真实 HTTP
- mock 默认返回 `Promise.resolve({ code: 200, data: {} })`
- 每个测试 `beforeEach(vi.clearAllMocks)`
- 用 `toHaveBeenCalledWith({ url, method, params/data })` 严格断言完整对象形状
- 路径参数（`/{id}`）必须断言插值后的字面字符串
- 测试名约定：`<functionName>_<scenario>_<expected>`，简单 GET 可省略 scenario

---

## 10 个测试文件

### 1. auth.test.js

**路径：** `web-admin/src/api/__tests__/auth.test.js`

| #   | 函数       | 测试                                |
| --- | ---------- | ----------------------------------- |
| 1   | `login`    | `login_shouldPostToAuthLogin`       |
| 2   | `register` | `register_shouldPostToAuthRegister` |
| 3   | `logout`   | `logout_shouldPostToAuthLogout`     |

### 2. readers.test.js

**路径：** `web-admin/src/api/__tests__/readers.test.js`

每个导出函数 1 个测试，覆盖：

- `getReaders` — GET 列表带 params
- `getReaderById` — GET 路径参数插值
- `createReader` — POST data
- `updateReader` — PUT 路径 + data
- `deleteReader` — DELETE 路径
- `batchDeleteReaders` — DELETE batch
- `issueCard` / `updateReaderStatus` / `getReaderByCardNumber` 等其余导出

实际数量以源码导出列表为准，约 ~7 个。

### 3. books.test.js

**路径：** `web-admin/src/api/__tests__/books.test.js`

约 ~10 个函数：列表/详情/CRUD/分类/库存查询/借阅历史/导入导出等。每个 1 个测试。

### 4. circulation.test.js

**路径：** `web-admin/src/api/__tests__/circulation.test.js`

约 ~10 个函数：借书/还书/续借/批量还书/预约/取消预约/罚款/借阅历史/规则等。每个 1 个测试。

### 5. system.test.js

**路径：** `web-admin/src/api/__tests__/system.test.js`

约 ~15 个函数（最大文件）：用户管理/角色/菜单/权限/部门/字典/操作日志/系统配置/登录日志等。每个 1 个测试。

### 6. analytics.test.js

**路径：** `web-admin/src/api/__tests__/analytics.test.js`

约 ~9 个函数：仪表板/借阅统计/读者统计/热门图书/趋势分析/报表导出等。每个 1 个测试。

### 7. inventory.test.js

**路径：** `web-admin/src/api/__tests__/inventory.test.js`

约 ~9 个函数：盘点/采购/调拨/损耗/入库/出库等。每个 1 个测试。

### 8. recommend.test.js

**路径：** `web-admin/src/api/__tests__/recommend.test.js`

约 ~5 个函数：个性化推荐/热门推荐/相似图书/收藏/反馈等。每个 1 个测试。

### 9. chat.test.js

**路径：** `web-admin/src/api/__tests__/chat.test.js`

约 ~3 个函数：发送消息/历史/会话列表等。每个 1 个测试。

### 10. department.test.js

**路径：** `web-admin/src/api/__tests__/department.test.js`

约 ~3 个函数：部门列表/创建/更新等。每个 1 个测试。

---

## 测试总数

约 **70-80 个测试**。

---

## 实现注意事项

### 路径参数断言

源码：

```javascript
export function getReaderById(id) {
  return request({ url: `/api/v1/readers/${id}`, method: "get" });
}
```

测试必须断言插值后的字符串：

```javascript
getReaderById(42);
expect(request).toHaveBeenCalledWith({
  url: "/api/v1/readers/42", // ✅ 插值后
  method: "get",
});
```

### 无参数函数

源码：

```javascript
export function logout() {
  return request({ url: "/api/v1/auth/logout", method: "post" });
}
```

测试：

```javascript
logout();
expect(request).toHaveBeenCalledWith({
  url: "/api/v1/auth/logout",
  method: "post",
});
```

注意：当源码不带 `data` / `params` 字段时，断言对象也不能加。

### 多参数函数

源码：

```javascript
export function updateReader(id, data) {
  return request({ url: `/api/v1/readers/${id}`, method: "put", data });
}
```

测试：

```javascript
const data = { realName: "李四" };
updateReader(7, data);
expect(request).toHaveBeenCalledWith({
  url: "/api/v1/readers/7",
  method: "put",
  data,
});
```

---

## 验证

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npx vitest run src/api 2>&1 | tail -10
```

**预期：** ~70-80 个测试通过。

---

## 修改文件清单

### 新建（10 个测试文件）

| 文件                                              | 估计测试数 |
| ------------------------------------------------- | ---------- |
| `web-admin/src/api/__tests__/auth.test.js`        | 3          |
| `web-admin/src/api/__tests__/readers.test.js`     | ~7         |
| `web-admin/src/api/__tests__/books.test.js`       | ~10        |
| `web-admin/src/api/__tests__/circulation.test.js` | ~10        |
| `web-admin/src/api/__tests__/system.test.js`      | ~15        |
| `web-admin/src/api/__tests__/analytics.test.js`   | ~9         |
| `web-admin/src/api/__tests__/inventory.test.js`   | ~9         |
| `web-admin/src/api/__tests__/recommend.test.js`   | ~5         |
| `web-admin/src/api/__tests__/chat.test.js`        | ~3         |
| `web-admin/src/api/__tests__/department.test.js`  | ~3         |

### 无源码修改

仅新增测试文件，不修改任何 API 源码。

### 无新依赖

`vitest` + `vi.mock` 已就位（P4A/P4B 已用）。

---

## 执行策略

并行 dispatch 10 个 agents（每个一个 API 文件），每个 agent：

1. 读取对应 `src/api/<name>.js`，列出所有导出函数及其签名
2. 为每个导出写一个调用形状测试
3. 运行 `npx vitest run src/api/__tests__/<name>.test.js`
4. 全部通过后单独提交：`test(web-admin): add <name> API unit tests`

最后聚合验证：`npx vitest run src/api`，预期 ~70-80 通过。
