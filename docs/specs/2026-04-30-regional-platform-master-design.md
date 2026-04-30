# 区域图书馆云平台 — 主设计文档

**日期：** 2026-04-30
**状态：** Draft → 待用户审阅
**作者：** GCRF 工程团队 / Claude
**预期阅读对象：** 区域教育局、学校 IT 决策者、本项目工程师

---

## 背景与目标

GCRF 校园图书馆管理系统（本仓 `backend/` + `web-admin/`）已完成 Phase 1 上线反馈补完，
覆盖单校管理后台。本轮根据最新区域级需求，将系统扩展为**4 平台一体的区域图书馆云**：

1. **区域图书馆云平台** — 教育局/区域级 Web 后台
2. **区域中小学图书馆公共检索平台 (OPAC)** — 公开检索网站
3. **区域中小学图书馆微信图书馆** — 公众号 + 小程序
4. **校园图书馆管理系统** — 学校级 Web 后台（基于现有 GCRF 系统升级）

**目标**：

- 一个教育局 → 多所学校（20 ~ 200+） → 各年级班级，统一管理
- 跨校馆藏检索、统一身份、馆藏标准达标测算、不适宜书库防护
- 学校断网仍可借还（边缘代理），网恢复后增量同步
- 公众号 / 小程序双形态触达学生与家长
- 替代既往 4 个分散孤立的旧系统/旧文档

**非目标**：

- 不替代图书馆物理硬件（条码枪、防盗仪）— 仅提供软件接口
- 不实现完整的 BI 自助分析平台（数据中心通过密钥嵌入到甲方既有 BI 即可）
- 不实现互联网级 OPAC（QPS < 1000，区域内学生家长访问足够）

---

## 总体决策一览（已用户确认）

| 维度                | 决策                                      | 理由                                   |
| ------------------- | ----------------------------------------- | -------------------------------------- |
| **文档结构**        | 顶层 PRD + 顶层架构 + 4 子 PRD + 6 子架构 | 投标可看顶层、迭代可看子文档，并行编写 |
| **多租户隔离**      | 共享 PostgreSQL + 每校独立 schema         | 兼顾隔离强度、跨校查询性能、运维成本   |
| **部署拓扑**        | 集中式 SaaS + 校内离线代理 (Edge Agent)   | 网络断仍可借还，运维比纯边缘部署轻 10× |
| **微信前端**        | 公众号 + 小程序（uni-app 单源码）         | 兼顾"集成校园公众号"诉求与现代化体验   |
| **Edge Agent 栈**   | Java Spring Boot 精简版                   | 复用 GCRF 团队现有技术栈               |
| **Edge Agent 存储** | 内嵌 SQLite（AES-256 加密）               | 几千读者+几万册图书+当日流水足够       |
| **同步通道**        | WSS (双向 mTLS)                           | 学校防火墙友好，无需开放额外端口       |
| **API 版本**        | 沿用现有 `/api/v1/...`                    | 与既有 GCRF 系统兼容                   |
| **OPAC 游客权限**   | 仅检索，不可预约                          | 防止恶意抢约                           |

---

## 1. 顶层架构与 4 平台职责拆分

### 1.1 平台拓扑

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
│  Web 后台         │         │  公开检索站      │         │  公众号 + 小程序   │
│  教育局 / 区域管理│         │  学生/家长/公众  │         │  uni-app 一套源码 │
└──────────────────┘         └──────────────────┘         └──────────────────┘
                                                                     │
                          ┌──────────────────────────────────────────┘
                          ▼
                ┌─────────────────────────┐
                │  学校借还工位（管理员）   │
                │  浏览器 → 校园边缘代理   │
                └─────────────────────────┘
                          │
                          ▼
        ┌──────────────────────────────────────────┐
        │  ④ 校园图书馆系统（GCRF — 现有）           │
        │  Web 管理后台 + 校园边缘代理               │
        │  Docker Compose 单机部署                  │
        │  断网仍可借还，重连后增量同步              │
        └──────────────────────────────────────────┘
```

### 1.2 4 平台职责

| #   | 平台           | 主用户              | 核心职责                                       | 当前状态       |
| --- | -------------- | ------------------- | ---------------------------------------------- | -------------- |
| ①   | 区域云平台     | 教育局/区域管理员   | 多校管控、馆藏标准、组织、数据中心、不适宜书库 | 待建           |
| ②   | 区域 OPAC      | 学生/家长/教师/公众 | 跨校公共检索、排行、AI 推荐                    | 待建           |
| ③   | 微信图书馆     | 学生/家长           | 移动端借阅入口 + 通知                          | 待建           |
| ④   | 校园图书馆系统 | 校馆员              | 编目/流通/读者/盘点/统计 + 智能分析            | **本仓已存在** |

### 1.3 共享基础设施

- **IAM** (`auth-service` 升级)：单一 OIDC Provider，4 平台共享 token
- **PostgreSQL 15**：每校 schema (`school_NNN`) + 区域公共 schema (`gcrf_region`)
- **ElasticSearch 8**：跨校检索索引（每校一个 index，alias 聚合查全区）
- **RabbitMQ 3.12**：异步消息（同步队列、推送通知、报表生成）
- **Redis 7**：缓存（OPAC 热搜、token、排行榜物化）
- **MinIO**：对象存储（书封、MARC、报表 Excel/PDF）

---

## 2. 多租户组织结构与统一认证

### 2.1 组织树（最深 6 层）

```
教育局 (REGION)
  └── 区/县 (DISTRICT)              ← 可选层
       ├── 学校 (SCHOOL)
       │    ├── 分校 (SUB_SCHOOL)    ← 可选层
       │    │    └── 分馆 (BRANCH)   ← 可选层
       │    └── 学段 (STAGE: 小学/初中/高中)
       │         └── 年级 (GRADE)
       │              └── 班级 (CLASS)
```

**实现要点**：

- `org_node` 表用 PostgreSQL `ltree` 存物化路径（如 `/100/200/305/`），任意层下属一 SQL 可查
- 节点类型可由超管自定义（`org_node_type` 字典表）
- 学校层及以上节点持有 `tenant_schema` 字段，年级班级走继承
- 各级管理员只能管理本节点及子树（中间件校验 path prefix）

### 2.2 角色与权限模型（RBAC + 数据范围）

| 系统角色                       | 权限范围                       | 来源平台   |
| ------------------------------ | ------------------------------ | ---------- |
| 区域超管 (REGION_ADMIN)        | 教育局全权                     | ① 区域云   |
| 区域馆员 (REGION_LIBRARIAN)    | 区域读取 + 馆藏标准维护        | ① 区域云   |
| 学校管理员 (SCHOOL_ADMIN)      | 本校全权                       | ④ 校园系统 |
| 学校馆长 (SCHOOL_LIBRARY_HEAD) | 本校馆藏 + 用户 + 报表         | ④ 校园系统 |
| 学校馆员 (LIBRARIAN)           | 本校借还 + 编目 + 盘点         | ④ 校园系统 |
| 操作员 (OPERATOR)              | 借还工位（最小权限）           | ④ 校园系统 |
| 教师 (TEACHER)                 | 借阅 + 推荐购书 + 班级阅读报表 | ②③④        |
| 学生 (STUDENT)                 | 借阅 + 预约 + 测评             | ②③         |
| 家长 (PARENT)                  | 关联子女查阅读情况             | ③          |
| 游客 (GUEST)                   | OPAC 只读                      | ②          |

- 自定义角色：在校园系统支持按"功能模块 × 数据范围"组合自定义角色（命中招标硬性条款）
- 数据范围：`SELF` / `CLASS` / `GRADE` / `SCHOOL` / `REGION`

### 2.3 SSO 统一认证流

```
   ┌───────────┐  1. 浏览器访问任一平台      ┌───────────────┐
   │用户(学/教/家)│ ────────────────────►│ 任一平台前端   │
   └───────────┘                         └───────┬───────┘
                                                 │ 2. 未登录 → 跳 IAM
                                                 ▼
                                    ┌────────────────────────┐
                                    │  IAM (区域统一认证)      │
                                    │  /oauth2/authorize     │
                                    │  支持：账号密码 / 微信   │
                                    │       / 一卡通 / 钉钉   │
                                    └────────┬───────────────┘
                                             │ 3. 颁发 code
                                             ▼
                                    平台后端拿 code 换 access_token
                                             │
                                             ▼
                                    返回用户 + 角色 + tenant + scope
```

- **JWT claims**：`sub` (user_id) / `tenant` (school_id) / `roles[]` / `scope`
- **token 有效期**：access 30min + refresh 30days；微信小程序 7day session
- **登录方式**：账号密码 / 微信扫码 / 微信公众号一键 / 学校一卡通对接 / 钉钉
- **家长账号**：通过`parent_student_link` 表关联子女，可在微信端查看子女阅读

### 2.4 用户数据导入/导出

- **批量导入**：CSV/Excel 模板 → 自动建账号 + 分配角色 + 关联组织
- **一卡通对接**：学校提供一卡通 API，`auth-service` 定时拉学生信息同步
- **数据导出**：当前组织全量数据打包下载（zip：CSV + 头像）

---

## 3. 部署拓扑与同步协议

### 3.1 云端组件清单（在现有 GCRF K8s 集群上扩容）

```
                      ┌──────────────────────────────┐
                      │  Ingress / SLB / 反爬限流     │
                      └──────────────┬───────────────┘
                                     ▼
                      ┌──────────────────────────────┐
                      │  API Gateway (8080)          │
                      └──────────────┬───────────────┘
                                     ▼
   ┌─────────────────────────────────────────────────────────────────┐
   │                       业务微服务层                                │
   │  现有 8 个：auth/book/circulation/reader/system/                 │
   │            notify/analytics/chat                                 │
   │  新增 6 个：org/standard/opac/wechat/sync/recommend(扩展)        │
   └─────────────────────────────────────────────────────────────────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            ▼                        ▼                        ▼
   ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │ PostgreSQL 15    │  │ Redis / Kafka /  │  │ ES 8 / MinIO     │
   │ region+per-school│  │ RabbitMQ         │  │                  │
   └──────────────────┘  └──────────────────┘  └──────────────────┘
```

**新增 6 个微服务**：

- `org-service` (8090) — 组织树 + 多租户 schema 路由
- `standard-service` (8091) — 馆藏标准 + 5 大类 22 小类达标测算 + **不适宜书库**比对
- `opac-service` (8092) — 区域 OPAC 公开 API（高 QPS、缓存重）
- `wechat-service` (8093) — 公众号 / 小程序后端（OAuth2、订阅消息、模板消息）
- `sync-service` (8094) — Edge Agent 同步中枢（WSS + RabbitMQ）
- `recommend-service` (8088, 已存在) — 智能推荐采购 / 智能馆藏分析（扩展）

### 3.2 校园边缘代理（Edge Agent）

**硬件最低配置**：

- Mini PC：i3 / 8G RAM / 256G SSD（约 1000 元）或复用既有借还工位
- 网络：内网 LAN，对外 ICMP/HTTPS 出站到云端

**软件栈**：

```
学校机房：
┌─────────────────────────────────────┐
│  Docker Compose                      │
│  ─────────────────────────────────  │
│   edge-agent (Java Spring Boot 精简)│
│   ↓ 内嵌                             │
│   SQLite 本地缓存 (AES-256 加密)     │
│   ─────                              │
│   nginx 反代 (LAN 80, HTTPS 自签)   │
└─────────────────────────────────────┘
       ▲                ▲
       │ HTTP/LAN       │ 出站 HTTPS+WSS
       │                ▼
   借还工位          云端 sync-service
```

**Edge Agent 缓存内容**（启动时拉取，热更新）：

- 本校全量读者快照（含借阅额度、状态）
- 本校馆藏图书快照（书名 / ISBN / 条码 / 索书号 / 在馆状态）
- 当日借还流水
- 馆藏标准（用于本地校验额度）

### 3.3 同步协议

#### 上行（学校 → 云端）

```
1. 工位扫码借书
2. 浏览器 → edge-agent (LAN HTTP)
3. edge-agent 校验本地缓存（额度/冻结/在馆）
4. 写 SQLite + 生成幂等 ID + 写入"待同步队列"
5. 立即返回成功给工位（本地体验 < 200ms）
6. 异步推到云端：edge-agent → wss://sync.gcrf.cloud/edge/{schoolId}
7. sync-service 校验幂等 + 写 PostgreSQL
8. 云端确认后回包 ACK，edge-agent 标记已同步
```

#### 下行（云端 → 学校）

```
1. 云端某变更（区域管理员冻结某读者、新书入馆）
2. circulation-service / reader-service 投递 RabbitMQ topic
3. sync-service 路由到对应学校 WSS 长连接
4. edge-agent 接收 → 应用到 SQLite 本地缓存
```

#### 冲突解决

- **借书冲突**：上行带 `barcode + timestamp + idempotency_key`，云端先到先借，后到 `409 Conflict`，edge-agent 收到后回滚本地 SQLite + 提示工位
- **读者状态**：云端为权威，下行覆盖本地
- **断网重连**：edge-agent 上传积压队列 + 拉取离线期间云端变更（增量游标）

### 3.4 安全

- Edge Agent 启动需输入"学校 deploy_token"（云端预生成，绑定 schoolId + IP 段）
- WSS 双向 mTLS（云端给每个学校颁发证书）
- 本地 SQLite AES-256 加密
- 浏览器→edge-agent 走 LAN，但仍要求 JWT 校验

---

## 4. 数据模型与 API 边界

### 4.1 数据模型分层

#### 区域公共 schema `gcrf_region`（多校共享）

| 实体                                               | 关键字段                                                              | 用途                |
| -------------------------------------------------- | --------------------------------------------------------------------- | ------------------- |
| `org_node`                                         | id, parent_id, type, path(ltree), tenant_schema                       | 组织树（最多 6 层） |
| `user`                                             | id, login, password_hash, region_id, school_id, status, identity_type | 全局用户            |
| `role` / `permission` / `user_role` / `data_scope` | 标准 RBAC + 数据范围                                                  | 权限                |
| `parent_student_link`                              | parent_user_id, student_user_id, relation                             | 家长-子女关联       |
| `collection_standard`                              | standard_key, school_type, target_value                               | 馆藏标准            |
| `inappropriate_book`                               | isbn, title, reason, source, valid_from                               | **不适宜书库**      |
| `region_config`                                    | key, value, region_id                                                 | 区域参数            |
| `cross_school_book_index` _(ES)_                   | school_id, isbn, title, author, classification, available             | 跨校检索            |

#### 学校 schema `school_<id>`（每校独立）

| 实体                                   | 关键字段                                                              | 用途                   |
| -------------------------------------- | --------------------------------------------------------------------- | ---------------------- |
| `book_catalog`                         | id, isbn, title, marc_data(jsonb), classification, category           | 编目 (CNMARC/USMARC)   |
| `book_copy`                            | id, catalog_id, barcode, call_no, location_id, status                 | 复本/典藏              |
| `collection_location`                  | id, name, parent_id（馆藏地树）                                       | 馆藏地                 |
| `reader`                               | id, user_id, card_number, grade, class, max_borrow                    | 本校读者               |
| `borrow_record`                        | id, copy_id, reader_id, borrow_at, due_at, return_at, idempotency_key | 借阅流水               |
| `reservation`                          | id, catalog_id, reader_id, status, hold_until                         | 预约（含微信进馆预约） |
| `inventory_task`                       | id, status, total, scanned, missing                                   | 盘点任务               |
| `purchase_suggestion`                  | id, isbn, score, source                                               | **智能采购推荐**       |
| `collection_analysis_report`           | id, generated_at, score_overall, jsonb_dimensions                     | **智能馆藏分析**       |
| `reading_test` / `reading_test_result` | 题目 / 学生答题                                                       | 阅读能力测评           |
| `book_review`                          | catalog_id, reader_id, rating, comment                                | 评分（推荐用）         |
| `wx_visit_reservation`                 | reader_id, slot, status                                               | 进馆预约               |

### 4.2 API 边界

#### ① 区域云平台

| 类别       | 关键 endpoint                                                                                                             |
| ---------- | ------------------------------------------------------------------------------------------------------------------------- | -------- |
| 组织管理   | `GET/POST /api/v1/org/nodes`, `POST /org/nodes/{id}/move`                                                                 |
| 用户管理   | `GET /api/v1/users?node=`, `POST /users/import`, `GET /users/export`                                                      |
| 馆藏资源   | `GET /api/v1/catalog/search`, `GET /catalog/stats?groupBy=school                                                          | orgNode` |
| 馆藏标准   | `GET/PUT /api/v1/standards`, `POST /standards/check?node=`, `GET /reports/qualification`                                  |
| 不适宜书库 | `GET /api/v1/inappropriate-books`, `POST /inappropriate-books/scan/{schoolId}`, `POST /inappropriate-books/auto-takedown` |
| 数据中心   | `GET /api/v1/analytics/region/*`, `POST /dashboard/keys`, `GET /dashboard/embed/{key}`                                    |

#### ② 区域 OPAC（公开，需限流）

| 类别 | 关键 endpoint                                              |
| ---- | ---------------------------------------------------------- |
| 检索 | `GET /api/v1/opac/search?q=&clc=&school=&adv=`             |
| 浏览 | `GET /api/v1/opac/clc/{code}`                              |
| 详情 | `GET /api/v1/opac/books/{isbn}` 含跨校在馆状态             |
| 排行 | `GET /api/v1/opac/rankings/{type}` (借阅/检索/查看/检索词) |
| 推荐 | `GET /api/v1/opac/recommend/related?isbn=`                 |
| 新书 | `GET /api/v1/opac/new-arrivals?school=`                    |

#### ③ 微信图书馆

| 类别     | 关键 endpoint                                                               |
| -------- | --------------------------------------------------------------------------- |
| 账号     | `POST /api/v1/wx/oauth/login`, `POST /wx/bind/cardNumber`                   |
| 借阅     | `GET /api/v1/wx/my/borrows`, `POST /wx/reservations`, `POST /wx/renew/{id}` |
| 子女     | `GET /api/v1/wx/children/{id}/reading-report`（家长）                       |
| 书单     | `GET /api/v1/wx/booklists`, `GET /wx/booklists/{id}/guide`                  |
| 测评     | `POST /api/v1/wx/reading-tests/{id}/submit`, `GET /wx/my/test-results`      |
| 进馆预约 | `POST /api/v1/wx/visit-reservation`, `GET /wx/visit-slots`                  |
| 通知     | `POST /api/v1/wx/subscribe-message/send` (云端调用)                         |

#### ④ 校园图书馆

| 类别   | 关键 endpoint                                                                                        |
| ------ | ---------------------------------------------------------------------------------------------------- |
| 编目   | `POST /api/v1/catalog/marc/import`, `POST /catalog/copies/batch`, `GET /catalog/marc/by-isbn/{isbn}` |
| 书标   | `POST /api/v1/print/labels?range=&isbn=&date=`                                                       |
| 馆藏地 | `POST /api/v1/catalog/copies/transfer`（按分类号/条码/日期）                                         |
| 流通   | `POST /api/v1/borrow`, `POST /return`, `POST /renew`, `POST /reservations`                           |
| 读者   | `GET/POST/PUT/DELETE /api/v1/readers`, `POST /readers/batch-cancel-by-grade`                         |
| 智能   | `GET /api/v1/intelligence/collection-analysis`, `GET /intelligence/purchase-suggestions`             |
| 统计   | `GET /api/v1/analytics/school/*`（按年级/班级/分类）                                                 |
| 角色   | `POST /api/v1/roles`, `PUT /roles/{id}/permissions`                                                  |

#### ⚙ Edge Agent ↔ Cloud（同步协议）

| 通道 | 关键 endpoint                                    |
| ---- | ------------------------------------------------ |
| WSS  | `wss://sync.gcrf.cloud/edge/{schoolId}` 双向消息 |
| HTTP | `GET /sync/snapshot/{schoolId}?since=` 增量快照  |
| HTTP | `POST /sync/queue/{schoolId}` 离线积压批量上报   |

---

## 5. 文档结构与归档计划

### 5.1 新文档目录

```
docs/
├── prd/                                ← 新建
│   ├── 00-overview.md                  顶层 PRD（4 平台总览 + 边界）
│   ├── 01-regional-cloud-platform.md
│   ├── 02-regional-opac.md
│   ├── 03-wechat-library.md
│   └── 04-campus-library.md
│
├── architecture/                       ← 扩展现有
│   ├── 00-overview.md                  顶层架构
│   ├── 01-multi-tenant-isolation.md    B 方案 schema 隔离
│   ├── 02-deployment-topology.md       C 方案 SaaS+边缘代理
│   ├── 03-edge-agent-sync-protocol.md  Edge Agent + WSS 同步协议
│   ├── 04-iam-sso.md                   IAM 统一认证
│   ├── 05-data-model.md                数据模型（公共 + 学校 schema）
│   └── 06-api-spec.md                  API 规范 + 命名约定
│
├── adr/                                ← 已有，新增 4 条
│   ├── ADR-001-multi-tenant-strategy.md
│   ├── ADR-002-deployment-topology.md
│   ├── ADR-003-wechat-frontend-uniapp.md
│   └── ADR-004-edge-agent-sync.md
│
├── specs/                              ← 已有
│   └── 2026-04-30-regional-platform-master-design.md  (本文件)
│
├── development/                        ← 已有
├── deployment/                         ← 已有
├── testing/                            ← 已有
├── api/                                ← 已有
└── archives/legacy-2026-04/            ← 归档目标
```

### 5.2 过时文档归档清单（git mv，保留历史）

| 源路径                                                                          | 目标路径                                        | 理由                              |
| ------------------------------------------------------------------------------- | ----------------------------------------------- | --------------------------------- |
| `library-backend/`                                                              | `docs/archives/legacy-2026-04/library-backend/` | 旧微服务骨架                      |
| `DevPlan/`                                                                      | `docs/archives/legacy-2026-04/DevPlan/`         | Phase 1/2 旧计划                  |
| `doc/`                                                                          | `docs/archives/legacy-2026-04/doc/`             | 与 `docs/` 重复                   |
| `PHASE6_PLAN.md` `PHASE6_PARTIAL_COMPLETION.md`                                 | archives                                        | Phase 6 已停                      |
| `STAGE15_PROGRESS_SUMMARY.md`                                                   | archives                                        | 由 `DEVELOPMENT_PROGRESS.md` 取代 |
| `PROJECT_STATUS_SUMMARY.md` `DOCUMENTATION_INDEX.md` `DOCUMENT_CLEANUP_PLAN.md` | archives                                        | 老状态文档                        |
| `QUICKSTART.md`                                                                 | archives                                        | 与 `docs/` 重复                   |
| `docs/development/IMPLEMENTATION_PLAN_STAGE15.md`                               | `docs/archives/`                                | Stage 15 已结束                   |

### 5.3 实施序列

```
Step 1  本主 spec 提交 git（唯一真相源）
   │
   ▼
Step 2  并行写 5 份顶层文档（PRD overview + arch overview + 4 ADR）
   │
   ▼
Step 3  并行写 4 份子 PRD（细化每平台功能、UI、用户流）
   │
   ▼
Step 4  并行写 6 份子架构文档（横切关注点）
   │
   ▼
Step 5  归档过时文档（B 类 git mv）
   │
   ▼
Step 6  写顶层 docs/README.md（导航索引）
   │
   ▼
Step 7  全部提交 git
```

**估计文档量**：~3500 行新文档（主 spec ~600 + 顶层 5 份 ~750 + PRD 4 份 ~1200 + 架构 6 份 ~1200）。

---

## Appendix A: ADR 摘要

### ADR-001: 多租户隔离 = 共享 DB + per-school schema

**Status**: Accepted (2026-04-30)
**Context**: 区域内 20-200 所学校；跨校检索 + 单校 99% 操作并存
**Decision**: PostgreSQL 单实例 + 每校 schema (`school_NNN`) + 区域公共 schema
**Consequences**:

- ✅ 跨校 OPAC 检索可走物化视图，性能可控
- ✅ 单校查询完全在自己 schema，逻辑隔离
- ✅ 备份/迁移可按校做（pg_dump --schema=）
- ⚠ 200 校 schema 数 200，PG 元数据压力可承受（社区案例 1000+ schema）
- ⚠ 学校强私有化诉求时需迁到独立 DB（保留为后续选项）

### ADR-002: 部署拓扑 = SaaS + 校内 Edge Agent

**Status**: Accepted (2026-04-30)
**Context**: 中国 K12 学校网络不稳定；借还书断网不能停业
**Decision**: 区域中心 K8s（SaaS）+ 学校 1 台 Mini PC 跑 Edge Agent
**Consequences**:

- ✅ 网络断仍可借还（本地 SQLite）
- ✅ 200 校仅需 200 台 Mini PC，不需要 200 套 K8s
- ✅ 数据 100% 在区域中心（合规集中管理）
- ⚠ Edge Agent 运维成本：每校配一名 IT 学习 docker-compose
- ⚠ 同步冲突需要严格处理（已设计幂等 + 先到先借）

### ADR-003: 微信前端 = 公众号 + 小程序（uni-app）

**Status**: Accepted (2026-04-30)
**Context**: 需求"集成校园公众号" + 学生需要现代化交互
**Decision**: uni-app 单源码 → 编译为公众号 H5 + 微信小程序
**Consequences**:

- ✅ 一套代码两个端，维护成本最低
- ✅ 公众号承担消息推送 + 入口；小程序承担深度交互
- ✅ 后续可再编译 App 端（uni-app 原生支持）
- ⚠ uni-app 部分 API 在公众号 H5 上有限制（蓝牙等仅小程序）

### ADR-004: Edge Agent 栈 = Java Spring Boot + SQLite + WSS

**Status**: Accepted (2026-04-30)
**Context**: 需要轻量、团队熟悉、企业网友好
**Decision**: Spring Boot 精简 + 内嵌 SQLite (AES-256) + WSS 双向 mTLS
**Consequences**:

- ✅ 复用 GCRF 团队 Java 技能栈
- ✅ SQLite 零运维（一个文件）
- ✅ WSS 走 443，学校防火墙开箱通过
- ⚠ Java 启动慢（Spring Native 后续优化）
- ⚠ 镜像体积 ~200M（GraalVM 后续优化）

---

## Appendix B: 风险与缓解

| 风险                        | 影响        | 缓解                                       |
| --------------------------- | ----------- | ------------------------------------------ |
| 200 校 schema PG 元数据压力 | 性能        | 监控 `pg_class` 大小；预留切到独立 DB 路径 |
| Edge Agent 维护负担         | 运维        | 提供一键部署脚本 + 远程监控通道            |
| OPAC 公开 → 爬虫            | 性能 + 法律 | 限流 + Cloudflare WAF + robots.txt         |
| 不适宜书库标准争议          | 法律 + 内容 | 区域可自建白名单 + 申诉流程                |
| 微信审核（小程序）          | 上线        | 单独申请教育类目 + 学校名义                |
| 一卡通 API 各异             | 集成        | 提供 adapter 层（每校适配器）              |

---

## Appendix C: 实施先后建议

**第一里程碑（M1，4-6 周）**：可演示给教育局

- 主 spec 完成
- 区域云后台（组织 + 用户管理）
- IAM SSO（账号密码 + 微信）
- OPAC（基础检索）

**第二里程碑（M2，6-8 周）**：可投产 1 所示范校

- 馆藏标准 + 不适宜书库
- 校园系统升级（智能采购推荐 + 智能馆藏分析）
- Edge Agent + 同步协议
- 微信公众号 H5（基本借阅查询）

**第三里程碑（M3，6-8 周）**：可铺开

- 微信小程序（深度功能）
- 数据中心 + 密钥嵌入
- 阅读测评 + 进馆预约
- 多校切换演练

总周期 ~6 个月，与现有 GCRF 团队 + 1-2 个新成员（uni-app + Edge Agent）即可。

---

**文档结束。**
请人工审阅后批准，进入子文档撰写阶段。
