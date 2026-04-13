# Harness Engineering 适配设计 — GCRF Library Management System

**日期：** 2026-04-13
**状态：** Approved
**来源：** `docs/Harness-Engineering-标准配置手册.md` (v1.0, 适用 pnpm + Turbo + React + FastAPI)
**目标：** 适配到本项目架构 (Maven + Spring Boot + Vue 3 + npm)

---

## 背景

标准配置手册是为 pnpm monorepo + React + FastAPI 架构设计的。本项目是 Java 21 Spring Boot 微服务 + Vue 3 前端，需要逐 Phase 适配。

**总体策略：** 根目录统一管理（方案 A）— 根目录放置 `package.json` 管理工程化工具（husky, commitlint, lint-staged），`web-admin/` 保留业务依赖。

---

## Phase 1 — 代码格式与质量

### 1.1 EditorConfig

**新建** 根目录 `.editorconfig`：

```ini
root = true

[*]
indent_style = space
indent_size = 2
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

[*.java]
indent_size = 4

[*.xml]
indent_size = 4

[*.py]
indent_size = 4

[*.md]
trim_trailing_whitespace = false
```

与手册区别：增加 `*.java` 和 `*.xml` 的 4-space 规则。

### 1.2 Prettier

- **保留**现有 `web-admin/.prettierrc` 不变
- **新增**根目录 `.prettierignore`：

```
dist/
node_modules/
coverage/
*.min.js
*.min.css
backend/
library-backend/
deployment/
*.log
```

### 1.3 ESLint

- **保留**现有 `web-admin/.eslintrc.cjs`（ESLint 8 + vue3-essential）
- 不升级 flat config（需 eslint-plugin-vue v10+，风险高，不在本次范围）

### 1.4 Ruff

- **跳过** — 项目无 Python 后端

---

## Phase 2 — Git Hooks 与提交规范

### 2.1 根目录 package.json（新建）

```json
{
  "name": "gcrf-library-management-system",
  "private": true,
  "description": "国创睿峰智能图书馆管理系统 - 工程化配置",
  "scripts": {
    "prepare": "husky"
  },
  "lint-staged": {
    "web-admin/**/*.{vue,js,jsx,cjs,mjs}": ["eslint --fix", "prettier --write"],
    "web-admin/**/*.{css,scss,json}": ["prettier --write"],
    "*.md": ["prettier --write"]
  },
  "devDependencies": {
    "husky": "^9.1.0",
    "lint-staged": "^16.4.0",
    "@commitlint/cli": "^20.5.0",
    "@commitlint/config-conventional": "^20.5.0",
    "prettier": "^3.1.0"
  }
}
```

### 2.2 husky hooks

**`.husky/pre-commit`：**

```
npx lint-staged
```

**`.husky/commit-msg`：**

```
npx commitlint --edit $1
```

### 2.3 commitlint

**根目录 `commitlint.config.js`：**

```javascript
export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "scope-enum": [
      2,
      "always",
      [
        "gateway",
        "auth",
        "book",
        "circulation",
        "reader",
        "system",
        "notification",
        "recommend",
        "chat",
        "analytics",
        "common",
        "web-admin",
        "infra",
        "docs",
      ],
    ],
    "scope-empty": [1, "never"],
  },
};
```

### 2.4 PR Template

**新建 `.github/pull_request_template.md`：**

```markdown
## Summary

<!-- Briefly describe what changed and why -->

## Type of Change

- [ ] `feat`: New feature
- [ ] `fix`: Bug fix
- [ ] `refactor`: Code refactoring
- [ ] `docs`: Documentation only
- [ ] `test`: Adding or updating tests
- [ ] `chore`: Build process or tooling
- [ ] `ci`: CI/CD changes

## Testing

<!-- How did you verify this change? -->

## Checklist

- [ ] Backend compiles (`mvn clean compile`)
- [ ] Backend tests pass (`mvn test`)
- [ ] Frontend lint passes (`cd web-admin && npm run lint`)
- [ ] Frontend builds (`cd web-admin && npm run build`)
- [ ] No hardcoded secrets or credentials
- [ ] API changes documented (if applicable)
- [ ] Self-reviewed the diff before requesting review
```

---

## Phase 3 — 前端测试 (Vitest for Vue 3)

### 3.1 安装依赖

```bash
cd web-admin
npm install -D vitest @vitest/coverage-v8 @vue/test-utils jsdom
```

### 3.2 配置

**新建 `web-admin/vitest.config.js`：**

```javascript
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.test.{js,ts}", "src/**/*.spec.{js,ts}"],
    css: false,
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
```

### 3.3 Smoke Test

**新建 `web-admin/src/__tests__/smoke.test.js`：**

```javascript
import { describe, it, expect } from "vitest";

describe("Smoke Test", () => {
  it("should pass", () => {
    expect(1 + 1).toBe(2);
  });
});
```

### 3.4 Scripts

在 `web-admin/package.json` 新增：

- `"test": "vitest run"`
- `"test:watch": "vitest"`
- `"test:coverage": "vitest run --coverage"`

---

## Phase 4 — 后端测试 (pytest)

**跳过** — 项目无 Python 后端。Java 后端测试由 Maven Surefire 管理，已在 CI 中覆盖。

---

## Phase 5 — CI 持续集成

### 5.1 新建 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  pull_request:
    branches: [main, develop]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  lint-frontend:
    name: Lint Frontend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: web-admin/package-lock.json
      - run: cd web-admin && npm ci
      - name: ESLint
        run: cd web-admin && npm run lint
      - name: Prettier check
        run: cd web-admin && npx prettier --check "src/**/*.{vue,js,css,json}"

  test-frontend:
    name: Test Frontend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: web-admin/package-lock.json
      - run: cd web-admin && npm ci
      - name: Run Vitest
        run: cd web-admin && npm run test:coverage
      - name: Upload coverage
        uses: actions/upload-artifact@v4
        with:
          name: frontend-coverage
          path: web-admin/coverage/

  build-frontend:
    name: Build Frontend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: web-admin/package-lock.json
      - run: cd web-admin && npm ci
      - run: cd web-admin && npm run build

  build-backend:
    name: Build & Test Backend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven
      - name: Compile
        run: cd backend && mvn clean compile -DskipTests
      - name: Test
        run: cd backend && mvn test
      - name: Upload test reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: backend-test-reports
          path: backend/**/target/surefire-reports/
```

### 5.2 与现有 workflow 的关系

| Workflow                  | 触发                    | 职责            |
| ------------------------- | ----------------------- | --------------- |
| **ci.yml** (新)           | PR → main/develop       | 质量门禁        |
| build-and-push.yml (已有) | push main/develop + tag | Docker 构建推送 |
| security-scan.yml (已有)  | PR + push + 定时        | Trivy 安全扫描  |

---

## Phase 6 — CD 持续部署

**跳过** — 本项目使用 Docker Compose + Harbor 部署，不使用 GCP Cloud Run。已有 `build-and-push.yml` 覆盖。

---

## Phase 7 — E2E 测试、文档、Code Review

### 7.1 Playwright E2E

- **保留**现有 `web-admin/playwright.config.ts` 不变
- 在根 `.gitignore` 补充 `playwright-report/` 和 `blob-report/`

### 7.2 文档结构

新建以下目录和文件：

```
docs/
├── guides/
│   └── getting-started.md       # 新人上手（从 QUICKSTART.md 精简）
├── adr/
│   ├── README.md                # ADR 索引
│   └── 0001-harness-engineering.md  # 本次工程化决策记录
└── specs/
    └── 2026-04-13-harness-engineering-adaptation-design.md  # 本文档
```

### 7.3 CODEOWNERS

**新建 `.github/CODEOWNERS`：**

```
* @garryyifei8
backend/                @garryyifei8
web-admin/              @garryyifei8
deployment/             @garryyifei8
.github/                @garryyifei8
docs/                   @garryyifei8
```

### 7.4 CLAUDE.md

**保留**现有 `CLAUDE.md` 不变。

---

## Phase 8 — 熵管理

### 8.1 Dependabot

**新建 `.github/dependabot.yml`：**

```yaml
version: 2
updates:
  - package-ecosystem: npm
    directory: /web-admin
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 10
    groups:
      dev-dependencies:
        dependency-type: development
      production-dependencies:
        dependency-type: production

  - package-ecosystem: maven
    directory: /backend
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 5

  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
```

### 8.2 Entropy Scan

**新建 `.github/workflows/entropy-scan.yml`：**

定时扫描 TODO/FIXME/HACK 计数，文件类型为 `*.java` `*.vue` `*.js`，搜索路径为 `backend/` 和 `web-admin/src/`。

### 8.3 跳过项

- **knip** — 不适用于 Java 后端，前端非 TypeScript monorepo，收益低

---

## 完整文件清单

### 新建文件

| 文件                                    | 来源 Phase |
| --------------------------------------- | ---------- |
| `.editorconfig`                         | Phase 1    |
| `.prettierignore`                       | Phase 1    |
| `package.json` (根目录)                 | Phase 2    |
| `.husky/pre-commit`                     | Phase 2    |
| `.husky/commit-msg`                     | Phase 2    |
| `commitlint.config.js`                  | Phase 2    |
| `.github/pull_request_template.md`      | Phase 2    |
| `web-admin/vitest.config.js`            | Phase 3    |
| `web-admin/src/__tests__/smoke.test.js` | Phase 3    |
| `.github/workflows/ci.yml`              | Phase 5    |
| `.github/CODEOWNERS`                    | Phase 7    |
| `.github/dependabot.yml`                | Phase 8    |
| `.github/workflows/entropy-scan.yml`    | Phase 8    |
| `docs/guides/getting-started.md`        | Phase 7    |
| `docs/adr/README.md`                    | Phase 7    |
| `docs/adr/0001-harness-engineering.md`  | Phase 7    |

### 修改文件

| 文件                     | 变更                                       |
| ------------------------ | ------------------------------------------ |
| `web-admin/package.json` | 添加 test/test:watch/test:coverage scripts |
| `.gitignore`             | 补充 playwright-report/ blob-report/       |

### 跳过项

| 手册模块                | 跳过原因                          |
| ----------------------- | --------------------------------- |
| Ruff (Python)           | 无 Python                         |
| pytest                  | 无 Python                         |
| CD (GCP Cloud Run)      | 使用 Docker Compose + Harbor      |
| ESLint flat config 升级 | 需 eslint-plugin-vue v10+，风险高 |
| knip 死代码检测         | 不适用于 Java，前端收益低         |
