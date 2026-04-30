# IAM 统一认证

**日期：** 2026-04-30
**状态：** Approved
**实现服务：** `auth-service` (8081) — 现有，本轮升级为多租户 OIDC Provider

---

## 1. 总览

```
                ┌─────────────────────────────────────┐
                │  auth-service (OIDC Provider)        │
                │  /oauth2/authorize  /token  /userinfo│
                └──────────────┬──────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
   ① 区域云后台         ② OPAC                 ③ 微信
   (web-admin region)   (opac SSR)              (uni-app)
                               │
        ┌──────────────────────▼──────────────────────┐
        │                                              │
        ▼                                              ▼
   ④ 校园后台                                    Edge Agent
   (web-admin campus)                            (mTLS + JWT)
```

四个平台 + Edge Agent 共享同一个 IAM，颁发的 token 跨平台有效。

## 2. 用户实体（gcrf_region.user）

```sql
CREATE TABLE gcrf_region.user (
  id              BIGSERIAL PRIMARY KEY,
  login           VARCHAR(50) UNIQUE NOT NULL,
  password_hash   VARCHAR(255),                       -- BCrypt
  identity_type   VARCHAR(30) NOT NULL,               -- REGION_ADMIN..PARENT
  region_id       BIGINT,
  school_id       BIGINT,                             -- 主属学校
  org_node_path   LTREE,                              -- 组织路径
  email           VARCHAR(100),
  phone           VARCHAR(20),
  status          VARCHAR(20) DEFAULT 'ACTIVE',
  created_at      TIMESTAMPTZ DEFAULT NOW(),
  ...
);
```

## 3. 登录方式（4 种）

### 3.1 账号密码

- 标准 OIDC `password` grant 不安全，改用 `authorization_code` + 登录页表单
- BCrypt 校验 + Failed-Login 锁定（5 次锁 5 分钟）

### 3.2 微信登录

#### 3.2.1 公众号 OAuth 静默授权

- 用户点击公众号菜单 → 微信跳 OAuth → 拿 openid → 后端换 token

#### 3.2.2 小程序 wx.login()

- 前端 `wx.login()` 拿 code → 后端 `code2Session` → 拿 openid + unionid

绑定流程：

```
首次微信登录 → 后端发现 openid 未绑定 → 引导输入读者证号 + 校验
  → 写入 user_wx_binding 表 → 发 token
后续微信登录 → 直接发 token
```

### 3.3 一卡通

- 学校配置一卡通 API（`/api/v1/integrations/card-system/config`）
- 学生输入一卡通号 + 密码 → `auth-service` 调一卡通 API 校验
- 校验通过 → 自动创建/匹配本地账号 → 发 token

### 3.4 钉钉

- 教师场景：钉钉应用扫码 / 自动登录
- 钉钉 OAuth → 拿 dingId → 与本地账号绑定

## 4. JWT 设计

### 4.1 Access Token（30 min）

```json
{
  "iss": "https://auth.gcrf.region",
  "sub": "12345",
  "exp": 1714502400,
  "iat": 1714500600,
  "tenant": "school_001",
  "school_id": 1,
  "region_id": 100,
  "identity_type": "STUDENT",
  "roles": ["STUDENT"],
  "data_scope": "SELF",
  "org_path": "100.001.s_xx.g_5.c_3"
}
```

签名：RS256（公钥发布在 `/.well-known/jwks.json`）

### 4.2 Refresh Token（30 days）

- 存储在 Redis（`refresh:<userId>`）
- 单点登出 = 删除 Redis 键 + access token 黑名单（Redis TTL 30 min）

### 4.3 微信小程序 Session（7 days）

- 小程序场景刷 token 频繁，单独使用 sessionKey + Redis 存储

## 5. 权限模型（RBAC + 数据范围）

### 5.1 数据库

```sql
-- gcrf_region 中
CREATE TABLE role (
  id           BIGSERIAL PRIMARY KEY,
  code         VARCHAR(50) UNIQUE,
  name         VARCHAR(100),
  is_builtin   BOOLEAN DEFAULT FALSE,
  is_custom    BOOLEAN DEFAULT FALSE,
  scope        VARCHAR(20) DEFAULT 'SCHOOL'  -- 角色作用范围
);

CREATE TABLE permission (
  id      BIGSERIAL PRIMARY KEY,
  code    VARCHAR(100) UNIQUE,    -- e.g. 'catalog:write'
  module  VARCHAR(50)
);

CREATE TABLE role_permission (
  role_id        BIGINT,
  permission_id  BIGINT,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_role (
  user_id    BIGINT,
  role_id    BIGINT,
  data_scope VARCHAR(20) DEFAULT 'SELF',  -- SELF/CLASS/GRADE/SCHOOL/REGION
  scope_node_path LTREE,                  -- 范围节点（如班级 path）
  PRIMARY KEY (user_id, role_id)
);
```

### 5.2 校验

```java
@PreAuthorize("hasPermission(#schoolId, 'catalog:write')")
public void updateCatalog(Long schoolId, ...) { ... }

// 实现：
class GcrfPermissionEvaluator {
  boolean hasPermission(Auth auth, Long schoolId, String permCode) {
    JwtClaims c = (JwtClaims) auth.getDetails();
    return rolePermissionRepo.exists(c.roles(), permCode)
        && tenantGuard.check(c, schoolId);
  }
}
```

## 6. 单点登录 / 登出

- 登录：4 平台跳 `auth.gcrf.region/login?return=...`
- 登出：`auth.gcrf.region/logout?back=...` → 清除 4 平台共享 cookie + Redis
- 微信小程序 / Edge Agent：独立 session，不参与浏览器 SSO

## 7. 安全

- HTTPS only（Strict-Transport-Security）
- Cookie: HttpOnly + Secure + SameSite=Strict
- CSRF: 双 token 模式（OPAC 公开页豁免）
- 敏感操作（密码改、解绑微信）：二次验证（短信 / 邮件 OTP）
- 暴破防护：rate limit + 验证码

## 8. 现有 GCRF auth-service 升级路径

| 当前                                | 升级后                              |
| ----------------------------------- | ----------------------------------- |
| 单租户 user 表                      | 多租户 user + tenant 字段           |
| `/api/v1/auth/login` 简单返回 token | OIDC `/oauth2/authorize` + `/token` |
| 无微信 / 一卡通 / 钉钉              | 4 种登录方式                        |
| 无 JWKS                             | 发布公钥到 `/.well-known/jwks.json` |

## 9. 关联文档

- 主 spec [§2.3 SSO 流](../specs/2026-04-30-regional-platform-master-design.md)
- [01-multi-tenant-isolation](01-multi-tenant-isolation.md)
- [05-data-model](05-data-model.md)
