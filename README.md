# GCRF 智能图书馆管理系统

国创睿峰智能图书馆管理系统（GCRF Intelligent Library Management System）— 企业级微服务架构 + Vue 3 管理后台，面向高校、公共图书馆场景设计，支持图书管理、借还流通、读者管理、AI 推荐与智能客服等核心业务。

## 快速开始

```bash
# 一键启动全栈
cd deployment
./scripts/deploy-and-smoke.sh
```

详细步骤见 [部署手册](docs/deployment/RUNBOOK.md)。

---

## 架构总览

### 技术栈

| 层次            | 技术                                                                      |
| --------------- | ------------------------------------------------------------------------- |
| **后端框架**    | Spring Boot 3.2.2, Spring Cloud 2023.0.0, Spring Cloud Alibaba 2023.0.1.0 |
| **持久层**      | MyBatis-Plus 3.5.9, PostgreSQL 15, Flyway（Schema 迁移）                  |
| **缓存 / 消息** | Redis 7, RabbitMQ 3.12                                                    |
| **服务治理**    | Nacos 2.3（注册中心 + 配置中心）                                          |
| **对象存储**    | MinIO                                                                     |
| **运行时**      | Java 21                                                                   |
| **前端**        | Vue 3, Vite, Element Plus, Pinia, Vue Router 4                            |
| **Mock**        | MSW（Mock Service Worker）                                                |
| **可观测性**    | Prometheus + Grafana + Loki + Alertmanager                                |
| **容器化**      | Docker + docker-compose                                                   |
| **CI**          | GitHub Actions                                                            |

### 微服务列表（11 个服务）

| 服务                 | 端口 | 职责                             |
| -------------------- | ---- | -------------------------------- |
| gateway-service      | 8080 | API 网关、鉴权过滤、限流         |
| auth-service         | 8081 | 登录、JWT 签发与刷新             |
| book-service         | 8082 | 图书 CRUD、分类、封面上传        |
| circulation-service  | 8083 | 借还、预约、罚款                 |
| reader-service       | 8084 | 读者管理、借阅证                 |
| system-service       | 8085 | 用户、角色、权限、部门           |
| recommend-service    | 8086 | 图书推荐引擎                     |
| chat-service         | 8087 | AI 智能客服                      |
| analytics-service    | 8089 | 数据分析、报表                   |
| notification-service | 8090 | 消息通知（邮件 / 短信 / 站内信） |
| web-admin            | 3011 | Vue 3 管理后台                   |

### 项目结构

```
.
├── backend/
│   ├── common/                   # 共享模块（core / web / security / mybatis）
│   ├── gateway-service/
│   ├── auth-service/
│   ├── book-service/
│   ├── circulation-service/
│   ├── reader-service/
│   ├── system-service/
│   ├── recommend-service/
│   ├── chat-service/
│   ├── analytics-service/
│   ├── notification-service/
│   └── pom.xml
├── web-admin/                    # Vue 3 前端
│   └── src/
│       ├── views/                # 25 个页面组件
│       ├── components/           # 11 个通用组件
│       ├── api/                  # 10 个 API 模块
│       ├── store/                # Pinia stores
│       ├── mock/                 # MSW Mock handlers（100% 覆盖）
│       └── utils/
├── deployment/
│   ├── docker-compose.infrastructure.yml
│   ├── docker-compose.services.yml
│   ├── docker-compose.monitoring.yml
│   └── scripts/
│       └── deploy-and-smoke.sh   # 一键部署 + 冒烟测试
├── docs/
│   ├── architecture/             # 架构文档
│   ├── deployment/               # 部署手册（RUNBOOK.md 等）
│   ├── specs/                    # 各阶段设计文档
│   ├── testing/                  # 测试策略文档
│   └── coverage-baseline.md      # 覆盖率基线
└── .github/workflows/            # CI 流水线
```

---

## 测试覆盖

| 层次                   | 测试数            | 工具                     |
| ---------------------- | ----------------- | ------------------------ |
| 前端 stores / utils    | 75                | Vitest                   |
| 前端 Vue 组件（11 个） | 144               | Vitest + Vue Test Utils  |
| 前端 API 模块（10 个） | 139               | Vitest                   |
| 前端 Views（25 个）    | 44                | Vitest                   |
| **前端单测合计**       | **402**           | Vitest                   |
| E2E                    | 66                | Playwright（Chromium）   |
| 后端单测 + 集成测试    | 依服务而定        | JUnit 5 + Testcontainers |
| MSW Mock 覆盖校验      | 114 / 114（100%） | 自定义 check script      |
| **总计**               | **544+**          | —                        |

覆盖率基线（前端行覆盖率 51.15%，5 个后端模块已测量）：[docs/coverage-baseline.md](docs/coverage-baseline.md)

---

## CI / CD

`.github/workflows/` 包含以下流水线：

| 文件                 | 触发       | 内容                                                                                                                          |
| -------------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `ci.yml`             | push / PR  | lint（含 MSW 覆盖校验）→ vitest + coverage → build 前端 → build + test 后端（JaCoCo）→ docker-build-smoke matrix（10 个服务） |
| `e2e.yml`            | PR         | Playwright E2E（Chromium）                                                                                                    |
| `security-scan.yml`  | 定时 / PR  | 安全扫描                                                                                                                      |
| `build-and-push.yml` | tag / 手动 | 镜像构建 + 推送镜像仓库                                                                                                       |

---

## 本地开发

### 前端

```bash
cd web-admin
npm install --legacy-peer-deps
npm run dev           # http://localhost:3011（MSW Mock 模式）
npm run test          # 单元测试（Vitest）
npm run test:e2e      # E2E 测试（Playwright）
npm run mock:check    # MSW 覆盖率校验（114/114）
npm run build         # 生产构建
```

### 后端

```bash
# 必须使用 Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

cd backend

# 编译单个模块
mvn clean compile -pl reader-service

# 测试单个模块（需 Docker 运行 Testcontainers）
mvn test -pl reader-service

# 启动服务（需先启动基础设施）
cd reader-service && mvn spring-boot:run
```

### 基础设施（本地）

```bash
cd deployment
docker-compose -f docker-compose.infrastructure.yml up -d
# 启动：PostgreSQL 15, Redis 7, RabbitMQ 3.12, Nacos 2.3, MinIO
```

### 生产部署

```bash
cd deployment
./scripts/deploy-and-smoke.sh
```

---

## 文档索引

| 文档                                                                       | 说明                                              |
| -------------------------------------------------------------------------- | ------------------------------------------------- |
| [docs/architecture/architect.md](docs/architecture/architect.md)           | **权威技术规范**（1570 行，含 PostgreSQL Schema） |
| [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md)     | 架构概览                                          |
| [docs/deployment/RUNBOOK.md](docs/deployment/RUNBOOK.md)                   | 部署操作手册                                      |
| [docs/deployment/OPERATIONS_GUIDE.md](docs/deployment/OPERATIONS_GUIDE.md) | 运维指南                                          |
| [docs/deployment/MONITORING_GUIDE.md](docs/deployment/MONITORING_GUIDE.md) | 可观测性配置                                      |
| [docs/coverage-baseline.md](docs/coverage-baseline.md)                     | 测试覆盖率基线                                    |
| [docs/specs/](docs/specs/)                                                 | 各阶段设计文档目录                                |

---

## 开发进度

项目历经 P0 → P6 + 扫尾共 8 个迭代阶段，已达到生产就绪状态：

| 阶段     | 内容                                                                                                     | 状态 |
| -------- | -------------------------------------------------------------------------------------------------------- | ---- |
| **P0**   | Gateway / API 契约对齐                                                                                   | 完成 |
| **P1**   | DB Schema 设计 + Flyway 迁移                                                                             | 完成 |
| **P2A**  | Testcontainers 基础设施搭建                                                                              | 完成 |
| **P2B**  | 后端服务单元测试                                                                                         | 完成 |
| **P2C**  | Controller 集成测试                                                                                      | 完成 |
| **P2D**  | 新代码测试 + Gateway / Logback 修复                                                                      | 完成 |
| **P3**   | 部署生产化（7 个 Dockerfile、完整 docker-compose、deploy-and-smoke.sh、RUNBOOK、CI docker-build matrix） | 完成 |
| **P4A**  | 前端 Stores + Utils 测试（75 个）                                                                        | 完成 |
| **P4B**  | 11 个 Vue 组件测试（144 个）                                                                             | 完成 |
| **P4C**  | 10 个 API 模块测试（139 个）                                                                             | 完成 |
| **P4D**  | 25 个 Views 测试（44 个）                                                                                | 完成 |
| **P5**   | MSW Mock 100% 覆盖对齐 + 自动校验脚本                                                                    | 完成 |
| **P6**   | Playwright E2E 测试（66 个）                                                                             | 完成 |
| **扫尾** | 覆盖率基线建立（前端 51.15%，5 个后端模块）                                                              | 完成 |

---

## 关键约束

- **Java 版本**：必须使用 Java 21（Spring Boot 3.2.2 要求）
- **数据库**：PostgreSQL 15+（使用 JSONB、数组类型、分区、全文搜索等 PG 特性，不兼容 MySQL）
- **依赖版本**：已锁定，禁止擅自升级（详见 `backend/pom.xml`）

---

## License

© GCRF 国创睿峰 2025-2026. All rights reserved.
