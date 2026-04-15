# P4A: 前端 Stores + Utils 单元测试设计

**日期：** 2026-04-14
**状态：** Approved
**优先级：** P4A（前端测试基础层）
**背景：** 生产就绪审计发现前端仅有 1 个 smoke test，所有 Pinia stores 和 utils 零测试覆盖。P4A 为基础逻辑层（stores + utils）建立单元测试，为后续 P4B/P4C/P4D 提供 mock 模式和基础框架。

---

## 目标

1. 为 6 个 Pinia stores 建立单元测试覆盖
2. 为 2 个关键 utils（request.js 拦截器、errorHandler.js）建立单元测试
3. 建立 Vitest + `@pinia/testing` + `vi.mock()` 的测试模式，供后续 P4B/P4C/P4D 复用

---

## Section 1：依赖安装

### `@pinia/testing`

检查是否已装：

```bash
grep '"@pinia/testing"' web-admin/package.json
```

如果缺失，安装：

```bash
cd web-admin
npm install -D @pinia/testing
```

其他依赖（`vitest`、`@vue/test-utils`、`jsdom`）已在 P2A 安装。

---

## Section 2：测试风格（统一）

### Store 测试模板

```javascript
import { describe, it, expect, beforeEach, vi } from "vitest";
import { createTestingPinia } from "@pinia/testing";
import { setActivePinia } from "pinia";

// IMPORTANT: vi.mock() must be at top, hoisted before imports
vi.mock("@/api/auth", () => ({
  login: vi.fn(),
  logout: vi.fn(),
  // ... other methods
}));

import { useUserStore } from "@/stores/user";
import * as authApi from "@/api/auth";

describe("useUserStore", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia({ stubActions: false }));
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("login_success_shouldSetTokenAndUserInfo", async () => {
    authApi.login.mockResolvedValue({
      data: { token: "abc123", userInfo: { username: "admin", name: "张三" } },
    });

    const store = useUserStore();
    await store.login({ username: "admin", password: "123" });

    expect(store.token).toBe("abc123");
    expect(store.userInfo.username).toBe("admin");
  });
});
```

### 关键约定

- `stubActions: false` — 让真实 action 执行（我们测逻辑），而非被 Pinia 替换为 spy
- `vi.mock('@/api/...')` — 在顶部 hoist，避免真实 HTTP 请求
- 每个测试 `beforeEach` 清理：
  - 重置 Pinia 实例
  - `vi.clearAllMocks()` 清 mock call history
  - `localStorage.clear()` 清持久化数据
- Mock router（`vue-router`）如果 store 触发导航

---

## Section 3：8 个测试文件

### Store 1: user.test.js（~8 tests）

**文件：** `web-admin/src/stores/__tests__/user.test.js`

**Mock：** `@/api/auth`, `vue-router` (for logout redirect), localStorage

| #   | 测试                                                    | 覆盖                                                 |
| --- | ------------------------------------------------------- | ---------------------------------------------------- |
| 1   | `login_success_shouldSetTokenUserInfoPermissions`       | 登录成功后 token/userInfo/permissions 被设置         |
| 2   | `login_apiError_shouldThrowAndNotSetToken`              | API 抛错时不设置 token，异常向上传递                 |
| 3   | `logout_shouldClearAllStateAndCallApi`                  | 登出清空 token/userInfo/permissions，调用 logout API |
| 4   | `logout_apiError_shouldStillClearState`                 | API 失败也清空本地状态                               |
| 5   | `hasPermission_withWildcard_shouldReturnTrue`           | `permissions = ['*']` 对任何权限返回 true            |
| 6   | `hasPermission_matchingPermission_shouldReturnTrue`     | 精确匹配返回 true                                    |
| 7   | `hasPermission_nonMatchingPermission_shouldReturnFalse` | 无匹配返回 false                                     |
| 8   | `setters_shouldUpdateState`                             | setToken/setUserInfo/setPermissions 各自工作         |

---

### Store 2: book.test.js（~10 tests）

**文件：** `web-admin/src/stores/__tests__/book.test.js`

**Mock：** `@/api/books` — `getCategories`、`getBooks`

| #   | 测试                                          | 覆盖                         |
| --- | --------------------------------------------- | ---------------------------- |
| 1   | `loadCategories_firstCall_shouldFetchFromApi` | 首次调用发起 API 请求        |
| 2   | `loadCategories_withinCache_shouldNotRefetch` | 缓存未过期不重复请求         |
| 3   | `loadCategories_force_shouldBypassCache`      | `force: true` 强制刷新       |
| 4   | `loadCategories_concurrentCalls_shouldDedupe` | 并发调用只触发 1 次 API      |
| 5   | `loadHotBooks_shouldCacheFor5Minutes`         | 5 分钟内不重复请求           |
| 6   | `addRecentBook_shouldDedupAndCapAt20`         | 重复书籍去重，超过 20 条截断 |
| 7   | `getCategoryName_shouldReturnNameFromTree`    | 从 categories 树中查找分类名 |
| 8   | `categoryOptions_shouldFlattenTree`           | 树形结构扁平化为下拉选项     |
| 9   | `clearCache_shouldResetTimestamps`            | 清空缓存时间戳               |
| 10  | `reset_shouldClearAllState`                   | 完全重置                     |

---

### Store 3: circulation.test.js（~10 tests）

**文件：** `web-admin/src/stores/__tests__/circulation.test.js`

**Mock：** `@/api/circulation`

| #   | 测试                                           | 覆盖                     |
| --- | ---------------------------------------------- | ------------------------ |
| 1   | `borrowCart_addItem_shouldIncrementCount`      | 添加到借书车             |
| 2   | `borrowCart_removeItem_shouldDecrement`        | 从借书车移除             |
| 3   | `borrowCart_clear_shouldEmpty`                 | 清空借书车               |
| 4   | `returnCart_addItem_shouldIncrementCount`      | 添加到还书车             |
| 5   | `returnCart_shouldHandleDuplicates`            | 同一本书不重复           |
| 6   | `isBorrowing_whenOperationActive_shouldBeTrue` | borrowOperation 活跃状态 |
| 7   | `fineRules_loadFromApi_shouldCache`            | 罚款规则 30 分钟缓存     |
| 8   | `fineRules_force_shouldBypassCache`            | 强制刷新                 |
| 9   | `borrowRules_shouldCache`                      | 借阅规则缓存             |
| 10  | `reset_shouldClearAllState`                    | 重置                     |

---

### Store 4: reader.test.js（~8 tests）

**文件：** `web-admin/src/stores/__tests__/reader.test.js`

**Mock：** `@/api/readers`

具体测试根据实际 store 结构决定 — 通常包括：

- 加载读者列表 / 缓存
- 按类型筛选
- 当前选中读者
- 读者统计
- reset

---

### Store 5: system.test.js（~8 tests）

**文件：** `web-admin/src/stores/__tests__/system.test.js`

**Mock：** `@/api/system`

通常覆盖：

- 加载菜单/权限/部门
- 角色权限查询
- 系统配置缓存

---

### Store 6: analytics.test.js（~6 tests）

**文件：** `web-admin/src/stores/__tests__/analytics.test.js`

**Mock：** `@/api/analytics`

通常覆盖：

- 仪表板数据加载
- 日期范围筛选
- 图表数据转换

---

### Util 1: request.test.js（~10 tests）

**文件：** `web-admin/src/utils/__tests__/request.test.js`

**Mock：** `axios`（模块层级）、`useUserStore`

核心逻辑：请求拦截器、响应拦截器、token 刷新队列。

| #   | 测试                                                     | 覆盖                                          |
| --- | -------------------------------------------------------- | --------------------------------------------- |
| 1   | `requestInterceptor_withToken_shouldAddAuthHeader`       | Authorization header 被注入                   |
| 2   | `requestInterceptor_withoutToken_shouldNotAddHeader`     | 无 token 时不加 header                        |
| 3   | `responseInterceptor_success_shouldReturnData`           | code=200 返回 data                            |
| 4   | `responseInterceptor_businessError_shouldCallHandler`    | code != 200 调用 errorHandler                 |
| 5   | `responseInterceptor_401_shouldTriggerRefresh`           | 401 触发 token refresh 流程                   |
| 6   | `responseInterceptor_401_queued_shouldRetryAfterRefresh` | refresh 中的请求队列                          |
| 7   | `responseInterceptor_readerServicePage_shouldWrap`       | Reader service 分页响应被包装                 |
| 8   | `responseInterceptor_httpError_shouldCallHandler`        | 4xx/5xx 调用 errorHandler.handleResponseError |
| 9   | `responseInterceptor_cancelled_shouldResolveSilently`    | 取消请求不报错                                |
| 10  | `responseInterceptor_timeout_shouldHandleTimeout`        | 超时触发超时处理                              |

**实现难点：** Mock axios 实例，需要模拟 interceptors.use() 注册的 onFulfilled/onRejected 回调。可以 export interceptor 函数让测试直接调用。

---

### Util 2: errorHandler.test.js（~12 tests）

**文件：** `web-admin/src/utils/__tests__/errorHandler.test.js`

**Mock：** `ElMessage`, `ElMessageBox`, router, userStore

| #   | 测试                                                        | 覆盖                         |
| --- | ----------------------------------------------------------- | ---------------------------- |
| 1   | `ErrorType_shouldBeComplete`                                | 8 个枚举值全部导出           |
| 2   | `handleAuthError_shouldShowMessageBoxAndLogout`             | 401 显示弹窗 + 调用 logout   |
| 3   | `handlePermissionError_shouldShowErrorAndRedirect`          | 403 显示 error + 跳转 home   |
| 4   | `handleValidationError_withErrors_shouldShowList`           | 显示校验错误列表             |
| 5   | `handleValidationError_withoutErrors_shouldShowGeneric`     | 无具体错误显示通用消息       |
| 6   | `handleBusinessError_shouldShowMessage`                     | 业务错误显示 message         |
| 7   | `handleSystemError_shouldSuggestRetry`                      | 系统错误建议重试             |
| 8   | `handleTimeoutError_shouldShowWarning`                      | 超时警告                     |
| 9   | `handleNetworkError_shouldSuggestCheck`                     | 网络错误提示检查             |
| 10  | `handleResponseError_status401_shouldDelegateToAuthHandler` | HTTP 401 路由到 AUTH handler |
| 11  | `handleBusinessCode_code1000_shouldMapToValidation`         | code 1000-1999 → VALIDATION  |
| 12  | `unhandledRejectionHandler_chunkLoadError_shouldReload`     | ChunkLoadError 触发页面刷新  |

---

## Section 4：测试执行

### 运行单个 store

```bash
cd web-admin
npm test -- user.test.js
```

### 运行所有 stores

```bash
npm test -- src/stores/__tests__
```

### 运行全部 P4A 测试 + 覆盖率

```bash
npm run test:coverage -- src/stores src/utils
```

---

## 修改文件清单

### 新建（8 个测试文件）

| 文件                                                 | 测试数 |
| ---------------------------------------------------- | ------ |
| `web-admin/src/stores/__tests__/user.test.js`        | ~8     |
| `web-admin/src/stores/__tests__/book.test.js`        | ~10    |
| `web-admin/src/stores/__tests__/circulation.test.js` | ~10    |
| `web-admin/src/stores/__tests__/reader.test.js`      | ~8     |
| `web-admin/src/stores/__tests__/system.test.js`      | ~8     |
| `web-admin/src/stores/__tests__/analytics.test.js`   | ~6     |
| `web-admin/src/utils/__tests__/request.test.js`      | ~10    |
| `web-admin/src/utils/__tests__/errorHandler.test.js` | ~12    |

**测试总数：** ~72 个

### 修改

| 文件                     | 变更                                   |
| ------------------------ | -------------------------------------- |
| `web-admin/package.json` | 添加 `@pinia/testing` devDep（如未装） |

---

## 验证方法

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm test 2>&1 | tail -10
```

**预期：** 约 72 个测试通过。无真实 HTTP 请求发起（所有 API 已 mock）。
