# 区域图书馆云平台 — 顶层架构

**日期：** 2026-04-30
**状态：** Approved
**主 spec：** [`docs/specs/2026-04-30-regional-platform-master-design.md`](../specs/2026-04-30-regional-platform-master-design.md)

---

## 1. 系统拓扑

```
                          ┌──────────────────────────────────┐
                          │   教育局 / 区域中心 K8s 集群       │
                          │  ─────────────────────────────   │
                          │   API Gateway + IAM (OAuth2)     │
                          │   PostgreSQL (per-school schema) │
                          │   Redis / RabbitMQ / MinIO       │
                          │   ElasticSearch (跨校检索)        │
                          └──────────────────────────────────┘
                                       │
       ┌───────────────────────────────┼───────────────────────────────┐
       ▼                               ▼                               ▼
┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│  ① 区域云平台    │         │  ② 区域 OPAC     │         │  ③ 微信图书馆     │
│  Vue3 + Naive UI │         │  Vue3 SSR        │         │  uni-app 单源码   │
└──────────────────┘         └──────────────────┘         └──────────────────┘
                                                                     │
                          ┌──────────────────────────────────────────┘
                          ▼
        ┌──────────────────────────────────────────┐
        │  ④ 校园系统 + Edge Agent (Docker)         │
        │     断网仍可借还，重连增量同步             │
        └──────────────────────────────────────────┘
```

## 2. 后端微服务组成（K8s）

### 2.1 已有 8 个（Phase 1 已上线）

| 服务                   | 端口 | 职责                                     |
| ---------------------- | ---- | ---------------------------------------- |
| `gateway-service`      | 8080 | API 网关、限流、路由                     |
| `auth-service`         | 8081 | IAM / OIDC（本轮升级为多租户多登录方式） |
| `book-service`         | 8082 | 编目 / MARC 数据                         |
| `circulation-service`  | 8083 | 借还 / 续借 / 预约                       |
| `reader-service`       | 8084 | 读者 / 读者证                            |
| `system-service`       | 8085 | 用户 / 角色 / 配置 / 备份 / 反馈 / 消息  |
| `notification-service` | 8086 | 邮件 / 短信 / 微信通知调度               |
| `analytics-service`    | 8087 | 统计 / 报表 / 导出                       |
| `chat-service`         | 8089 | AI 问答（已存在）                        |

### 2.2 本轮新增 6 个

| 服务                | 端口 | 职责                                                            |
| ------------------- | ---- | --------------------------------------------------------------- |
| `org-service`       | 8090 | 组织树 (ltree) + 多租户 schema 路由                             |
| `standard-service`  | 8091 | 馆藏标准、达标测算、**不适宜书库扫描**                          |
| `opac-service`      | 8092 | 区域 OPAC 公开 API（高 QPS、缓存重）                            |
| `wechat-service`    | 8093 | 公众号 / 小程序后端                                             |
| `sync-service`      | 8094 | Edge Agent 同步中枢 (WSS + RabbitMQ)                            |
| `recommend-service` | 8088 | 智能推荐采购 + 智能馆藏分析（已存在 chat-service 内，本轮独立） |

## 3. 数据存储

| 存储            | 用途                               | 容量预估（单区域 200 校） |
| --------------- | ---------------------------------- | ------------------------- |
| PostgreSQL 15   | 主库（公共 + 200 校 schema）       | 200 GB                    |
| Redis 7         | 缓存（OPAC 热搜 / token / 排行榜） | 8 GB                      |
| RabbitMQ 3.12   | 异步消息（同步队列 / 通知）        | 2 GB                      |
| ElasticSearch 8 | 跨校全文检索（按校 index + alias） | 80 GB                     |
| MinIO           | 对象存储（书封 / MARC / 报表）     | 500 GB                    |

## 4. 横切关注点（详见各子文档）

| 关注点              | 子文档                                                             |
| ------------------- | ------------------------------------------------------------------ |
| 多租户隔离          | [`01-multi-tenant-isolation.md`](01-multi-tenant-isolation.md)     |
| 部署拓扑            | [`02-deployment-topology.md`](02-deployment-topology.md)           |
| Edge Agent 同步协议 | [`03-edge-agent-sync-protocol.md`](03-edge-agent-sync-protocol.md) |
| IAM SSO             | [`04-iam-sso.md`](04-iam-sso.md)                                   |
| 数据模型            | [`05-data-model.md`](05-data-model.md)                             |
| API 规范            | [`06-api-spec.md`](06-api-spec.md)                                 |

## 5. 技术栈

### 后端

- Java 21 / Spring Boot 3.2.2 / Spring Cloud 2023.0.0 / Spring Cloud Alibaba 2023.0.1.0
- MyBatis-Plus 3.5.9 / PostgreSQL JDBC 42.7.1
- Flyway 数据库迁移（每服务独立 V001+ migration）
- OpenFeign + SimpleDiscoveryClient（K8s service discovery）

### 前端

- Vue 3 + Vite + TypeScript（区域云 + OPAC）
- Element Plus / Naive UI（按平台风格）
- uni-app（公众号 H5 + 小程序）

### Edge Agent

- Spring Boot 3 精简（WAR < 50MB）
- SQLite + AES-256
- WebSocket + JWT + mTLS
- Docker Compose 单机部署

### CI/CD

- GitHub Actions / GitLab CI（按客户选）
- Docker buildx（amd64）
- ArgoCD GitOps（区域 K8s）+ docker-compose pull（学校 Edge Agent）

## 6. 监控与可观测

- **Metrics**：Prometheus + Grafana（已有）
- **Logging**：Loki / ELK（按客户选）
- **Tracing**：OpenTelemetry → Tempo
- **Alerting**：AlertManager → 钉钉机器人 + 邮件
- **Edge Agent**：心跳上报到 sync-service，断连超 5 min 告警

## 7. 演进与扩展

- **学校私有化**：将某校从共享 schema 迁出到独立 PG（已设计 ADR-001 退路）
- **跨区域**：单区域演化为多区域时，各区域独立 K8s + 跨区域 OPAC 联邦
- **AI**：chat-service 升级为 RAG（用区域所有 MARC + 借阅数据）
- **国际化**：所有文案外置 i18n（中文 / 英文 / 维语等）

---

**关联文档**：

- [主 spec](../specs/2026-04-30-regional-platform-master-design.md)
- [PRD overview](../prd/00-overview.md)
- [ADR-001 多租户](../adr/ADR-001-multi-tenant-strategy.md)
- [ADR-002 部署拓扑](../adr/ADR-002-deployment-topology.md)
- [ADR-003 微信前端](../adr/ADR-003-wechat-frontend-uniapp.md)
- [ADR-004 Edge Agent 同步](../adr/ADR-004-edge-agent-sync.md)
