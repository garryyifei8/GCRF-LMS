# Harness Engineering 适配 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the Harness Engineering standard (Phases 1/2/3/5/7/8) to the GCRF Library Management System, adapted from pnpm+React+FastAPI to Maven+Vue3+npm.

**Architecture:** Root-level `package.json` manages engineering toolchain (husky, commitlint, lint-staged, prettier). `web-admin/` retains its own business dependencies. CI workflow runs as a PR quality gate alongside existing build-and-push and security-scan workflows.

**Tech Stack:** husky 9, lint-staged 16, commitlint 20, Vitest 4, @vue/test-utils, GitHub Actions, Dependabot

**Spec:** `docs/specs/2026-04-13-harness-engineering-adaptation-design.md`

---

## File Map

### New Files

| File                                    | Responsibility                                     |
| --------------------------------------- | -------------------------------------------------- |
| `.editorconfig`                         | Unified editor settings (indent, charset, newline) |
| `.prettierignore`                       | Exclude backend/deployment from Prettier           |
| `package.json` (root)                   | Engineering toolchain deps + lint-staged config    |
| `commitlint.config.js`                  | Conventional Commits rules with project scopes     |
| `.husky/pre-commit`                     | Run lint-staged on staged files                    |
| `.husky/commit-msg`                     | Run commitlint on commit message                   |
| `.github/pull_request_template.md`      | PR checklist for Maven + npm projects              |
| `.github/CODEOWNERS`                    | Default code reviewers                             |
| `.github/dependabot.yml`                | Automated dependency updates (npm, maven, actions) |
| `.github/workflows/ci.yml`              | PR quality gate: lint, test, build                 |
| `.github/workflows/entropy-scan.yml`    | Weekly TODO/FIXME/HACK count                       |
| `web-admin/vitest.config.js`            | Vitest config for Vue 3 + jsdom                    |
| `web-admin/src/__tests__/smoke.test.js` | Smoke test to verify Vitest works                  |
| `docs/guides/getting-started.md`        | New developer onboarding guide                     |
| `docs/adr/README.md`                    | ADR index                                          |
| `docs/adr/0001-harness-engineering.md`  | Decision record for this engineering setup         |

### Modified Files

| File                     | Change                                    |
| ------------------------ | ----------------------------------------- |
| `web-admin/package.json` | Add test/test:watch/test:coverage scripts |
| `.gitignore`             | Add playwright-report/ blob-report/       |

---

## Task 1: Phase 1 — EditorConfig + Prettier

**Files:**

- Create: `.editorconfig`
- Create: `.prettierignore`

- [ ] **Step 1: Create `.editorconfig`**

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

- [ ] **Step 2: Create `.prettierignore`**

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

- [ ] **Step 3: Verify Prettier respects `.prettierignore`**

Run:

```bash
cd web-admin && npx prettier --check "src/**/*.{vue,js,css,json}" 2>&1 | head -5
```

Expected: Either "All matched files use Prettier code style!" or a list of frontend-only files — no backend files.

- [ ] **Step 4: Commit**

```bash
git add .editorconfig .prettierignore
git commit -m "chore(infra): add EditorConfig and .prettierignore

- EditorConfig: 2-space default, 4-space for Java/XML
- Prettierignore: exclude backend, deployment, build artifacts"
```

---

## Task 2: Phase 2a — Root package.json + husky + lint-staged

**Files:**

- Create: `package.json` (root)
- Create: `.husky/pre-commit`

- [ ] **Step 1: Create root `package.json`**

```json
{
  "name": "gcrf-library-management-system",
  "private": true,
  "type": "module",
  "description": "国创睿峰智能图书馆管理系统 - 工程化配置",
  "scripts": {
    "prepare": "husky"
  },
  "lint-staged": {
    "web-admin/**/*.{vue,js,jsx,cjs,mjs}": ["eslint --fix", "prettier --write"],
    "web-admin/**/*.{css,scss,json}": ["prettier --write"],
    "*.md": ["prettier --write"]
  }
}
```

- [ ] **Step 2: Install dependencies**

Run:

```bash
npm install -D husky lint-staged prettier
```

Expected: `node_modules/` created in root, `package-lock.json` generated.

- [ ] **Step 3: Initialize husky**

Run:

```bash
npx husky init
```

Expected: `.husky/` directory created with a default `pre-commit` file.

- [ ] **Step 4: Write `.husky/pre-commit`**

Replace the default content with:

```
npx lint-staged
```

- [ ] **Step 5: Verify lint-staged runs on a staged file**

Run:

```bash
touch /tmp/test-lint-staged.md
cp /tmp/test-lint-staged.md test-lint-staged.md
git add test-lint-staged.md
npx lint-staged --verbose 2>&1 | head -20
git checkout -- test-lint-staged.md 2>/dev/null; git reset HEAD test-lint-staged.md 2>/dev/null; rm -f test-lint-staged.md
```

Expected: lint-staged output showing Prettier ran on the `.md` file.

- [ ] **Step 6: Update `.gitignore` to include root `node_modules`**

The root `.gitignore` already has `node_modules/` — verify this covers the root directory (it does, since the pattern is not path-scoped). No change needed.

- [ ] **Step 7: Commit**

```bash
git add package.json package-lock.json .husky/
git commit -m "chore(infra): add husky + lint-staged

- Root package.json for engineering toolchain
- Pre-commit hook runs lint-staged on frontend files"
```

---

## Task 3: Phase 2b — commitlint

**Files:**

- Create: `commitlint.config.js`
- Create: `.husky/commit-msg`

- [ ] **Step 1: Install commitlint**

Run:

```bash
npm install -D @commitlint/cli @commitlint/config-conventional
```

- [ ] **Step 2: Create `commitlint.config.js`**

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

- [ ] **Step 3: Create `.husky/commit-msg`**

```
npx commitlint --edit $1
```

- [ ] **Step 4: Verify commitlint rejects bad messages**

Run:

```bash
echo "bad message" | npx commitlint
```

Expected: Error output containing "subject may not be empty" or "type may not be empty".

- [ ] **Step 5: Verify commitlint accepts good messages**

Run:

```bash
echo "feat(book): add search endpoint" | npx commitlint
```

Expected: No errors, exit code 0.

- [ ] **Step 6: Commit**

```bash
git add commitlint.config.js .husky/commit-msg package.json package-lock.json
git commit -m "chore(infra): add commitlint with project scopes

- Conventional Commits enforced via commit-msg hook
- Scopes: gateway, auth, book, circulation, reader, system,
  notification, recommend, chat, analytics, common, web-admin,
  infra, docs"
```

---

## Task 4: Phase 2c — PR Template + CODEOWNERS

**Files:**

- Create: `.github/pull_request_template.md`
- Create: `.github/CODEOWNERS`

- [ ] **Step 1: Create `.github/pull_request_template.md`**

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

- [ ] **Step 2: Create `.github/CODEOWNERS`**

```
# Default reviewer for all changes
* @garryyifei8

# Backend services
backend/                @garryyifei8

# Frontend
web-admin/              @garryyifei8

# Infrastructure
deployment/             @garryyifei8
.github/                @garryyifei8

# Documentation
docs/                   @garryyifei8
```

- [ ] **Step 3: Commit**

```bash
git add .github/pull_request_template.md .github/CODEOWNERS
git commit -m "chore(infra): add PR template and CODEOWNERS"
```

---

## Task 5: Phase 3 — Vitest for Vue 3

**Files:**

- Create: `web-admin/vitest.config.js`
- Create: `web-admin/src/__tests__/smoke.test.js`
- Modify: `web-admin/package.json` (add scripts)

- [ ] **Step 1: Install Vitest dependencies in web-admin**

Run:

```bash
cd web-admin && npm install -D vitest @vitest/coverage-v8 @vue/test-utils jsdom
```

- [ ] **Step 2: Create `web-admin/vitest.config.js`**

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

- [ ] **Step 3: Add test scripts to `web-admin/package.json`**

Add to the `"scripts"` section:

```json
"test": "vitest run",
"test:watch": "vitest",
"test:coverage": "vitest run --coverage"
```

- [ ] **Step 4: Create smoke test `web-admin/src/__tests__/smoke.test.js`**

```javascript
import { describe, it, expect } from "vitest";

describe("Smoke Test", () => {
  it("should pass", () => {
    expect(1 + 1).toBe(2);
  });
});
```

- [ ] **Step 5: Run the smoke test**

Run:

```bash
cd web-admin && npm run test
```

Expected: 1 test passed.

- [ ] **Step 6: Run coverage**

Run:

```bash
cd web-admin && npm run test:coverage
```

Expected: Coverage report generated (likely 0% since smoke test doesn't cover app code — that's fine).

- [ ] **Step 7: Commit**

```bash
cd web-admin && git add vitest.config.js src/__tests__/smoke.test.js package.json package-lock.json
git commit -m "test(web-admin): add Vitest with Vue 3 config and smoke test

- Vitest + jsdom + @vue/test-utils
- Added test/test:watch/test:coverage scripts"
```

---

## Task 6: Phase 5 — CI Workflow

**Files:**

- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create `.github/workflows/ci.yml`**

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

- [ ] **Step 2: Validate YAML syntax**

Run:

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))" && echo "YAML OK"
```

Expected: "YAML OK"

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci(infra): add PR quality gate workflow

- lint-frontend: ESLint + Prettier check
- test-frontend: Vitest with coverage
- build-frontend: Vite production build
- build-backend: Maven compile + test (Java 21)"
```

---

## Task 7: Phase 7 — Documentation Structure

**Files:**

- Create: `docs/guides/getting-started.md`
- Create: `docs/adr/README.md`
- Create: `docs/adr/0001-harness-engineering.md`
- Modify: `.gitignore`

- [ ] **Step 1: Update `.gitignore`**

Append to the end of `.gitignore`:

```
# Playwright
playwright-report/
blob-report/
```

- [ ] **Step 2: Create `docs/guides/getting-started.md`**

````markdown
# 新人上手指南

> 本指南帮助新开发者快速搭建本地开发环境。

## 前提条件

| 工具       | 版本   | 用途              |
| ---------- | ------ | ----------------- |
| Java       | 21     | 后端微服务        |
| Maven      | 3.9+   | Java 构建         |
| Node.js    | 20+    | 前端构建          |
| npm        | 10+    | 前端包管理        |
| Docker     | 20.10+ | 基础设施服务      |
| PostgreSQL | 15+    | 数据库            |
| Redis      | 7.x    | 缓存              |
| Nacos      | 2.3.x  | 服务注册/配置中心 |

## 快速启动

### 1. 克隆项目

```bash
git clone <repository-url>
cd GCRF_LibraryManagementSystem
```
````

### 2. 安装工程化工具

```bash
npm install  # 根目录：安装 husky, commitlint, lint-staged
```

### 3. 启动基础设施

```bash
cd deployment
docker-compose -f docker-compose.infrastructure.yml up -d
```

等待 PostgreSQL 和 Nacos 完全启动（约 30 秒）。

### 4. 构建后端

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd backend
mvn clean compile
```

### 5. 启动前端

```bash
cd web-admin
npm install
npm run dev  # http://localhost:3011
```

## 常用命令

| 命令                          | 位置         | 用途               |
| ----------------------------- | ------------ | ------------------ |
| `mvn clean compile`           | `backend/`   | 编译后端           |
| `mvn test -pl reader-service` | `backend/`   | 测试单个服务       |
| `npm run dev`                 | `web-admin/` | 启动前端开发服务器 |
| `npm run lint`                | `web-admin/` | 前端代码检查       |
| `npm run test`                | `web-admin/` | 前端单元测试       |
| `npm run build`               | `web-admin/` | 前端生产构建       |

## 提交规范

本项目使用 Conventional Commits，提交时会自动校验：

```
<type>(<scope>): <subject>

# 示例
feat(book): add search by ISBN
fix(web-admin): fix pagination display
docs(docs): update API documentation
```

**可用 scope:** gateway, auth, book, circulation, reader, system, notification, recommend, chat, analytics, common, web-admin, infra, docs

## 更多文档

- [架构文档](../architecture/architect.md) — 权威技术规格（1570 行）
- [ADR 记录](../adr/README.md) — 架构决策记录

````

- [ ] **Step 3: Create `docs/adr/README.md`**

```markdown
# Architecture Decision Records (ADR)

> 记录项目中的重要技术决策及其背景。

## 格式

每条 ADR 包含：状态、背景、决策、后果。

## 记录列表

| # | 标题 | 状态 | 日期 |
|---|------|------|------|
| 0001 | [Harness Engineering 工程化配置](0001-harness-engineering.md) | Accepted | 2026-04-13 |
````

- [ ] **Step 4: Create `docs/adr/0001-harness-engineering.md`**

```markdown
# ADR-0001: Harness Engineering 工程化配置

**状态:** Accepted
**日期:** 2026-04-13

## 背景

项目缺乏统一的代码格式、提交规范、Git hooks 和 CI 质量门禁。团队参考内部标准配置手册（适用于 pnpm + React + FastAPI），需适配到本项目的 Maven + Spring Boot + Vue 3 架构。

## 决策

1. **根目录统一管理** — 根目录 `package.json` 管理工程化工具（husky, commitlint, lint-staged），`web-admin/` 保留业务依赖
2. **渐进式采用** — ESLint 保持现有 v8 配置，不升级 flat config（风险高）
3. **跳过不适用模块** — Ruff (无 Python)、pytest (无 Python)、GCP CD (使用 Harbor)、knip (不适用于 Java)
4. **新增 CI workflow** — 与现有 build-and-push.yml 和 security-scan.yml 并行，专注 PR 质量门禁
5. **Dependabot 三生态** — npm + maven + github-actions

## 后果

- 所有提交消息强制 Conventional Commits 格式
- 前端文件提交前自动 lint + format
- PR 需通过 lint、test、build 四个 job 才可合并
- 依赖自动更新，减少安全风险
```

- [ ] **Step 5: Commit**

```bash
git add .gitignore docs/guides/getting-started.md docs/adr/README.md docs/adr/0001-harness-engineering.md
git commit -m "docs(docs): add getting-started guide and ADR structure

- docs/guides/getting-started.md: new developer onboarding
- docs/adr/: ADR index + 0001 harness engineering decision"
```

---

## Task 8: Phase 8 — Dependabot + Entropy Scan

**Files:**

- Create: `.github/dependabot.yml`
- Create: `.github/workflows/entropy-scan.yml`

- [ ] **Step 1: Create `.github/dependabot.yml`**

```yaml
version: 2
updates:
  # Frontend (npm)
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

  # Backend (Maven)
  - package-ecosystem: maven
    directory: /backend
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 5

  # GitHub Actions
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
```

- [ ] **Step 2: Create `.github/workflows/entropy-scan.yml`**

```yaml
name: Entropy Scan

on:
  schedule:
    - cron: "0 9 * * 1" # Every Monday 9am UTC
  workflow_dispatch:

jobs:
  todo-count:
    name: TODO Tracking
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Count TODOs
        run: |
          echo "## TODO Count" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Pattern | Count |" >> $GITHUB_STEP_SUMMARY
          echo "|---------|-------|" >> $GITHUB_STEP_SUMMARY
          TODO_COUNT=$(grep -r "TODO" --include="*.java" --include="*.vue" --include="*.js" backend/ web-admin/src/ | wc -l || echo 0)
          FIXME_COUNT=$(grep -r "FIXME" --include="*.java" --include="*.vue" --include="*.js" backend/ web-admin/src/ | wc -l || echo 0)
          HACK_COUNT=$(grep -r "HACK" --include="*.java" --include="*.vue" --include="*.js" backend/ web-admin/src/ | wc -l || echo 0)
          echo "| TODO | $TODO_COUNT |" >> $GITHUB_STEP_SUMMARY
          echo "| FIXME | $FIXME_COUNT |" >> $GITHUB_STEP_SUMMARY
          echo "| HACK | $HACK_COUNT |" >> $GITHUB_STEP_SUMMARY
```

- [ ] **Step 3: Validate YAML syntax**

Run:

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/dependabot.yml'))" && echo "dependabot OK"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/entropy-scan.yml'))" && echo "entropy-scan OK"
```

Expected: Both print "OK".

- [ ] **Step 4: Commit**

```bash
git add .github/dependabot.yml .github/workflows/entropy-scan.yml
git commit -m "chore(infra): add Dependabot and entropy scan workflow

- Dependabot: npm + maven + github-actions weekly updates
- Entropy scan: weekly TODO/FIXME/HACK count report"
```

---

## Task 9: Verification

- [ ] **Step 1: Verify pre-commit hook works end-to-end**

Create a test file, stage it, and commit with a valid message:

Run:

```bash
echo "# Test" > /tmp/verify-hook.md
cp /tmp/verify-hook.md verify-hook.md
git add verify-hook.md
git commit -m "chore(infra): verify commit hooks"
```

Expected: lint-staged runs (Prettier on .md), commitlint passes, commit succeeds.

- [ ] **Step 2: Clean up verification commit**

Run:

```bash
git rm verify-hook.md
git commit -m "chore(infra): remove hook verification file"
```

- [ ] **Step 3: Verify commitlint rejects bad scope**

Run:

```bash
echo "feat(badscope): test" | npx commitlint
```

Expected: Error about scope not being one of the allowed values.

- [ ] **Step 4: Verify Vitest passes**

Run:

```bash
cd web-admin && npm run test
```

Expected: 1 test passed.

- [ ] **Step 5: Verify frontend build still works**

Run:

```bash
cd web-admin && npm run build
```

Expected: Build succeeds, `dist/` created.

- [ ] **Step 6: Final commit — spec and plan docs**

```bash
git add docs/specs/
git commit -m "docs(docs): add harness engineering spec and implementation plan"
```
