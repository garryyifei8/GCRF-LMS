# P6: Playwright E2E 测试覆盖

**日期：** 2026-04-15
**状态：** Approved
**优先级：** P6（端到端验证）
**背景：** 已有 Playwright 配置 + 3 个 spec（auth/books/circulation, ~30 tests）但未验证可跑。需要修复+补 4 个核心业务 spec + CI 集成。

---

## 目标

1. 修复并跑通现有 auth/books/circulation 3 spec
2. 新增 4 个 spec：readers、system、dashboard、profile
3. CI workflow `e2e.yml`：PR 时跑 E2E（chromium only）
4. Playwright HTML report 上传 artifact

运行模式：MSW mock（无需真后端，开发服务器自动启动）。

---

## 测试结构

```
web-admin/
├── e2e/
│   ├── auth.spec.ts          # 已存在 — 修复
│   ├── books.spec.ts         # 已存在 — 修复
│   ├── circulation.spec.ts   # 已存在 — 修复
│   ├── readers.spec.ts       # 新增
│   ├── system.spec.ts        # 新增
│   ├── dashboard.spec.ts     # 新增
│   ├── profile.spec.ts       # 新增
│   └── helpers/
│       └── auth.ts           # 新增
├── playwright.config.ts      # 已存在
└── package.json              # 已存在 test:e2e
```

---

## 测试模式

```typescript
import { test, expect } from "@playwright/test";
import { loginAsAdmin } from "./helpers/auth";

test.describe("Readers Management", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto("/readers/students");
  });

  test("should display student list", async ({ page }) => {
    await expect(
      page.getByRole("heading", { name: /学生读者|Students/i }),
    ).toBeVisible();
    await expect(page.locator(".el-table")).toBeVisible();
  });

  test("should search readers by keyword", async ({ page }) => {
    await page.fill('input[placeholder*="搜索"]', "张三");
    await page.click('button:has-text("查询")');
    await page.waitForLoadState("networkidle");
    // assertion based on mock data
  });
});
```

### 关键约定

- **优先 ARIA / 文本选择器**（`getByRole`、`getByText`、`button:has-text("...")`）比 CSS class 稳定
- **避免 `.el-message--error`**：用 `getByText(/错误|失败/)` 或具体提示文字
- **`waitForURL` + `waitForLoadState('networkidle')`** 等异步完成
- **每 spec 独立登录**（用 helper），不共享 state
- **MSW mock 模式**：dev server 启动时自动注册 MSW worker

---

## helpers/auth.ts

```typescript
import { Page } from "@playwright/test";

export async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  // Try multiple selectors for robustness against UI variants
  await page.fill(
    'input[placeholder*="用户名" i], input[name="username"], input[type="text"]:first-of-type',
    "admin",
  );
  await page.fill('input[type="password"]', "admin123");
  await page.click('button:has-text("登录"), button[type="submit"]');
  await page.waitForURL(/\/(dashboard|home|index)/, { timeout: 15_000 });
}

export async function logout(page: Page) {
  await page.click('.user-avatar, .header-user, [class*="user"]');
  await page.click("text=退出登录");
  await page.waitForURL("**/login", { timeout: 10_000 });
}
```

---

## 4 个新 Spec 测试覆盖

### readers.spec.ts（~6 tests）

1. list 页加载（学生 tab）
2. 搜索关键字触发请求
3. 分页变化
4. 新增对话框打开
5. 删除流程（确认 → 调用 API）
6. 切换到 teachers tab

### system.spec.ts（~6 tests）

1. users 列表加载
2. 用户搜索
3. 角色 tab 切换
4. 部门 tree 展示
5. 用户新增对话框
6. 权限分配对话框

### dashboard.spec.ts（~4 tests）

1. mount 加载（无 console error）
2. 显示统计卡片（≥ 4 张）
3. echarts 图表渲染（canvas/svg 元素）
4. 切换日期范围（如有控件，否则验证默认数据）

### profile.spec.ts（~3 tests）

1. info 页表单可编辑
2. 修改密码表单提交
3. 表单验证（空字段 → 错误提示）

**总计：~19 新测试 + 修复后 30+ 旧测试 = ~50 个 E2E**

---

## CI Workflow

**新建：** `.github/workflows/e2e.yml`

```yaml
name: E2E Tests

on:
  pull_request:
    branches: [main, develop, master]
  workflow_dispatch:

jobs:
  e2e-chromium:
    name: E2E (Chromium)
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: web-admin/package-lock.json

      - run: cd web-admin && npm ci

      - name: Install Playwright Browsers
        run: cd web-admin && npx playwright install --with-deps chromium

      - name: Run E2E tests
        run: cd web-admin && npx playwright test --project=chromium
        env:
          CI: true

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: web-admin/playwright-report/
          retention-days: 14

      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: playwright-traces
          path: web-admin/test-results/
          retention-days: 14
```

仅 chromium；CI 资源紧张时可加 `workers: 1`。

---

## 实现风险与缓解

| 风险                                                                | 缓解                                                                                  |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 现有 3 spec 选择器基于 `name="username"`，Element Plus 不渲染该属性 | T1 用 helper 重写 + `placeholder` / 文本选择器                                        |
| MSW worker 在 Playwright 浏览器中未注册                             | 检查 `public/mockServiceWorker.js` 存在；启动 console 应见 `[MSW] Loading X handlers` |
| dev server 启动慢 → timeout                                         | webServer timeout 已 120s                                                             |
| 异步等待 flaky                                                      | 统一 `waitForLoadState('networkidle')` + 明确 timeout                                 |
| Dialog/弹窗异步渲染                                                 | `page.locator('.el-dialog').waitFor()` 后操作                                         |
| webkit/firefox 兼容                                                 | CI 仅 chromium；本地可加                                                              |

---

## 验证

```bash
# 本地
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npx playwright test --project=chromium 2>&1 | tail -10
```

**预期：** ~50 tests passed。

---

## 文件修改清单

### 新建（6 个）

- `web-admin/e2e/helpers/auth.ts`
- `web-admin/e2e/readers.spec.ts`
- `web-admin/e2e/system.spec.ts`
- `web-admin/e2e/dashboard.spec.ts`
- `web-admin/e2e/profile.spec.ts`
- `.github/workflows/e2e.yml`

### 修改（3 个）

- `web-admin/e2e/auth.spec.ts` — 修选择器
- `web-admin/e2e/books.spec.ts` — 修选择器
- `web-admin/e2e/circulation.spec.ts` — 修选择器

---

## 执行策略

### Phase 1（1 agent — 关键基础）

- **T1**：写 `helpers/auth.ts` + 修复 auth.spec.ts，跑通验证基础

### Phase 2（4 agents 并行 — 基于 T1 模式）

- **T2**：修复 books.spec.ts
- **T3**：修复 circulation.spec.ts
- **T4**：写 readers.spec.ts + system.spec.ts
- **T5**：写 dashboard.spec.ts + profile.spec.ts

### Phase 3（1 agent）

- **T6**：写 `.github/workflows/e2e.yml`

### Phase 4（验证）

- **T7**：本地 `npx playwright test` 跑全套

T1 必须先完成（其他依赖该 helper 的 selector 模式）。T2-T6 可并行。T7 串行最后。
