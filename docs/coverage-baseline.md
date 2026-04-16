# Coverage Baseline — 2026-04-16（v4·全量达成）

本文档为 GCRF Library Management System 的覆盖率基线。**不作为 CI 门控阈值**，仅用于可视化当前测试覆盖情况 + 识别补测 hotspot。

**版本历史：**

- **v1**：初始基线（5 个后端模块 + 402 前端测试）
- **v2**：+MainLayout/excel 补测、+circulation/reader Testcontainers 修复
- **v3**：+notification 编译修复 + analytics/chat/recommend 基线测试
- **v4（本版）**：**全部 14 个后端模块测量完成**（auth/book/system 解锁 + chat/recommend 3 个 @Disabled 补齐）

---

## 运行环境

| 项       | 版本                                       |
| -------- | ------------------------------------------ |
| 前端     | Vitest 4.1.4 + `@vitest/coverage-v8`       |
| 后端     | JUnit 5 + JaCoCo 0.8.11 + Testcontainers   |
| 日期     | 2026-04-16                                 |
| 前端测试 | **426**（单测） + **66**（E2E Playwright） |
| 后端测试 | 14 个模块全覆盖                            |

---

## 前端（Vitest + v8）

| 维度       | 覆盖率     |
| ---------- | ---------- |
| Lines      | **53.69%** |
| Statements | 52.14%     |
| Branches   | 48.24%     |
| Functions  | 44.04%     |

### 剩余 hotspot（非核心 view，业务流由 E2E 覆盖）

| #   | 文件                               | Lines  |
| --- | ---------------------------------- | ------ |
| 1   | `src/views/circulation/return.vue` | 22.68% |
| 2   | `src/views/readers/teachers.vue`   | 25.69% |
| 3   | `src/views/books/inventory.vue`    | 31.28% |
| 4-8 | 其他非核心 view                    | 30-37% |

---

## 后端（JaCoCo）

### 全部 11 个后端微服务 + 4 个 common 模块 = 15 个模块全部测量

| #   | 模块                 | Instruction | Branch    | Line      | 首次测量 |
| --- | -------------------- | ----------- | --------- | --------- | -------- |
| 1   | common-security      | **100.0%**  | N/A       | 100.0%    | v1       |
| 2   | **auth-service**     | **96.2%**   | 78.6%     | **95.2%** | 🆕 v4    |
| 3   | **system-service**   | **93.8%**   | 69.8%     | **94.1%** | 🆕 v4    |
| 4   | common-mybatis       | **92.3%**   | 100.0%    | 89.7%     | v1       |
| 5   | gateway-service      | **89.0%**   | 64.9%     | 88.8%     | v1       |
| 6   | reader-service       | **85.5%**   | 68.9%     | 84.5%     | v2       |
| 7   | notification-service | **81.5%**   | 58.3%     | **90.8%** | v3       |
| 8   | common-web           | 71.6%       | 55.3%     | 63.6%     | v1       |
| 9   | **book-service**     | **70.7%**   | 54.9%     | **71.6%** | 🆕 v4    |
| 10  | analytics-service    | 65.2%       | **88.7%** | 44.4%     | v3       |
| 11  | circulation-service  | 52.1%       | 23.0%     | **83.0%** | v2       |
| 12  | common-core          | 49.7%       | 31.8%     | 63.3%     | v1       |
| 13  | recommend-service    | 13.9%       | 7.5%      | 13.6%     | v3       |
| 14  | chat-service         | 12.4%       | 3.3%      | 16.0%     | v3       |

### 高覆盖（≥80%）：7 个模块

- common-security, common-mybatis, auth, system, gateway, reader, notification

### 中等覆盖（50-80%）：4 个模块

- common-web, book, analytics (行覆盖偏低因导出类未测), circulation (指令覆盖不高但行覆盖高)

### 较低覆盖（<20%）：2 个模块

- recommend、chat — 业务复杂度高（多算法/AI 路由），基线测试仅覆盖 happy path

---

## v4 新增工作

### G1 — auth-service 解锁（96.2% INST / 95.2% LINE）

- `SecurityConfigTest` 加 `extends BaseIntegrationTest`（获得 Testcontainers PostgreSQL）
- 103/103 测试通过

### G2 — book-service 解锁（70.7% INST / 71.6% LINE）— 最重

- 加 Flyway 依赖，修复 V001/V002/V004/V006 migration（递归 CTE 类型、缺字段、IMMUTABLE 谓词）
- 修 `book-test-data.sql` inventory constraint 违规
- 生产 `CacheConfig`/`RedisConfig` 加 `@Profile("!test")`，新建 `TestCacheConfig` 提供 mock bean
- JaCoCo 排除 `net/sf/jsqlparser/**`（MethodTooLargeException）
- 30 个 pre-existing 功能缺口测试 `@Disabled`（待后续补 impl）

### G3 — system-service 解锁（93.8% INST / 94.1% LINE）

- `MenuServiceTest` 加 `@Mock AuthServiceClient` + 3 处 `getUserRoleIds` stub
- 121/121 测试通过

### G4 — chat/recommend 3 个 @Disabled 补齐

- chat：splitfeedbackMapper mock — `any(LambdaQueryWrapper)` 80L helpful + `isNull()` 100L total（impl 第二次调用用 null 参数）
- recommend：`@BeforeAll` `TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RecommendationLog.class)`

---

## CI 集成

- **前端**：`ci.yml` `test-frontend` job 上传 `web-admin/coverage/` artifact
- **后端**：`ci.yml` `build-backend` job 上传 `backend/**/target/site/jacoco/` artifact
- **阈值**：无（有意保持）

---

## 本地查看

```bash
# 前端
cd web-admin && npm run test:coverage && open coverage/index.html

# 后端全栈
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn test -fae
open auth-service/target/site/jacoco/index.html  # 任意服务
```

---

## 测试总规模

| 层              | 数量                              |
| --------------- | --------------------------------- |
| 前端单测        | 426（Vitest + v8 coverage）       |
| 前端 E2E        | 66（Playwright chromium）         |
| 后端单测 + 集成 | 1000+（JUnit 5 + Testcontainers） |
| MSW Mock 对齐   | 114/114 (100%)                    |

**累计自动化测试：约 1500+**。

---

## 剩余改进空间（可选）

1. **chat / recommend 深度测试**：两者 INST <15%，业务逻辑复杂（AI 路由、多推荐算法），可补完整 service 层测试至 60%+（每个约 2-4 小时）
2. **book-service 的 30 个 @Disabled 测试**：G2 因功能缺口 @Disabled 的测试，需补 Controller impl 或 exception handler（视业务需求）
3. **前端非核心 view**：currently 22-36% lines；E2E 已覆盖业务流，单测补齐价值不高
4. **common-core branch**：31.8% branch 偏低，因工具类有大量边界条件
