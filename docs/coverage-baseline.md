# Coverage Baseline — 2026-04-16（更新版）

本文档为 GCRF Library Management System 的覆盖率基线。**不作为 CI 门控阈值**，仅用于可视化当前测试覆盖情况 + 识别补测 hotspot。

**v2 更新**：补了 MainLayout + excel.js 零覆盖 + 重跑后端（含 auth/reader Testcontainers 修复）。

---

## 运行环境

| 项           | 版本                                        |
| ------------ | ------------------------------------------- |
| 前端         | Vitest 4.1.4 + `@vitest/coverage-v8`        |
| 后端         | JUnit 5 + JaCoCo 0.8.11 + Testcontainers    |
| 日期         | 2026-04-16                                  |
| 前端测试总数 | **426**（原 402 + MainLayout 8 + excel 16） |
| 前端 E2E     | 66（Playwright，不计入 vitest coverage）    |
| 后端测试     | 依服务而定（见下表）                        |

---

## 前端（Vitest + v8）

### 总体

| 维度       | v1     | **v2**     | Δ     |
| ---------- | ------ | ---------- | ----- |
| Statements | 49.58% | **52.14%** | +2.56 |
| Branches   | 46.22% | **48.24%** | +2.02 |
| Functions  | 41.91% | **44.04%** | +2.13 |
| Lines      | 51.15% | **53.69%** | +2.54 |

通过 / 总数：Lines 2661 / 4956、Statements 2809 / 5387、Branches 1649 / 3418、Functions 1013 / 2300。

### Top 10 低覆盖文件（v2 · lines %）

零覆盖的 MainLayout 和 excel 已补测。剩余 hotspot 均为非核心 view（仅 smoke test）。

| #   | 文件                               | Lines  | 备注            |
| --- | ---------------------------------- | ------ | --------------- |
| 1   | `src/views/circulation/return.vue` | 22.68% | 仅 smoke test   |
| 2   | `src/views/readers/teachers.vue`   | 25.69% | 仅 smoke test   |
| 3   | `src/views/books/inventory.vue`    | 31.28% | 仅 smoke test   |
| 4   | `src/views/books/catalog.vue`      | 32.70% | 仅 smoke test   |
| 5   | `src/views/readers/card.vue`       | 32.67% | 仅 smoke test   |
| 6   | `src/views/books/collection.vue`   | 33.46% | 仅 smoke test   |
| 7   | `src/views/system/backup.vue`      | 34.18% | 仅 smoke test   |
| 8   | `src/views/readers/students.vue`   | 36.29% | 含 5 个交互测试 |

### 解读

- **已覆盖充分**：stores、utils（含 excel）、ui 组件、API 模块、MainLayout
- **非核心 view** 仍为主要缺口，但业务流程由 E2E 覆盖（66 tests），不必重复投入单测

---

## 后端（JaCoCo）

### 已测量模块（v2 · 8 个模块）

| 模块                    | Instruction | Branch | Line      | 备注  |
| ----------------------- | ----------- | ------ | --------- | ----- |
| common-core             | 49.7%       | 31.8%  | 63.3%     | v1    |
| common-web              | 71.6%       | 55.3%  | 63.6%     | v1    |
| common-mybatis          | 92.3%       | 100.0% | 89.7%     | v1    |
| common-security         | 100.0%      | N/A    | 100.0%    | v1    |
| gateway-service         | **89.0%**   | 64.9%  | 88.8%     | v1    |
| **circulation-service** | **52.1%**   | 23.0%  | **83.0%** | 🆕 v2 |
| **reader-service**      | **85.5%**   | 68.9%  | **84.5%** | 🆕 v2 |

### 未测量模块（pre-existing 测试 bug）

| 模块                 | 阻塞原因                                                                                     |
| -------------------- | -------------------------------------------------------------------------------------------- |
| auth-service         | Testcontainers 启动后仍有测试失败（具体需检查 surefire 报告）                                |
| book-service         | Surefire 测试失败（中断 JaCoCo 报告生成）                                                    |
| system-service       | Surefire 测试失败                                                                            |
| notification-service | 编译失败（WebSocketNotificationServiceImplTest `convertAndSend` ambiguous method reference） |
| analytics-service    | 无测试代码                                                                                   |
| chat-service         | 无测试代码                                                                                   |
| recommend-service    | 无测试代码                                                                                   |

**本次 F1 改进：**

- 为 auth-service 和 reader-service 添加 `src/test/resources/bootstrap.yml` 禁用 Nacos（修复 Testcontainers 测试的 Cloud Config 启动失败）
- reader-service 首次跑出 85.5% 指令覆盖

### 后续修复建议

1. **notification-service 编译错误**：修 `WebSocketNotificationServiceImplTest` 的 mock — 用 `doAnswer` 或明确 cast 消除重载歧义（10 分钟）
2. **auth-service / book-service / system-service**：逐个查 `target/surefire-reports/*.txt` 看具体失败用例
3. **analytics / chat / recommend**：补 service 层单测（P2B 当时跳过）

### 解读

- **高覆盖 (≥85%)**：gateway-service、reader-service、common-mybatis、common-security
- **中等 (60-85%)**：circulation-service、common-web
- **较低 (<60%)**：common-core（数据工具边界条件多）
- **待测**：4 个业务服务 + 3 个无测试服务

---

## CI 集成

- **前端**：`ci.yml` 的 `test-frontend` job 已上传 `web-admin/coverage/` artifact
- **后端**：`ci.yml` 的 `build-backend` job 已上传 `backend/**/target/site/jacoco/` artifact
- **阈值**：无（有意保持，避免阻塞合并）

---

## 本地查看

```bash
# 前端 HTML
cd web-admin && npm run test:coverage && open coverage/index.html

# 后端 HTML
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn test -fae
open reader-service/target/site/jacoco/index.html  # 示例
```

CI artifact：Actions → 选中 run → Artifacts → `frontend-coverage` / `backend-jacoco-reports`。
