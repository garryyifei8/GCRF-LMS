# GCRF Library Management System - OpenAPI 3.0 Specifications

## 📋 概述

本目录包含GCRF图书馆管理系统所有微服务的OpenAPI 3.0规范文件。

## 📁 目录结构

```
05_API_SPECIFICATIONS/
├── README.md                 # 本文件
├── common/                   # 通用定义
│   ├── schemas.yaml         # 通用数据模型 (Result<T>, PageResult<T>)
│   ├── security.yaml        # 认证授权定义
│   ├── errors.yaml          # 错误响应定义
│   └── parameters.yaml      # 通用参数定义
├── gateway-api.yaml         # Gateway Service API
├── auth-api.yaml            # Auth Service API
├── book-api.yaml            # Book Service API
├── circulation-api.yaml     # Circulation Service API
├── reader-api.yaml          # Reader Service API
├── system-api.yaml          # System Service API
└── notification-api.yaml    # Notification Service API
```

## 🚀 快速开始

### 在线查看

使用Swagger Editor在线查看和编辑API规范:
https://editor.swagger.io/

### 本地查看

1. 启动对应的微服务
2. 访问Swagger UI: `http://localhost:{port}/swagger-ui.html`
   - Gateway: http://localhost:8080/swagger-ui.html
   - Auth: http://localhost:8081/swagger-ui.html
   - Book: http://localhost:8082/swagger-ui.html
   - Circulation: http://localhost:8083/swagger-ui.html
   - Reader: http://localhost:8084/swagger-ui.html
   - System: http://localhost:8085/swagger-ui.html
   - Notification: http://localhost:8086/swagger-ui.html

## 📝 使用说明

### 统一响应格式

所有API使用统一的响应格式 `Result<T>`:

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2025-11-06T10:00:00Z"
}
```

### 分页响应格式

分页API使用 `PageResult<T>`:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2025-11-06T10:00:00Z"
}
```

### 认证方式

使用JWT Bearer Token认证:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## 📊 API统计

| 服务         | API端点数 | 完成状态  |
| ------------ | --------- | --------- |
| Gateway      | 路由配置  | ✅ 100%   |
| Auth         | 11个      | ✅ 100%   |
| Book         | 15个      | ⏳ 进行中 |
| Circulation  | 17个      | ⏳ 进行中 |
| Reader       | 20个      | ⏳ 进行中 |
| System       | 40个      | ⏳ 进行中 |
| Notification | 25个      | ⚪ 未开始 |
| **总计**     | **128个** | **42%**   |

## 🔄 更新记录

- 2025-11-06: 初始版本创建,完成通用定义和Book Service规范

## 📞 联系方式

- 项目负责人: GCRF Team
- 邮箱: dev@gcrf.com
