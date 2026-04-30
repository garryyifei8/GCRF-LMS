# API 规范

**日期：** 2026-04-30
**状态：** Approved
**关联：** [04-iam-sso](04-iam-sso.md) / [05-data-model](05-data-model.md)

---

## 1. 通用约定

### 1.1 路径前缀

- 所有 API 走 `/api/v1/...`（保持与现有 GCRF 一致）
- 公开（OPAC）走 `/api/v1/opac/...`
- 微信走 `/api/v1/wx/...`
- 同步走 `/sync/...`（不带 v1，便于独立演进）

### 1.2 鉴权

- Bearer Token：`Authorization: Bearer <jwt>`
- 公开 OPAC 路由可匿名（限流 10 req/s/IP）
- Edge Agent 上行 WSS 加 mTLS + 短期 JWT

### 1.3 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1714498800000,
  "traceId": "abc123"
}
```

错误：

```json
{
  "code": 40301,
  "message": "cross-tenant access denied",
  "errors": [{ "field": "schoolId", "msg": "..." }],
  "timestamp": ...,
  "traceId": "..."
}
```

### 1.4 错误码段

| 段       | 含义               |
| -------- | ------------------ |
| 200, 201 | 成功               |
| 400xx    | 参数错误           |
| 401xx    | 未认证             |
| 403xx    | 无权限             |
| 404xx    | 资源不存在         |
| 409xx    | 冲突（借书冲突等） |
| 429      | 限流               |
| 500xx    | 服务端错误         |

### 1.5 分页

```
GET /resource?pageNum=1&pageSize=20&sortBy=createdAt&order=DESC
```

```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 1234,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 62
  }
}
```

### 1.6 命名约定

- URL：kebab-case（`/borrow-records`）
- JSON 字段：camelCase（`readerId`）
- 数据库：snake_case（`reader_id`）
- 时间：ISO 8601 UTC（`2026-04-30T10:30:00Z`）

## 2. 平台 API 列表（速查）

> 详细字段见各服务 OpenAPI / Swagger（启动后访问 `/swagger-ui.html`）

### 2.1 IAM (`auth-service`)

| Method | Path                            | 说明         |
| ------ | ------------------------------- | ------------ |
| GET    | `/api/v1/.well-known/jwks.json` | JWKS 公钥    |
| GET    | `/oauth2/authorize`             | OIDC 授权    |
| POST   | `/oauth2/token`                 | 换 token     |
| POST   | `/api/v1/auth/login`            | 账号密码登录 |
| POST   | `/api/v1/auth/wx-login`         | 微信登录     |
| POST   | `/api/v1/auth/card-login`       | 一卡通登录   |
| POST   | `/api/v1/auth/refresh`          | 刷新 token   |
| POST   | `/api/v1/auth/logout`           | 登出         |

### 2.2 组织 (`org-service`)

| Method | Path                                               |
| ------ | -------------------------------------------------- |
| GET    | `/api/v1/org/nodes?parentId=&depth=`               |
| POST   | `/api/v1/org/nodes`                                |
| PUT    | `/api/v1/org/nodes/{id}`                           |
| DELETE | `/api/v1/org/nodes/{id}`                           |
| POST   | `/api/v1/org/nodes/{id}/move`                      |
| POST   | `/api/v1/org/nodes/import`                         |
| POST   | `/api/v1/org/schools` （新建学校 → 自动建 schema） |

### 2.3 馆藏标准 (`standard-service`)

| Method | Path                                          |
| ------ | --------------------------------------------- |
| GET    | `/api/v1/standards`                           |
| PUT    | `/api/v1/standards/{key}?schoolType=`         |
| POST   | `/api/v1/standards/check?node=`               |
| GET    | `/api/v1/reports/qualification?node=&format=` |
| GET    | `/api/v1/inappropriate-books`                 |
| POST   | `/api/v1/inappropriate-books`                 |
| POST   | `/api/v1/inappropriate-books/scan/{schoolId}` |
| POST   | `/api/v1/inappropriate-books/auto-takedown`   |

### 2.4 编目 (`book-service`)

| Method | Path                                   |
| ------ | -------------------------------------- |
| GET    | `/api/v1/catalog?keyword=&clc=`        |
| GET    | `/api/v1/catalog/{id}`                 |
| POST   | `/api/v1/catalog`                      |
| PUT    | `/api/v1/catalog/{id}`                 |
| POST   | `/api/v1/catalog/marc/import`          |
| GET    | `/api/v1/catalog/marc/by-isbn/{isbn}`  |
| POST   | `/api/v1/catalog/copies/batch`         |
| POST   | `/api/v1/catalog/copies/transfer`      |
| POST   | `/api/v1/print/labels`                 |
| GET    | `/api/v1/catalog/region-library?isbn=` |

### 2.5 流通 (`circulation-service`)

| Method | Path                           |
| ------ | ------------------------------ |
| POST   | `/api/v1/borrow`               |
| POST   | `/api/v1/return`               |
| POST   | `/api/v1/renew/{borrowId}`     |
| POST   | `/api/v1/reservations`         |
| GET    | `/api/v1/reservations?status=` |
| GET    | `/api/v1/borrows?readerId=`    |

### 2.6 读者 (`reader-service`)

| Method | Path                                    |
| ------ | --------------------------------------- |
| GET    | `/api/v1/readers`                       |
| POST   | `/api/v1/readers`                       |
| PUT    | `/api/v1/readers/{id}`                  |
| DELETE | `/api/v1/readers/{id}`                  |
| POST   | `/api/v1/readers/batch-cancel-by-grade` |
| GET    | `/api/v1/readers/{id}/borrow-history`   |

### 2.7 OPAC (`opac-service`)

| Method | Path                                         | 限流             |
| ------ | -------------------------------------------- | ---------------- |
| GET    | `/api/v1/opac/search?q=&clc=&adv=`           | 10/s/IP          |
| GET    | `/api/v1/opac/suggest?q=`                    | 30/s/IP          |
| GET    | `/api/v1/opac/books/{isbn}`                  | 10/s/IP          |
| GET    | `/api/v1/opac/clc/tree`                      | 1/min/IP（缓存） |
| GET    | `/api/v1/opac/clc/{code}/books`              | 10/s/IP          |
| GET    | `/api/v1/opac/rankings/{type}`               | 30/min/IP        |
| GET    | `/api/v1/opac/recommend/related?isbn=`       | 10/s/IP          |
| GET    | `/api/v1/opac/new-arrivals`                  | 30/min/IP        |
| GET    | `/api/v1/opac/availability/{isbn}/by-school` | 10/s/IP          |

### 2.8 微信 (`wechat-service`)

| Method | Path                                      |
| ------ | ----------------------------------------- |
| POST   | `/api/v1/wx/oauth/login`                  |
| POST   | `/api/v1/wx/bind/cardNumber`              |
| POST   | `/api/v1/wx/bind/parent-child`            |
| GET    | `/api/v1/wx/search`                       |
| GET    | `/api/v1/wx/my/borrows`                   |
| POST   | `/api/v1/wx/renew/{id}`                   |
| GET    | `/api/v1/wx/my/reservations`              |
| POST   | `/api/v1/wx/reservations`                 |
| GET    | `/api/v1/wx/my/reading-report`            |
| GET    | `/api/v1/wx/children/{id}/reading-report` |
| GET    | `/api/v1/wx/booklists`                    |
| GET    | `/api/v1/wx/reading-tests`                |
| POST   | `/api/v1/wx/reading-tests/{id}/submit`    |
| GET    | `/api/v1/wx/visit-slots`                  |
| POST   | `/api/v1/wx/visit-reservation`            |
| POST   | `/api/v1/wx/subscribe-message/send`       |

### 2.9 智能 (`recommend-service`)

| Method | Path                                                |
| ------ | --------------------------------------------------- |
| GET    | `/api/v1/intelligence/collection-analysis`          |
| POST   | `/api/v1/intelligence/collection-analysis/generate` |
| GET    | `/api/v1/intelligence/purchase-suggestions?budget=` |

### 2.10 同步 (`sync-service`)

| Method | Path                               | 协议                     |
| ------ | ---------------------------------- | ------------------------ |
| GET    | `/sync/snapshot/{schoolId}?since=` | HTTP（Edge 启动 / 重连） |
| POST   | `/sync/queue/{schoolId}`           | HTTP（积压批量上报）     |
| GET    | `/sync/version`                    | HTTP（OTA 版本）         |
| GET    | `/edge/{schoolId}`                 | WSS（双向消息）          |

### 2.11 系统 (`system-service`, 已有)

| Method | Path                              |
| ------ | --------------------------------- |
| GET    | `/api/v1/system/config`           |
| PUT    | `/api/v1/system/config`           |
| GET    | `/api/v1/system/messages?userId=` |
| POST   | `/api/v1/system/feedback`         |
| POST   | `/api/v1/system/backup`           |
| GET    | `/api/v1/system/backup`           |

### 2.12 分析 (`analytics-service`, 已有 + 扩展)

| Method | Path                                              |
| ------ | ------------------------------------------------- |
| GET    | `/api/v1/analytics/region/overview?node=`         |
| GET    | `/api/v1/analytics/region/by-school?metric=`      |
| GET    | `/api/v1/analytics/school/category-stats`         |
| GET    | `/api/v1/analytics/school/circulation`            |
| GET    | `/api/v1/analytics/school/reading-quality?level=` |
| GET    | `/api/v1/analytics/school/student-stats`          |
| GET    | `/api/v1/analytics/export/comprehensive-report`   |

## 3. 限流策略

| 类型       | 限制                               |
| ---------- | ---------------------------------- |
| OPAC 公开  | 见上表                             |
| 已登录读者 | 60 req/s/user                      |
| 管理员     | 200 req/s/user                     |
| 全局       | 10000 req/s（按 Gateway 整体阈值） |

## 4. 版本演进

- API 大版本：`/api/v1/` → `/api/v2/`（不向后兼容时）
- 字段新增不增版本（向前兼容）
- Deprecation：响应头 `Deprecation: true` + `Sunset: <date>`

## 5. OpenAPI 自动生成

- 每服务启用 SpringDoc → 启动后 `/swagger-ui.html`
- 聚合：API Gateway 提供 `/swagger-ui.html?service=<name>` 入口

## 6. 关联文档

- [04-iam-sso](04-iam-sso.md)
- [05-data-model](05-data-model.md)
- 现有 API：[`../api/`](../api/)
