# P4D: 25 个 Vue 视图测试设计

**日期：** 2026-04-15
**状态：** Approved
**优先级：** P4D（前端视图层测试）
**背景：** P4A 完成 stores+utils（75 tests），P4B 完成 11 个组件（144 tests），P4C 完成 10 个 API 模块（139 tests）。P4D 为 `web-admin/src/views/` 下 25 个业务视图建立 smoke 覆盖与核心交互测试。

---

## 目标

1. 为 25 个 Vue 视图建立 mount smoke 测试（防御 store/API 重命名导致页面崩溃）
2. 为 5 个核心业务页（login、dashboard、circulation/borrow、readers/students、books/list）添加交互测试覆盖关键业务流程
3. 不追求完整覆盖率（深度交互留给 E2E）

---

## 测试结构

```
web-admin/src/views/
├── login/index.vue
├── login/__tests__/index.test.js   # smoke + 4 interaction
├── dashboard/index.vue
├── dashboard/__tests__/index.test.js
├── circulation/borrow.vue
├── circulation/__tests__/borrow.test.js
└── ...
```

测试文件与 view 同目录的 `__tests__/` 子目录内，文件名匹配。

---

## Smoke 测试模板

```javascript
import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { createTestingPinia } from "@pinia/testing";
import ElementPlus from "element-plus";
import LoginView from "@/views/login/index.vue";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: "/login" }),
}));

vi.mock("@/api/auth", () => ({
  login: vi.fn().mockResolvedValue({ code: 200, data: { token: "x" } }),
  register: vi.fn(),
  getUserInfo: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  logout: vi.fn(),
  refreshToken: vi.fn(),
}));

describe("views/login", () => {
  const factory = () =>
    mount(LoginView, {
      global: {
        plugins: [createTestingPinia({ stubActions: false }), ElementPlus],
        stubs: { "router-link": true, "router-view": true },
      },
    });

  it("mount_shouldRenderWithoutError", () => {
    const wrapper = factory();
    expect(wrapper.exists()).toBe(true);
  });
});
```

### 关键约定

- **每个 view 都用 `createTestingPinia({ stubActions: false })`**
- **mock `vue-router`** — 返回 `useRouter` / `useRoute`
- **mock 视图引用的所有 `@/api/*` 模块** — 每个 export 都返回 resolved promise
- **stubs** — 如果用到 `<router-link>` / `<router-view>`，stub 为 true
- 交互测试用 `await wrapper.vm.$nextTick()` 或 `await flushPromises()` 等待异步

---

## 25 个 View Smoke 测试清单

### 4.1 全量 Smoke

| #   | 文件                                               | 测试                           |
| --- | -------------------------------------------------- | ------------------------------ |
| 1   | `views/login/__tests__/index.test.js`              | mount_shouldRenderWithoutError |
| 2   | `views/dashboard/__tests__/index.test.js`          | mount_shouldRenderWithoutError |
| 3   | `views/readers/__tests__/students.test.js`         | mount_shouldRenderWithoutError |
| 4   | `views/readers/__tests__/teachers.test.js`         | mount_shouldRenderWithoutError |
| 5   | `views/readers/__tests__/card.test.js`             | mount_shouldRenderWithoutError |
| 6   | `views/books/__tests__/list.test.js`               | mount_shouldRenderWithoutError |
| 7   | `views/books/__tests__/catalog.test.js`            | mount_shouldRenderWithoutError |
| 8   | `views/books/__tests__/collection.test.js`         | mount_shouldRenderWithoutError |
| 9   | `views/books/__tests__/inventory.test.js`          | mount_shouldRenderWithoutError |
| 10  | `views/circulation/__tests__/borrow.test.js`       | mount_shouldRenderWithoutError |
| 11  | `views/circulation/__tests__/return.test.js`       | mount_shouldRenderWithoutError |
| 12  | `views/circulation/__tests__/records.test.js`      | mount_shouldRenderWithoutError |
| 13  | `views/circulation/__tests__/reservations.test.js` | mount_shouldRenderWithoutError |
| 14  | `views/system/__tests__/users.test.js`             | mount_shouldRenderWithoutError |
| 15  | `views/system/__tests__/roles.test.js`             | mount_shouldRenderWithoutError |
| 16  | `views/system/__tests__/departments.test.js`       | mount_shouldRenderWithoutError |
| 17  | `views/system/__tests__/config.test.js`            | mount_shouldRenderWithoutError |
| 18  | `views/system/__tests__/backup.test.js`            | mount_shouldRenderWithoutError |
| 19  | `views/ai/__tests__/chat.test.js`                  | mount_shouldRenderWithoutError |
| 20  | `views/ai/__tests__/recommend.test.js`             | mount_shouldRenderWithoutError |
| 21  | `views/ai/__tests__/analytics.test.js`             | mount_shouldRenderWithoutError |
| 22  | `views/profile/__tests__/info.test.js`             | mount_shouldRenderWithoutError |
| 23  | `views/profile/__tests__/password.test.js`         | mount_shouldRenderWithoutError |
| 24  | `views/error/__tests__/404.test.js`                | mount_shouldRenderWithoutError |
| 25  | `views/demo/__tests__/components.test.js`          | mount_shouldRenderWithoutError |

**Smoke 小计：25 个测试**

### 4.2 核心页面交互测试

| 视图                   | 交互测试                                                                  | 数量 |
| ---------------------- | ------------------------------------------------------------------------- | ---- |
| **login**              | 输入 username/password / 提交触发 login API / 失败显示错误 / 记住密码切换 | 4    |
| **dashboard**          | mount 时调用 stats API / 显示统计卡片 / 切换日期范围触发刷新              | 3    |
| **circulation/borrow** | 读者号搜索 / 图书条码搜索 / 提交借阅触发 API / 失败显示错误               | 4    |
| **readers/students**   | 关键词搜索触发请求 / 分页变化触发请求 / 新增按钮打开对话框 / 删除调用 API | 4    |
| **books/list**         | 搜索 / 分页 / 分类筛选 / 状态筛选                                         | 4    |

**交互小计：19 个测试**

**总计：~44 个测试**

---

## 5 个核心页面交互测试细节

### login（views/login/**tests**/index.test.js）

```javascript
it("login_validForm_shouldCallLoginApi", async () => {
  const { login } = await import("@/api/auth");
  const wrapper = factory();
  await wrapper.find('input[placeholder*="用户名"]').setValue("admin");
  await wrapper.find('input[type="password"]').setValue("123456");
  await wrapper.find(".login-button").trigger("click");
  await flushPromises();
  expect(login).toHaveBeenCalled();
});
```

实际选择器需根据组件结构调整。重点：验证 API 被调用，不验证 UI 细节。

### dashboard

mock 多个 stat API（`getOverview`、`getBorrowTrends` 等），mount 后验证至少 1 个被调用即可。

### circulation/borrow

mock `getReaderByCardNumber`、`getBookByBarcode`、`borrowBook`。验证读者搜索/图书搜索/借阅提交流程。

### readers/students

mock `getReaders`。验证搜索框输入触发 list 请求、分页 change 触发请求。

### books/list

mock `getBooks`、`getCategories`。验证搜索/分页/分类筛选触发请求。

---

## Mock 总览

每个测试需要的标准 mock 集合：

```javascript
// 1. Router
vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: {}, query: {}, path: "/" }),
}));

// 2. Pinia stores - automatic via createTestingPinia

// 3. 每个 @/api/* 模块 - 必须列出所有 export
vi.mock("@/api/<module>", () => ({
  funcA: vi.fn().mockResolvedValue({ code: 200, data: {} }),
  funcB: vi
    .fn()
    .mockResolvedValue({ code: 200, data: { records: [], total: 0 } }),
}));

// 4. echarts (仅 dashboard、analytics 需要)
vi.mock("echarts", () => ({
  init: vi.fn(() => ({
    setOption: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn(),
  })),
  graphic: { LinearGradient: vi.fn() },
}));

// 5. ElMessageBox (确认对话框)
vi.mock("element-plus", async () => {
  const actual = await vi.importActual("element-plus");
  return {
    ...actual,
    ElMessageBox: { confirm: vi.fn().mockResolvedValue("confirm") },
  };
});
```

---

## 实现风险与缓解

| 风险                                                 | 缓解                                                                      |
| ---------------------------------------------------- | ------------------------------------------------------------------------- |
| 视图依赖很多，mock 漏一个崩溃                        | smoke 只断言 `wrapper.exists()`；即使内部告警也通过                       |
| 大型 view（dashboard 1362, students 1457 LOC）编写慢 | 不强求覆盖率，smoke + 3-4 关键交互即可                                    |
| 异步时序导致 flake                                   | 统一用 `await flushPromises()`                                            |
| ElementPlus Dialog/Tree 等异步组件                   | 交互测试聚焦 store/API 调用，不验证内部 UI                                |
| 选择器随 UI 变化失效                                 | 用 data-test attribute 或更稳定的选择器，必要时用 `wrapper.vm` 直接调方法 |

---

## 验证

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npx vitest run src/views 2>&1 | tail -10
```

**预期：** ~44 个测试通过。

---

## 文件修改清单

### 新建（25 个测试文件）

见 4.1 节清单。所有为新增，无源码修改。

### 无新依赖

`@pinia/testing`、`@vue/test-utils`、`element-plus`、`vitest` 均已在 P4A/P4B/P4C 就位。

---

## 执行策略

并行 dispatch 10 个 agents：

**Phase 1（5 agents 并行 — 核心页 smoke + 交互）：**

- T1: login（smoke + 4）
- T2: dashboard（smoke + 3）
- T3: circulation/borrow（smoke + 4）
- T4: readers/students（smoke + 4）
- T5: books/list（smoke + 4）

**Phase 2（5 agents 并行 — 剩余 20 个 view smoke，按目录分组）：**

- T6: readers（teachers, card）
- T7: books（catalog, collection, inventory）
- T8: circulation（return, records, reservations）
- T9: system（users, roles, departments, config, backup）
- T10: ai + profile + error + demo（chat, recommend, analytics, info, password, 404, components）

最终聚合验证：`npx vitest run src/views`，预期 ~44 通过。
