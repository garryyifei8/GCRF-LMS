# Plan-B1: 核心 IAM + RBAC 设计文档

**日期：** 2026-05-09
**状态：** Approved（待 plan 编写）
**主 spec：** [`2026-04-30-regional-platform-master-design.md`](./2026-04-30-regional-platform-master-design.md) §2.2 / §2.3
**相关 PRD：** [`../prd/01-regional-cloud-platform.md`](../prd/01-regional-cloud-platform.md) §2.2
**前置：** Plan-A 已交付 v1.1.0-plan-A（多租户基础设施 + org-service）

---

## 背景与目标

主 spec Appendix C 把 **IAM SSO（账号密码 + 微信）** 列为 M1（教育局演示）三大交付物之一，前两项（区域云后台 / OPAC 基础检索）已分别由 Plan-A、Plan-C1+C1.5 完成。微信端因 wechat-service 主体在 M2 才开建，且需要双模式公众号（区域级 + 单校级）配置 UI，一并延后到 Plan-B2 / M2 完成。

Plan-B1 聚焦"核心 IAM + RBAC"，把 GCRF 从单校单服务级 auth-service（用户名密码 + 简单 JWT）演进为支撑 4 平台、200+ 学校的统一身份中心：

- JWT 富化：`tenant / roles[] / scope` claim
- RBAC：10 系统角色 seed + 11 模块权限 + 用户角色分配
- 用户身份从 `auth_service` DB 中心化到 `gcrf_region.users`
- 标准 refresh token 30 天 + 旋转防重放
- 副产品：`system-service → auth-service` 那条 Feign 链路（昨日修复的 4 层 bug 来源）整条拆掉，改为本地 mapper

显式不做（避免范围扩张）：

- 微信公众号 OAuth + 小程序 wx.login（→ Plan-B2）
- `parent_student_link` 家长子女关联（→ Plan-B2）
- 一卡通对接 / 钉钉 SSO（按需上 OIDC 适配层，无明确客户）
- 自定义角色 CRUD（功能模块 × 数据范围矩阵 UI）（→ M2 校园系统升级）
- 通用 `@DataScope` AOP 拦截器 / MyBatis-Plus DataPermissionHandler（→ M2）
- CLASS / GRADE 级动态过滤（→ M2）

---

## §1 架构与组件边界

**演进策略：在现有 `auth-service` 上原地升级**，不新建独立 iam-service。理由：auth-service 现仅 2 个 controller、1 张表、1 个 mapper，体量小；新建独立服务会引入跨服务调用且无业务隔离收益。

```
┌────────────────────────────────────────────────────────────┐
│ web-admin (Vue 3)                                            │
│  ├── 现有：用户管理（system-service 代理 auth-service）       │
│  └── 新增：                                                  │
│       /system/roles          角色管理（10 系统角色，只读）    │
│       /system/users/:id      用户详情加 "角色" tab            │
└─────────────────────┬──────────────────────────────────────┘
                      │ JWT (HS512, 512-bit key)
                      ▼
┌────────────────────────────────────────────────────────────┐
│ auth-service (8081) — 升级为 IAM                             │
│                                                              │
│  /api/v1/auth/login          ← 富化响应                      │
│  /api/v1/auth/refresh        ← 标准 refresh token 协议       │
│  /api/v1/auth/logout         ← 吊销 refresh                  │
│  /api/v1/auth/me             ← 当前用户上下文                 │
│  /api/v1/auth/validate       ← 保留（向后兼容）               │
│  /api/v1/users (CRUD)        ← 数据源迁到 gcrf_region.users  │
│  /api/v1/users/{id}/roles    ← GET/POST/DELETE 角色分配      │
│  /api/v1/roles               ← GET 系统角色（M1 只读）        │
│  /api/v1/roles/{id}          ← GET 角色 + 权限明细            │
│  /api/v1/permissions         ← GET 11 个模块权限              │
└─────────────────────┬──────────────────────────────────────┘
                      │ datasource → gcrf_region
                      ▼
┌────────────────────────────────────────────────────────────┐
│ PostgreSQL  gcrf_region schema                              │
│                                                              │
│  users                  ← 自 auth_service.users 迁来 + 扩展  │
│  auth_role              ← 10 系统角色 seed                   │
│  auth_permission        ← 11 模块权限 seed                   │
│  auth_role_permission   ← 默认映射 seed                      │
│  auth_user_role         ← 用户角色分配（admin → REGION_ADMIN）│
└────────────────────────────────────────────────────────────┘
                      ▲
                      │ 业务服务不再调 Feign 拿 user
                      │ JWT 校验 + 必要时本地查 gcrf_region
┌─────────────────────┴──────────────────────────────────────┐
│ system / circulation / opac / org / analytics / ...          │
│                                                              │
│  common-security:                                            │
│    SecurityContextFilter       — 解析 JWT 写 ThreadLocal     │
│    SecurityContextHolder       — currentTenant/Roles/...     │
│    @RequireRole / @RequirePermission / @RequireScope         │
│  common-mybatis (Plan-A 已有):                               │
│    TenantContextFilter / SearchPathInterceptor               │
│    （配合 JWT.tenant claim 自动切 search_path）                │
└────────────────────────────────────────────────────────────┘
```

**关键边界**：

- `auth-service` 是 IAM 唯一颁发者，所有 access/refresh token 由它签发
- RBAC 表全部在 `gcrf_region`，跨服务可读
- 业务服务不调 auth-service 的 user 接口，只验 JWT 签名 + SecurityContextFilter
- `TenantContextFilter`（Plan-A）保留，**JWT.tenant claim 落地后它真正起作用**
- 副产品：`system-service.UserController` 拆掉对 `UserManagementClient` 的 Feign 依赖，改为本地查 gcrf_region.users（彻底消除昨日 4 层 bug 链路）

---

## §2 数据模型

### 2.1 `gcrf_region.users`（迁移自 auth_service.users + 扩展）

```sql
CREATE TABLE IF NOT EXISTS gcrf_region.users (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(50) NOT NULL UNIQUE,         -- 登录名（来源字段保留）
    username           VARCHAR(100) NOT NULL,               -- 显示名
    password           VARCHAR(255) NOT NULL,               -- BCrypt
    email              VARCHAR(100),
    phone              VARCHAR(20),
    user_type          VARCHAR(20) NOT NULL DEFAULT 'STUDENT',  -- 粗分类
    avatar_url         VARCHAR(500),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time    TIMESTAMPTZ,
    last_login_ip      VARCHAR(50),
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until       TIMESTAMPTZ,

    -- 新增：组织 + 多租户
    org_node_id        BIGINT REFERENCES gcrf_region.org_node(id),
    school_id          BIGINT REFERENCES gcrf_region.school(id),       -- denormalized
    tenant_schema      VARCHAR(64),                                     -- JWT.tenant 来源；区域级用户为 NULL

    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_alive
    ON gcrf_region.users (email) WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_org_node    ON gcrf_region.users (org_node_id);
CREATE INDEX IF NOT EXISTS idx_users_school      ON gcrf_region.users (school_id);
CREATE INDEX IF NOT EXISTS idx_users_status      ON gcrf_region.users (status);
CREATE INDEX IF NOT EXISTS idx_users_user_type   ON gcrf_region.users (user_type);
```

### 2.2 `auth_role`

```sql
CREATE TABLE IF NOT EXISTS gcrf_region.auth_role (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    scope_default   VARCHAR(20) NOT NULL,           -- REGION/SCHOOL/CLASS/GRADE/SELF
    is_system       BOOLEAN NOT NULL DEFAULT false, -- 系统角色不可改不可删
    school_id       BIGINT REFERENCES gcrf_region.school(id),  -- 自定义角色归属（M1 全部 NULL）
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_school ON gcrf_region.auth_role (school_id);
```

**Seed 10 系统角色**（is_system=true）：

| code                | name       | scope_default |
| ------------------- | ---------- | ------------- |
| REGION_ADMIN        | 区域超管   | REGION        |
| REGION_LIBRARIAN    | 区域馆员   | REGION        |
| SCHOOL_ADMIN        | 学校管理员 | SCHOOL        |
| SCHOOL_LIBRARY_HEAD | 学校馆长   | SCHOOL        |
| LIBRARIAN           | 学校馆员   | SCHOOL        |
| OPERATOR            | 操作员     | SCHOOL        |
| TEACHER             | 教师       | CLASS         |
| STUDENT             | 学生       | SELF          |
| PARENT              | 家长       | SELF          |
| GUEST               | 游客       | SELF          |

### 2.3 `auth_permission`

```sql
CREATE TABLE IF NOT EXISTS gcrf_region.auth_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,    -- 'book.read', 'circulation.write'
    module      VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,            -- read / write
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_perm_module ON gcrf_region.auth_permission (module);
```

**Seed 11 模块权限**（粗粒度，M1 够用）：

```
book.read / book.write
circulation.read / circulation.write
reader.read / reader.write
system.read / system.write
analytics.read
org.read / org.write
opac.read
```

### 2.4 `auth_role_permission`

```sql
CREATE TABLE IF NOT EXISTS gcrf_region.auth_role_permission (
    role_id       BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES gcrf_region.auth_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);
```

**Seed 默认映射**：

| Role                | 拥有权限                                                                |
| ------------------- | ----------------------------------------------------------------------- |
| REGION_ADMIN        | 全部 11 个                                                              |
| REGION_LIBRARIAN    | book.read, analytics.read, org.read, opac.read                          |
| SCHOOL_ADMIN        | book.\*, circulation.\*, reader.\*, system.\*, analytics.read, org.read |
| SCHOOL_LIBRARY_HEAD | book.\*, circulation.\*, reader.\*, analytics.read                      |
| LIBRARIAN           | book.\*, circulation.\*, reader.\*                                      |
| OPERATOR            | circulation.write                                                       |
| TEACHER             | book.read, opac.read, analytics.read                                    |
| STUDENT             | opac.read, book.read                                                    |
| PARENT              | opac.read, book.read                                                    |
| GUEST               | opac.read                                                               |

### 2.5 `auth_user_role`

```sql
CREATE TABLE IF NOT EXISTS gcrf_region.auth_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES gcrf_region.users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    school_id       BIGINT REFERENCES gcrf_region.school(id),  -- NULL = 区域级
    scope_override  VARCHAR(20),                                -- 可覆盖 role.scope_default（M1 留字段不用）
    scope_path      VARCHAR(500),                               -- 细粒度子树（M1 留字段不用）
    assigned_by     BIGINT REFERENCES gcrf_region.users(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,                                -- NULL = 永久
    UNIQUE NULLS NOT DISTINCT (user_id, role_id, school_id)    -- PG 15+
);
CREATE INDEX IF NOT EXISTS idx_user_role_user   ON gcrf_region.auth_user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_school ON gcrf_region.auth_user_role (school_id);
```

### 2.6 数据迁移策略

生产 `auth_service.users` 现 ~10 条数据，量极小：

1. Flyway V003 在 gcrf_region 建表 + seed
2. 一次性手工 SQL（在维护窗口执行）：

   ```sql
   -- 启用 dblink（postgres-contrib，已可用）
   CREATE EXTENSION IF NOT EXISTS dblink;
   INSERT INTO gcrf_region.users (
     id, user_id, username, password, email, phone, user_type,
     avatar_url, status, last_login_time, last_login_ip,
     failed_login_count, locked_until, created_at, updated_at, deleted_at
   )
   SELECT id, user_id, username, password, email, phone, user_type,
          avatar_url, status, last_login_time::timestamptz, last_login_ip,
          COALESCE(failed_login_count,0), locked_until::timestamptz,
          created_at::timestamptz, updated_at::timestamptz, deleted_at::timestamptz
   FROM dblink('host=postgresql port=5432 dbname=auth_service user=postgres password=...',
               'SELECT id, user_id, username, password, email, phone, user_type,
                       avatar_url, status, last_login_time, last_login_ip,
                       failed_login_count, locked_until, created_at, updated_at, deleted_at
                FROM users')
   AS old(id BIGINT, user_id VARCHAR, username VARCHAR, password VARCHAR,
          email VARCHAR, phone VARCHAR, user_type VARCHAR, avatar_url VARCHAR,
          status VARCHAR, last_login_time TIMESTAMP, last_login_ip VARCHAR,
          failed_login_count INT, locked_until TIMESTAMP,
          created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP)
   ON CONFLICT (user_id) DO NOTHING;

   -- 重置 sequence 到 max(id)+1
   SELECT setval(pg_get_serial_sequence('gcrf_region.users','id'),
                 (SELECT COALESCE(MAX(id),0)+1 FROM gcrf_region.users), false);
   ```

3. row count 比对一致后，给 `admin` 自动绑 REGION_ADMIN 角色：
   ```sql
   INSERT INTO gcrf_region.auth_user_role (user_id, role_id, school_id, assigned_by)
   SELECT u.id, r.id, NULL, u.id
     FROM gcrf_region.users u, gcrf_region.auth_role r
    WHERE u.user_id = 'admin' AND r.code = 'REGION_ADMIN'
   ON CONFLICT DO NOTHING;
   ```
4. auth-service 新版 image 部署后，`/auth/login admin/admin123` 验证通过 → 1 周观察期
5. 观察无问题后 `DROP DATABASE auth_service;`

---

## §3 API 表面

### 3.1 IAM 端点（auth-service 8081）

| Endpoint                            | Method                    | 说明                                                          | 调用方                                      |
| ----------------------------------- | ------------------------- | ------------------------------------------------------------- | ------------------------------------------- |
| `/api/v1/auth/login`                | POST                      | 用户名密码登录，富化响应                                      | 各平台前端                                  |
| `/api/v1/auth/refresh`              | POST                      | refresh token 换新 access；旋转防重放                         | 各平台前端                                  |
| `/api/v1/auth/logout`               | POST                      | 吊销 refresh                                                  | 各平台前端                                  |
| `/api/v1/auth/me`                   | GET                       | 当前用户上下文                                                | 各平台前端（每页面）                        |
| `/api/v1/auth/validate`             | GET                       | 仅校验 token（向后兼容保留）                                  | 现有调用方                                  |
| `/api/v1/users`                     | GET / POST / PUT / DELETE | 用户 CRUD（数据源迁到 gcrf_region.users）                     | system-service Feign（M1 末拆掉）+ 区域后台 |
| `/api/v1/users/{id}/reset-password` | POST                      | 重置密码                                                      | 区域后台                                    |
| `/api/v1/users/{id}/status`         | PUT                       | 启停                                                          | 区域后台                                    |
| `/api/v1/users/{id}/roles`          | GET                       | 当前角色列表                                                  | 区域后台用户管理                            |
| `/api/v1/users/{id}/roles`          | POST                      | 分配角色 `{roleId, schoolId?, expiresAt?}`（仅 REGION_ADMIN） | 区域后台                                    |
| `/api/v1/users/{id}/roles/{roleId}` | DELETE                    | 撤销                                                          | 区域后台                                    |
| `/api/v1/roles`                     | GET                       | 系统角色列表（M1 只读）                                       | 区域后台                                    |
| `/api/v1/roles/{id}`                | GET                       | 角色详情 + 权限                                               | 区域后台                                    |
| `/api/v1/permissions`               | GET                       | 11 个模块权限（按 module 分组）                               | 区域后台                                    |

### 3.2 业务服务侧

不新增 endpoint，仅新增 SecurityContextFilter 行为：

```java
// common-security 暴露
@RequireRole({"REGION_ADMIN"})
@RequirePermission("book.write")
@RequireScope(SCOPE.REGION)

SecurityContextHolder.currentUserId()
SecurityContextHolder.currentTenant()           // school_000001 / null
SecurityContextHolder.currentRoles()            // ["LIBRARIAN", "TEACHER"]
SecurityContextHolder.currentScope()            // SCHOOL / REGION / SELF
SecurityContextHolder.currentOrgPath()          // ltree '/100/200/305/'
SecurityContextHolder.hasRole("LIBRARIAN")
SecurityContextHolder.hasPermission("book.write")
SecurityContextHolder.hasScope(SCOPE.REGION)
```

### 3.3 前端新增

| 路由                              | 页面     | 内容                                                                                                                                       |
| --------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `/system/roles`                   | 角色管理 | 表格列出 10 系统角色：code / name / scope_default / 权限数 / 用户数；点击行展开"权限"tab（只读勾选矩阵） + "用户"tab（带该角色的用户列表） |
| `/system/users/:id`（现有页扩展） | 用户详情 | 加 "角色" tab：当前分配角色列表 + "+ 添加角色"按钮（弹窗：选 role + 选 school + 可选 expires）                                             |

### 3.4 故意 punt

- ❌ `POST /api/v1/roles`（自定义角色创建）→ M2
- ❌ `PUT /api/v1/roles/{id}`（自定义角色编辑）→ M2
- ❌ `/oauth2/*` → 按需上 OIDC 适配
- ❌ `parent_student_link` / 微信绑定 → Plan-B2

---

## §4 JWT 与认证流

### 4.1 Access Token

**算法**：HS512 + 512-bit 共享密钥（修复当前 400-bit key 告警）。
**TTL**：30 min。
**Claims**：

```json
{
  "sub": "1",
  "user_id": 1,
  "username": "admin",
  "tenant": "school_000001",
  "tenant_id": 1,
  "roles": ["LIBRARIAN", "TEACHER"],
  "scope": "SCHOOL",
  "iat": 1234567890,
  "exp": 1234569690,
  "jti": "uuid",
  "iss": "gcrf-iam",
  "aud": ["gcrf-platform"]
}
```

`tenant` / `tenant_id` 对区域级用户为 `null`。`scope` 取角色 scope_default 中范围最大者（REGION > SCHOOL > GRADE > CLASS > SELF）。

### 4.2 Refresh Token

**形式**：UUIDv4 不透明 token。
**TTL**：30 d。
**存储**：Redis `refresh:{token}` → `{user_id, exp}`。
**旋转**：每次 refresh 删旧发新，防重放。
**吊销**：登出/角色变更时主动 DEL。

### 4.3 Permissions 不进 JWT

**原因**：

1. token 大小膨胀（11+ 权限 × 多角色累加可超 1KB）
2. 角色变更后旧 token 持有过期权限（30 min 内失效仍是问题）

**做法**：业务服务在 SecurityContextFilter 里 lazy 查，Redis 缓存：

```
key:  perms:{user_id}
val:  ["book.read", "book.write", ...]
TTL:  5 min
evict: 角色分配 / 撤销时主动 DEL
```

### 4.4 三条核心流

```
登录 POST /auth/login {username, password}
  → BCrypt verify
  → load user + roles from gcrf_region
  → max(scope_default)
  → 签发 access(30min HS512) + refresh(30d Redis)
  → 返回 {accessToken, refreshToken, expiresIn, user, roles, tenant, scope, permissions}

刷新 POST /auth/refresh {refreshToken}
  → Redis GET refresh:{token} → user_id；不存在 = 401
  → 重 load user + roles（防止旧 token 持有已撤销角色）
  → 签发新 access + 新 refresh
  → DEL 旧 refresh

登出 POST /auth/logout {refreshToken}
  → DEL refresh:{token}
  → access 不黑名单（30 min 自然过期；M1 简化，M2 wechat 上线时再加 jti 黑名单）
```

### 4.5 SecurityContextFilter（common-security）

```java
1. 读 Authorization: Bearer <token>
2. HS512 verify（共享密钥从 env 读）
3. exp 验证
4. 解出 user_id / tenant / roles[] / scope → 写 ThreadLocal SecurityContext
5. hasPermission(code) 命中时查 Redis perms:{user_id}，miss 则 SELECT JOIN 三表
6. 与 Plan-A 的 TenantContextFilter 协作：JWT.tenant 写入 TenantContext，
   SearchPathInterceptor 据此 SET search_path
```

---

## §5 数据范围（Data Scope）

| 范围              | 实现机制                                                                                                                              | M1 代码改动   |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------- | ------------- | ---- | ------------------------- |
| **SCHOOL**        | 复用 Plan-A `TenantContextFilter` + `SearchPathInterceptor`；JWT.tenant=school_NNN → search_path 自动切学校 schema → 跨校数据天然隔离 | 0（坐享其成） |
| **REGION**        | JWT.tenant=null → search_path 留默认 → 走 gcrf_region 共享表；管辖子树由 mapper 显式加 `org_path LIKE :currentOrgPath                 |               | '%'` | 4-5 个区域 mapper 加 LIKE |
| **SELF**          | 业务代码已是 `WHERE user_id = currentUser()`（reader / borrow），不重构                                                               | 0             |
| **CLASS / GRADE** | M1 punt — 教师班级阅读报表等接口主要落在 Plan-B2/M2 微信端                                                                            | 0（明确推迟） |

**新增工具（轻量）**：

```java
SecurityContextHolder.currentOrgPath()   // ltree '/100/200/305/'
SecurityContextHolder.currentScope()     // SCOPE 枚举
SecurityContextHolder.hasScope(SCOPE.REGION)

@RequireScope(SCOPE.REGION)   // 防御性硬 deny；不做查询重写
```

**故意 punt**（M2 自定义角色矩阵上线时一并做）：

- ❌ 通用 `@DataScope` AOP 拦截器
- ❌ MyBatis-Plus `DataPermissionHandler` 集成
- ❌ CLASS / GRADE 级动态过滤
- ❌ "数据范围 × 功能模块"矩阵 UI

---

## §6 迁移路径与任务估算

### 6.1 生产升级序列

每个 step 单独可回滚，两两之间生产 100% 工作：

```
Step 0  common-security: SecurityContextFilter + JwtUtil 升 512-bit key（本地构建，先打底）

Step 1  Flyway V003：gcrf_region 建 users + 4 张 RBAC 表 + 11 + 10 + 默认映射 seed

Step 2  数据迁移：dblink 一次性 INSERT；row count 比对；admin 绑 REGION_ADMIN

Step 3  auth-service 切数据源到 gcrf_region；灰度 1 pod 验证登录 → 切流量
        ⚠️ 风险点：登录走错库 → 全平台登不上；必须灰度 1 pod 先验证

Step 4  auth-service 加 IAM 端点（/auth/refresh, /auth/me, /users/{id}/roles 等）
        + JWT 富化（roles/tenant/scope claim）
        前端忽略未知字段无副作用

Step 5  各业务服务（system / circulation / opac / org / analytics）升级 common-security
        加 SecurityContextFilter；老 JWT 没新 claim 时 fallback 到老语义

Step 6  web-admin 加角色管理页 + 用户角色分配抽屉

Step 7  拆 system-service → auth-service Feign 链路：
        UserController 改本地 mapper 查 gcrf_region.users，删 UserManagementClient
        昨日 4 层 bug 链路彻底消失

Step 8  下线 auth_service DB（Step 3 后 1 周观察期无问题）
```

### 6.2 任务估算

| 模块                                                                                    | 估计 task    |
| --------------------------------------------------------------------------------------- | ------------ |
| common-security: SecurityContextFilter + 注解 + Helper + 测试                           | 3            |
| auth-service: 数据源迁移 + JWT 富化 + IAM 端点 + 角色 CRUD（只读）                      | 5            |
| Flyway V003 + 数据迁移 SQL + RBAC seed                                                  | 2            |
| 业务服务统一接入 SecurityContextFilter（system / circulation / opac / org / analytics） | 1            |
| 拆 system → auth Feign 链路                                                             | 1            |
| web-admin 角色管理页 + 用户角色抽屉                                                     | 2            |
| 部署（image 重建 + 三节点 ctr import + 灰度切流）+ E2E + tag                            | 2            |
| **合计**                                                                                | **~16 task** |

预计 1.5-2 周交付。

### 6.3 风险与缓解

| 风险                                                         | 缓解                                                                                   |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| 数据源切换导致登录全平台瘫痪                                 | Step 3 灰度 1 pod；保留旧 image 可回滚；老 auth_service DB 1 周观察期                  |
| JWT 字段变更打破前端                                         | 老字段（accessToken/expiresIn）保留，新字段 superset；前端老代码 ignore 新字段无副作用 |
| `users` 表跨库迁移漏数据                                     | 迁移前后 row count 比对；老库保留 1 周对照                                             |
| RBAC seed 给 admin 配错权限 → 自己锁外面                     | seed 自动给 `admin` 绑 REGION_ADMIN；首发后立即用 `/auth/me` 验证                      |
| Plan-A `TenantContextFilter` 旧 JWT 无 tenant claim 解析失败 | filter 已有 try/catch 兜底（Plan-A 既有行为）                                          |

---

## 关联 ADR

本 plan 不新建 ADR。决策来源：

- 主 spec §2.2 / §2.3：JWT claim 形式、10 系统角色、5 数据范围
- 主 spec §1.1 / Appendix A ADR-001：多租户 = 共享 DB + per-school schema
- Plan-A 已落地：TenantContextFilter + SearchPathInterceptor 模式

---

## 验收标准

- [ ] `/auth/login admin/admin123` 返回 access(30min) + refresh(30d) + roles=["REGION_ADMIN"] + scope="REGION"
- [ ] `/auth/refresh` 旋转 token；旧 refresh 删除；新 refresh 可继续用
- [ ] `/auth/me` 返回 user + roles + tenant + permissions
- [ ] `/users/{id}/roles POST` 仅 REGION_ADMIN 可调用，其他角色 403
- [ ] system-service 调 `/api/v1/system/users` 不再走 Feign（链路 trace 不出现 `auth-service`）
- [ ] auth-service 部署后所有平台登录正常 ≥ 24 h；test-online.sh 通过率不低于现有 96%
- [ ] gcrf_region.users row count = auth_service.users row count（迁移完整性）
- [ ] tag `v1.3.0-plan-B1` 推送

---

**Last Updated**: 2026-05-09
