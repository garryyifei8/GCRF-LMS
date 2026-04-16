# Coverage Baseline — 2026-04-16

本文档为 GCRF Library Management System 的首次覆盖率基线。**不作为 CI 门控阈值**，仅用于可视化当前测试覆盖情况 + 识别补测 hotspot。

---

## 运行环境

| 项           | 版本                                     |
| ------------ | ---------------------------------------- |
| 前端         | Vitest 4.1.4 + `@vitest/coverage-v8`     |
| 后端         | JUnit 5 + JaCoCo 0.8.11                  |
| 日期         | 2026-04-16                               |
| 前端测试总数 | 402（54 个 test 文件）                   |
| 前端 E2E     | 66（Playwright，不计入 vitest coverage） |
| 后端测试     | 依服务而定                               |

---

## 前端（Vitest + v8）

### 总体

| 维度       | 覆盖率     | 通过/总数   |
| ---------- | ---------- | ----------- |
| Statements | **49.58%** | 2671 / 5387 |
| Branches   | **46.22%** | 1580 / 3418 |
| Functions  | **41.91%** | 964 / 2300  |
| Lines      | **51.15%** | 2535 / 4956 |

### Top 10 低覆盖文件（lines %）

| #   | 文件                               | Lines  | Functions | 备注               |
| --- | ---------------------------------- | ------ | --------- | ------------------ |
| 1   | `src/layouts/MainLayout.vue`       | 0%     | 0%        | 布局组件，无测试   |
| 2   | `src/utils/excel.js`               | 0%     | 0%        | Excel 导入导出工具 |
| 3   | `src/views/circulation/return.vue` | 22.68% | 13.2%     | 仅 smoke test      |
| 4   | `src/views/readers/teachers.vue`   | 25.69% | 16.21%    | 仅 smoke test      |
| 5   | `src/views/books/inventory.vue`    | 31.28% | 14.77%    | 仅 smoke test      |
| 6   | `src/views/books/catalog.vue`      | 32.70% | 21.12%    | 仅 smoke test      |
| 7   | `src/views/readers/card.vue`       | 32.67% | 27.81%    | 仅 smoke test      |
| 8   | `src/views/books/collection.vue`   | 33.46% | 20.58%    | 仅 smoke test      |
| 9   | `src/views/system/backup.vue`      | 34.18% | 15.15%    | 仅 smoke test      |
| 10  | `src/views/readers/students.vue`   | 36.29% | 21.31%    | 含 5 个交互测试    |

### 解读

- **已覆盖充分**：stores、utils/request、utils/errorHandler、ui 组件（Button/Card/Input/StatCard 等）、API 模块（P4A/P4B/P4C）
- **Views 层普遍较低**：因采用"smoke + 5 核心页交互测试"策略，非核心页仅有 smoke 测试覆盖 mount 路径
- **零覆盖**：`MainLayout.vue`（布局组件）、`excel.js`（工具函数）— 补测可显著拉高指标

---

## 后端（JaCoCo）

### 已测量模块

| 模块            | Instruction           | Branch              | Line            |
| --------------- | --------------------- | ------------------- | --------------- |
| common-core     | **49.7%** (1609/3236) | **31.8%** (133/418) | 63.3% (371/586) |
| common-web      | **71.6%** (603/842)   | **55.3%** (63/114)  | 63.6% (124/195) |
| common-mybatis  | **92.3%** (96/104)    | **100.0%** (6/6)    | 89.7% (26/29)   |
| common-security | **100.0%** (142/142)  | N/A                 | 100.0% (40/40)  |
| gateway-service | **89.0%** (973/1093)  | **64.9%** (72/111)  | 88.8% (199/224) |

### 未测量模块（本次运行失败/跳过）

| 模块                 | 原因                                                        |
| -------------------- | ----------------------------------------------------------- |
| auth-service         | 13 个集成测试失败（本地 PostgreSQL `postgres` role 不存在） |
| book-service         | 因 auth-service 失败，多模块构建中止                        |
| circulation-service  | 同上                                                        |
| reader-service       | 同上                                                        |
| system-service       | 同上                                                        |
| notification-service | 同上                                                        |
| analytics-service    | 同上                                                        |
| chat-service         | 同上                                                        |
| recommend-service    | 同上                                                        |

### 后续复测所需

1. 启动 Testcontainers 所需 Docker daemon（已就位）
2. auth-service 的集成测试应该使用 `BaseIntegrationTest` 的 Testcontainers PostgreSQL，而非本地 DB — 需检查该服务的测试是否正确继承该基类
3. 执行：`mvn test -fae`（`fail-at-end` 允许单模块失败后继续其他模块）

### 解读

- **高覆盖**：gateway-service (89%)、common-mybatis (92%)、common-security (100%)、common-web (72%) — P2A-D 工作结果
- **中等**：common-core (50%)
- **未知**：8 个业务服务本次未成功测量，但既往 P2A/B/C/D 投入了大量 service + controller 测试，预期不会太低

---

## CI 集成

- **前端**：`ci.yml` 的 `test-frontend` job 已运行 `test:coverage` 并上传 `web-admin/coverage/` artifact
- **后端**：`ci.yml` 的 `build-backend` job 已配置上传 `backend/**/target/site/jacoco/` artifact（T3 刚加）
- **阈值**：无（有意保持，避免阻塞合并）

---

## 补测建议优先级

### 高（1 小时工作量可显著提升指标）

- `src/layouts/MainLayout.vue`（0%）— 一个 smoke + 一个 navigation toggle 测试，可能能拉到 40%+
- `src/utils/excel.js`（0%）— 工具函数，mock FileReader/Blob 后易测，预期可到 80%+
- 后端重跑 8 个业务服务的 JaCoCo 拿真实基线

### 中（视业务需要）

- 非核心 view 的交互测试（目前仅 smoke）— circulation/return、readers/teachers、books/inventory 等
- common-core 分支覆盖（31.8% → 60%+）— 工具类的边界条件

### 低（无需补齐）

- View 层的完整集成测试 — E2E 已覆盖核心业务流程（P6 66 tests），不必重复投入
- 前端 `dashboard/index.vue`（1362 行）— 有 smoke + 4 个交互测试，本质复杂度决定无法轻易拉高

---

## 附录：如何本地查看报告

```bash
# 前端 HTML 报告
cd web-admin
npm run test:coverage
open coverage/index.html

# 后端 HTML 报告（需先跑 mvn test）
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn test -fae  # -fae = fail-at-end
open gateway-service/target/site/jacoco/index.html
```

CI artifact 下载：Actions → 选中 run → Artifacts → `frontend-coverage` / `backend-jacoco-reports`。
