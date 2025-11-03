# 国创睿峰智能图书馆管理系统 - API 设计文档

**版本**: v1.0
**编写日期**: 2025-10-14
**编写人**: API架构师团队
**文档状态**: 设计阶段

---

## 目录

1. [API架构概述](#1-api架构概述)
2. [API设计原则](#2-api设计原则)
3. [通用规范](#3-通用规范)
4. [认证鉴权服务API](#4-认证鉴权服务api)
5. [系统管理服务API](#5-系统管理服务api)
6. [图书管理服务API](#6-图书管理服务api)
7. [流通管理服务API](#7-流通管理服务api)
8. [读者管理服务API](#8-读者管理服务api)
9. [推荐服务API](#9-推荐服务api)
10. [智能问答服务API](#10-智能问答服务api)
11. [数据分析服务API](#11-数据分析服务api)
12. [文件服务API](#12-文件服务api)
13. [通知服务API](#13-通知服务api)
14. [搜索服务API](#14-搜索服务api)
15. [AI能力服务API](#15-ai能力服务api)
16. [API网关路由配置](#16-api网关路由配置)
17. [错误码规范](#17-错误码规范)
18. [性能与限流](#18-性能与限流)

---

## 1. API架构概述

### 1.1 架构图

```
┌──────────────────────────────────────────────────────────────┐
│                    客户端层 (Clients)                          │
├──────────────────────────────────────────────────────────────┤
│  Web Admin      │  WeChat Mini Program  │  Self-Service      │
│  (管理端)        │    (读者小程序)        │   (自助机)         │
└─────────┬────────┴───────────┬───────────┴───────────┬────────┘
          │                    │                       │
          └────────────────────▼───────────────────────┘
                              HTTPS
┌──────────────────────────────────────────────────────────────┐
│               API Gateway (Spring Cloud Gateway)              │
│         负载均衡 | 认证鉴权 | 限流熔断 | 路由转发              │
└─────┬────────┬────────┬────────┬────────┬────────┬───────────┘
      │        │        │        │        │        │
      ▼        ▼        ▼        ▼        ▼        ▼
┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
│  Auth   ││ System  ││  Book   ││Circul-  ││ Reader  ││Recommend│
│ Service ││ Service ││ Service ││ation    ││ Service ││ Service │
│         ││         ││         ││ Service ││         ││         │
└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘
     │          │          │          │          │          │
┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐┌─────────┐
│   NLP   ││Analytics││  File   ││ Notif-  ││ Search  ││ Vision  │
│ Service ││ Service ││ Service ││ ication ││ Service ││ Service │
│         ││         ││         ││ Service ││         ││         │
└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘└─────────┘
```

### 1.2 服务划分

| 服务名称 | 端口 | 职责说明 | 主要功能 |
|---------|------|---------|----------|
| gateway-service | 8080 | API网关 | 统一入口、路由转发、认证鉴权、限流熔断 |
| auth-service | 8081 | 认证鉴权服务 | 登录、注册、Token管理、权限验证 |
| system-service | 8084 | 系统管理服务 | 用户管理、角色权限、部门管理、系统配置 |
| book-service | 8082 | 图书管理服务 | 编目、典藏、盘点、荐购捐赠 |
| circulation-service | 8083 | 流通管理服务 | 借阅、归还、预约、续借、逾期处理 |
| reader-service | 8085 | 读者管理服务 | 读者证办理、读者信息管理、人脸特征 |
| recommend-service | 8086 | 推荐服务 | 个性化推荐、协同过滤、内容推荐 |
| nlp-service | 8087 | NLP服务 | 智能问答、意图识别、实体抽取 |
| analytics-service | 8088 | 数据分析服务 | 统计报表、趋势分析、用户画像 |
| file-service | 8089 | 文件服务 | 图片上传、文件存储、OCR识别 |
| notification-service | 8090 | 通知服务 | 消息推送、到期提醒、预约通知 |
| search-service | 8091 | 搜索服务 | 全文检索、语义搜索、搜索建议 |
| vision-service | 8092 | 视觉服务 | 人脸识别、封面识别、条码识别 |

---

## 2. API设计原则

### 2.1 RESTful规范

1. **资源命名**: 使用名词复数形式,避免动词
   - ✅ `GET /api/books`
   - ❌ `GET /api/getBooks`

2. **HTTP方法语义**
   - `GET`: 查询资源
   - `POST`: 创建资源
   - `PUT`: 完整更新资源
   - `PATCH`: 部分更新资源
   - `DELETE`: 删除资源

3. **URL层级**: 不超过3层
   - ✅ `/api/books/{id}/copies`
   - ❌ `/api/libraries/departments/shelves/books/copies`

### 2.2 版本管理

- 使用URL路径版本: `/api/v1/books`
- 当前版本: v1
- 向后兼容原则: 新版本不破坏旧版本API

### 2.3 响应格式统一

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1697267890123,
  "traceId": "abc-123-def"
}
```

### 2.4 分页规范

**请求参数**:
```
GET /api/books?pageNum=1&pageSize=20&sortBy=createdAt&sortOrder=desc
```

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5
  }
}
```

---

## 3. 通用规范

### 3.1 请求头

| 字段名 | 是否必填 | 说明 | 示例 |
|-------|---------|------|------|
| Content-Type | 是 | 内容类型 | application/json |
| Authorization | 部分 | 认证Token | Bearer eyJhbGc... |
| X-Request-ID | 否 | 请求追踪ID | uuid-1234 |
| X-Client-Type | 否 | 客户端类型 | web/mobile/kiosk |
| X-Client-Version | 否 | 客户端版本 | 1.0.0 |

### 3.2 响应头

| 字段名 | 说明 | 示例 |
|-------|------|------|
| X-RateLimit-Limit | 限流上限 | 1000 |
| X-RateLimit-Remaining | 剩余请求次数 | 998 |
| X-RateLimit-Reset | 重置时间戳 | 1697267890 |
| X-Response-Time | 响应时间(ms) | 125 |

### 3.3 状态码规范

| HTTP状态码 | 业务code | 说明 |
|-----------|----------|------|
| 200 | 200 | 成功 |
| 201 | 201 | 创建成功 |
| 400 | 400-499 | 客户端错误(参数错误、业务规则) |
| 401 | 401 | 未认证 |
| 403 | 403 | 无权限 |
| 404 | 404 | 资源不存在 |
| 429 | 429 | 请求过多,限流 |
| 500 | 500-599 | 服务器错误 |

---

## 4. 认证鉴权服务API

**基础路径**: `/api/v1/auth`

### 4.1 用户登录

**接口**: `POST /auth/login`

**描述**: 管理员登录系统

**请求体**:
```json
{
  "username": "admin",
  "password": "admin123",
  "captcha": "5678",
  "captchaKey": "uuid-1234"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "refresh_token_abc",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "username": "admin",
      "realName": "张三",
      "role": "ADMIN",
      "permissions": ["book:create", "book:edit", "circulation:borrow"]
    }
  }
}
```

### 4.2 刷新Token

**接口**: `POST /auth/refresh`

**请求体**:
```json
{
  "refreshToken": "refresh_token_abc"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "new_access_token",
    "expiresIn": 7200
  }
}
```

### 4.3 登出

**接口**: `POST /auth/logout`

**请求头**: Authorization: Bearer {token}

**响应**:
```json
{
  "code": 200,
  "message": "登出成功"
}
```

### 4.4 获取验证码

**接口**: `GET /auth/captcha`

**响应**:
```json
{
  "code": 200,
  "data": {
    "captchaKey": "uuid-1234",
    "captchaImage": "data:image/png;base64,iVBORw0KGg..."
  }
}
```

### 4.5 修改密码

**接口**: `PUT /auth/password`

**请求头**: Authorization: Bearer {token}

**请求体**:
```json
{
  "oldPassword": "old123",
  "newPassword": "new456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

---

## 5. 系统管理服务API

**基础路径**: `/api/v1/system`

### 5.1 用户管理

#### 5.1.1 查询用户列表

**接口**: `GET /system/users`

**请求参数**:
```
?username=admin&role=ADMIN&status=ACTIVE&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "userId": 1,
        "username": "admin",
        "realName": "管理员",
        "role": "ADMIN",
        "email": "admin@example.com",
        "phone": "13800138000",
        "status": "ACTIVE",
        "lastLoginAt": "2025-10-14T10:30:00",
        "createdAt": "2025-01-01T00:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

#### 5.1.2 创建用户

**接口**: `POST /system/users`

**请求体**:
```json
{
  "username": "librarian01",
  "password": "lib123456",
  "realName": "李四",
  "role": "LIBRARIAN",
  "email": "lisi@example.com",
  "phone": "13900139000",
  "departmentId": 2
}
```

**响应**:
```json
{
  "code": 201,
  "message": "用户创建成功",
  "data": {
    "userId": 2,
    "username": "librarian01"
  }
}
```

#### 5.1.3 更新用户信息

**接口**: `PUT /system/users/{userId}`

**请求体**:
```json
{
  "realName": "李四",
  "email": "lisi_new@example.com",
  "phone": "13900139001",
  "status": "ACTIVE"
}
```

#### 5.1.4 删除用户

**接口**: `DELETE /system/users/{userId}`

#### 5.1.5 重置用户密码

**接口**: `POST /system/users/{userId}/reset-password`

**请求体**:
```json
{
  "newPassword": "reset123456"
}
```

### 5.2 角色权限管理

#### 5.2.1 查询角色列表

**接口**: `GET /system/roles`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "roleId": 1,
      "roleName": "管理员",
      "roleCode": "ADMIN",
      "description": "系统管理员,拥有所有权限",
      "permissions": [
        {"permissionId": 1, "permissionCode": "system:user:create", "name": "创建用户"},
        {"permissionId": 2, "permissionCode": "book:create", "name": "图书编目"}
      ]
    }
  ]
}
```

#### 5.2.2 创建角色

**接口**: `POST /system/roles`

#### 5.2.3 分配权限

**接口**: `POST /system/roles/{roleId}/permissions`

**请求体**:
```json
{
  "permissionIds": [1, 2, 3, 5]
}
```

### 5.3 部门管理

#### 5.3.1 查询部门列表

**接口**: `GET /system/departments`

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "deptCode": "ROOT",
        "deptName": "广州城市图书馆",
        "parentId": null,
        "deptLevel": 1,
        "deptPath": "/1",
        "leaderName": "馆长",
        "phone": "020-12345678",
        "status": "ACTIVE"
      }
    ],
    "total": 6
  }
}
```

#### 5.3.2 创建部门

**接口**: `POST /system/departments`

**请求体**:
```json
{
  "deptCode": "TECH",
  "deptName": "技术部",
  "parentId": 1,
  "leaderName": "张三",
  "phone": "020-88888888",
  "email": "tech@library.com",
  "description": "负责技术服务"
}
```

#### 5.3.3 更新部门

**接口**: `PUT /system/departments/{deptId}`

#### 5.3.4 删除部门

**接口**: `DELETE /system/departments/{deptId}`

### 5.4 系统配置

#### 5.4.1 获取系统配置

**接口**: `GET /system/configs`

**响应**:
```json
{
  "code": 200,
  "data": {
    "libraryName": "广州城市图书馆",
    "studentMaxBorrow": 5,
    "studentBorrowDays": 30,
    "teacherMaxBorrow": 10,
    "teacherBorrowDays": 60,
    "overdueFinePer Day": 0.10,
    "reservationDays": 7,
    "notificationEnabled": true
  }
}
```

#### 5.4.2 更新系统配置

**接口**: `PUT /system/configs`

**请求体**:
```json
{
  "studentMaxBorrow": 5,
  "studentBorrowDays": 30
}
```

### 5.5 数据备份

#### 5.5.1 创建备份任务

**接口**: `POST /system/backups`

**请求体**:
```json
{
  "backupType": "FULL",
  "description": "2025年10月全量备份"
}
```

#### 5.5.2 查询备份列表

**接口**: `GET /system/backups`

#### 5.5.3 恢复备份

**接口**: `POST /system/backups/{backupId}/restore`

---

## 6. 图书管理服务API

**基础路径**: `/api/v1/books`

### 6.1 图书编目

#### 6.1.1 查询图书列表

**接口**: `GET /books`

**请求参数**:
```
?keyword=三体&category=I247&status=AVAILABLE&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "bookId": 1,
        "isbn": "9787536692930",
        "title": "三体",
        "author": "刘慈欣",
        "publisher": "重庆出版社",
        "publishDate": "2008-01-01",
        "category": "I247.55",
        "callNumber": "I247.55/2",
        "price": 23.00,
        "coverUrl": "https://cdn.example.com/covers/santiganti.jpg",
        "summary": "地球文明向宇宙发出第一声啼鸣...",
        "keywords": "科幻,宇宙,文明",
        "tags": ["雨果奖", "科幻", "刘慈欣"],
        "totalCopies": 5,
        "availableCopies": 3,
        "borrowCount": 127,
        "rating": 4.8,
        "status": "AVAILABLE",
        "createdAt": "2025-01-15T10:00:00"
      }
    ],
    "total": 1
  }
}
```

#### 6.1.2 根据ISBN查询图书

**接口**: `GET /books/isbn/{isbn}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "bookId": 1,
    "isbn": "9787536692930",
    "title": "三体",
    "author": "刘慈欣",
    "publisher": "重庆出版社",
    "marc": {
      "field_245": "三体 / 刘慈欣著",
      "field_260": "重庆 : 重庆出版社, 2008",
      "field_300": "302页"
    }
  }
}
```

#### 6.1.3 创建图书编目

**接口**: `POST /books`

**请求体**:
```json
{
  "isbn": "9787536692930",
  "title": "三体",
  "author": "刘慈欣",
  "publisher": "重庆出版社",
  "publishDate": "2008-01-01",
  "category": "I247.55",
  "price": 23.00,
  "pages": 302,
  "summary": "地球文明向宇宙发出第一声啼鸣...",
  "keywords": "科幻,宇宙,文明"
}
```

**响应**:
```json
{
  "code": 201,
  "message": "图书编目成功",
  "data": {
    "bookId": 1,
    "callNumber": "I247.55/2"
  }
}
```

#### 6.1.4 云端数据检索

**接口**: `GET /books/cloud-search`

**请求参数**:
```
?isbn=9787536692930
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "isbn": "9787536692930",
    "title": "三体",
    "author": "刘慈欣",
    "publisher": "重庆出版社",
    "publishDate": "2008-01-01",
    "category": "I247.55",
    "coverUrl": "https://cloud-db.example.com/covers/xxx.jpg",
    "marc": {...},
    "source": "National Library Cloud Database"
  }
}
```

#### 6.1.5 批量导入图书

**接口**: `POST /books/batch-import`

**请求体**: multipart/form-data
- file: Excel文件或MARC文件

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalCount": 100,
    "successCount": 98,
    "failCount": 2,
    "errors": [
      {"row": 5, "reason": "ISBN格式错误"},
      {"row": 23, "reason": "图书已存在"}
    ]
  }
}
```

#### 6.1.6 更新图书信息

**接口**: `PUT /books/{bookId}`

#### 6.1.7 删除图书

**接口**: `DELETE /books/{bookId}`

### 6.2 图书典藏

#### 6.2.1 创建图书副本(典藏)

**接口**: `POST /books/{bookId}/copies`

**请求体**:
```json
{
  "copyCount": 3,
  "location": "A区3层",
  "rfidTag": true
}
```

**响应**:
```json
{
  "code": 201,
  "data": {
    "copies": [
      {
        "copyId": 1,
        "barcode": "BK00001",
        "rfidCode": "RFID-001",
        "callNumber": "I247.55/2-1",
        "location": "A区3层"
      },
      {
        "copyId": 2,
        "barcode": "BK00002",
        "rfidCode": "RFID-002",
        "callNumber": "I247.55/2-2",
        "location": "A区3层"
      }
    ]
  }
}
```

#### 6.2.2 查询图书副本列表

**接口**: `GET /books/{bookId}/copies`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "copyId": 1,
      "barcode": "BK00001",
      "callNumber": "I247.55/2-1",
      "location": "A区3层",
      "status": "AVAILABLE",
      "borrowCount": 23,
      "lastBorrowedAt": "2025-10-10T14:30:00"
    }
  ]
}
```

#### 6.2.3 条码置换

**接口**: `PUT /books/copies/{copyId}/barcode`

**请求体**:
```json
{
  "newBarcode": "BK99999"
}
```

#### 6.2.4 图书剔旧

**接口**: `POST /books/copies/{copyId}/discard`

**请求体**:
```json
{
  "reason": "DAMAGED",
  "description": "图书严重损坏,无法修复"
}
```

### 6.3 图书盘点

#### 6.3.1 创建盘点任务

**接口**: `POST /books/inventory-tasks`

**请求体**:
```json
{
  "taskName": "2025年10月全馆盘点",
  "inventoryType": "FULL",
  "category": "I",
  "location": "A区",
  "scheduledAt": "2025-10-20T08:00:00"
}
```

**响应**:
```json
{
  "code": 201,
  "data": {
    "taskId": "TASK-20251014001",
    "status": "PENDING"
  }
}
```

#### 6.3.2 提交盘点数据

**接口**: `POST /books/inventory-tasks/{taskId}/records`

**请求体**:
```json
{
  "scannedBarcodes": ["BK00001", "BK00002", "BK00003"]
}
```

#### 6.3.3 查询盘点结果

**接口**: `GET /books/inventory-tasks/{taskId}/result`

**响应**:
```json
{
  "code": 200,
  "data": {
    "taskId": "TASK-20251014001",
    "status": "COMPLETED",
    "totalBooks": 10000,
    "scannedBooks": 9980,
    "missingBooks": 20,
    "extraBooks": 0,
    "misplacedBooks": 15,
    "discrepancies": [
      {
        "barcode": "BK00123",
        "title": "红楼梦",
        "type": "MISSING",
        "expectedLocation": "A区1层",
        "actualLocation": null
      }
    ]
  }
}
```

### 6.4 图书荐购

#### 6.4.1 提交荐购申请

**接口**: `POST /books/recommendations`

**请求体**:
```json
{
  "isbn": "9787020002207",
  "title": "红楼梦",
  "author": "曹雪芹",
  "reason": "经典名著,建议采购增加副本",
  "readerId": 100
}
```

#### 6.4.2 查询荐购列表

**接口**: `GET /books/recommendations`

**请求参数**:
```
?status=PENDING&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "recommendationId": 1,
        "isbn": "9787020002207",
        "title": "红楼梦",
        "author": "曹雪芹",
        "reason": "经典名著",
        "readerId": 100,
        "readerName": "张三",
        "status": "PENDING",
        "aiScore": 0.92,
        "createdAt": "2025-10-14T10:00:00"
      }
    ],
    "total": 15
  }
}
```

#### 6.4.3 审核荐购申请

**接口**: `PUT /books/recommendations/{recommendationId}/review`

**请求体**:
```json
{
  "status": "APPROVED",
  "comment": "同意采购,已加入采购清单"
}
```

### 6.5 图书捐赠

#### 6.5.1 提交捐赠申请

**接口**: `POST /books/donations`

**请求体**:
```json
{
  "isbn": "9787020002207",
  "title": "红楼梦",
  "author": "曹雪芹",
  "condition": "GOOD",
  "donorName": "李四",
  "donorPhone": "13900139000",
  "deliveryMethod": "ONSITE"
}
```

#### 6.5.2 查询捐赠列表

**接口**: `GET /books/donations`

#### 6.5.3 验收捐赠图书

**接口**: `PUT /books/donations/{donationId}/accept`

**请求体**:
```json
{
  "status": "ACCEPTED",
  "certificateUrl": "https://cdn.example.com/certificates/xxx.pdf"
}
```

### 6.6 打印条码

#### 6.6.1 生成条码数据

**接口**: `POST /books/barcodes/generate`

**请求体**:
```json
{
  "copyIds": [1, 2, 3],
  "template": "STANDARD",
  "includeCallNumber": true
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "printJobId": "PJ-20251014001",
    "barcodes": [
      {
        "copyId": 1,
        "barcode": "BK00001",
        "callNumber": "I247.55/2-1",
        "barcodeImage": "data:image/png;base64,..."
      }
    ]
  }
}
```

---

## 7. 流通管理服务API

**基础路径**: `/api/v1/circulation`

### 7.1 图书借出

#### 7.1.1 验证读者借阅资格

**接口**: `POST /circulation/borrow/validate`

**请求体**:
```json
{
  "readerNo": "RD20250001",
  "barcodes": ["BK00001", "BK00002"]
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "readerInfo": {
      "readerId": 100,
      "readerNo": "RD20250001",
      "name": "张三",
      "type": "STUDENT",
      "grade": "五年级1班",
      "currentBorrowCount": 3,
      "maxBorrow": 5,
      "hasOverdue": false
    },
    "bookValidation": [
      {
        "barcode": "BK00001",
        "title": "三体",
        "status": "AVAILABLE",
        "canBorrow": true
      },
      {
        "barcode": "BK00002",
        "title": "红楼梦",
        "status": "BORROWED",
        "canBorrow": false,
        "reason": "图书已被借出"
      }
    ]
  }
}
```

#### 7.1.2 办理借阅

**接口**: `POST /circulation/borrow`

**请求体**:
```json
{
  "readerNo": "RD20250001",
  "barcodes": ["BK00001"],
  "operatorId": 1
}
```

**响应**:
```json
{
  "code": 201,
  "message": "借阅成功",
  "data": {
    "circulationRecords": [
      {
        "circulationId": 1,
        "barcode": "BK00001",
        "title": "三体",
        "borrowDate": "2025-10-14T10:30:00",
        "dueDate": "2025-11-13T23:59:59"
      }
    ]
  }
}
```

### 7.2 图书归还

#### 7.2.1 扫码归还

**接口**: `POST /circulation/return`

**请求体**:
```json
{
  "barcodes": ["BK00001"],
  "operatorId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "returnRecords": [
      {
        "circulationId": 1,
        "barcode": "BK00001",
        "title": "三体",
        "readerName": "张三",
        "borrowDate": "2025-10-14T10:30:00",
        "returnDate": "2025-10-20T14:00:00",
        "isOverdue": false,
        "overdueDays": 0,
        "overdueFine": 0,
        "hasReservation": true,
        "nextReaderName": "李四"
      }
    ]
  }
}
```

#### 7.2.2 处理逾期罚款

**接口**: `POST /circulation/overdue-fines/{circulationId}/pay`

**请求体**:
```json
{
  "amount": 3.50,
  "paymentMethod": "CASH"
}
```

### 7.3 图书续借

#### 7.3.1 办理续借

**接口**: `POST /circulation/renew`

**请求体**:
```json
{
  "circulationId": 1,
  "readerId": 100
}
```

**响应**:
```json
{
  "code": 200,
  "message": "续借成功",
  "data": {
    "circulationId": 1,
    "newDueDate": "2025-12-13T23:59:59",
    "renewCount": 1
  }
}
```

### 7.4 图书预约

#### 7.4.1 提交预约

**接口**: `POST /circulation/reservations`

**请求体**:
```json
{
  "bookId": 1,
  "readerId": 100
}
```

**响应**:
```json
{
  "code": 201,
  "data": {
    "reservationId": 1,
    "bookTitle": "三体",
    "queuePosition": 3,
    "estimatedAvailableDate": "2025-10-25"
  }
}
```

#### 7.4.2 查询预约列表

**接口**: `GET /circulation/reservations`

**请求参数**:
```
?readerId=100&status=ACTIVE
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "reservationId": 1,
      "bookId": 1,
      "bookTitle": "三体",
      "coverUrl": "https://cdn.example.com/covers/santi.jpg",
      "queuePosition": 3,
      "status": "ACTIVE",
      "createdAt": "2025-10-14T10:00:00",
      "estimatedAvailableDate": "2025-10-25"
    }
  ]
}
```

#### 7.4.3 取消预约

**接口**: `DELETE /circulation/reservations/{reservationId}`

#### 7.4.4 预约到书通知

**接口**: `POST /circulation/reservations/{reservationId}/notify`

### 7.5 流通记录查询

#### 7.5.1 查询借阅记录

**接口**: `GET /circulation/records`

**请求参数**:
```
?readerId=100&status=BORROWED&startDate=2025-01-01&endDate=2025-10-14&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "circulationId": 1,
        "barcode": "BK00001",
        "bookTitle": "三体",
        "coverUrl": "https://cdn.example.com/covers/santi.jpg",
        "readerNo": "RD20250001",
        "readerName": "张三",
        "borrowDate": "2025-10-14T10:30:00",
        "dueDate": "2025-11-13T23:59:59",
        "returnDate": null,
        "status": "BORROWED",
        "isOverdue": false,
        "renewCount": 0
      }
    ],
    "total": 1
  }
}
```

#### 7.5.2 查询读者当前借阅

**接口**: `GET /circulation/readers/{readerId}/current-borrows`

**响应**:
```json
{
  "code": 200,
  "data": {
    "currentBorrowCount": 3,
    "maxBorrow": 5,
    "records": [
      {
        "circulationId": 1,
        "barcode": "BK00001",
        "bookTitle": "三体",
        "coverUrl": "https://cdn.example.com/covers/santi.jpg",
        "borrowDate": "2025-10-14T10:30:00",
        "dueDate": "2025-11-13T23:59:59",
        "daysRemaining": 30,
        "canRenew": true,
        "renewCount": 0
      }
    ]
  }
}
```

#### 7.5.3 查询逾期记录

**接口**: `GET /circulation/overdue`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "circulationId": 10,
      "readerNo": "RD20250005",
      "readerName": "王五",
      "bookTitle": "红楼梦",
      "barcode": "BK00123",
      "borrowDate": "2025-09-01T10:00:00",
      "dueDate": "2025-10-01T23:59:59",
      "overdueDays": 13,
      "overdueFine": 1.30
    }
  ]
}
```

---

## 8. 读者管理服务API

**基础路径**: `/api/v1/readers`

### 8.1 读者信息管理

#### 8.1.1 查询读者列表

**接口**: `GET /readers`

**请求参数**:
```
?keyword=张三&type=STUDENT&grade=五年级&status=ACTIVE&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "readerId": 100,
        "readerNo": "RD20250001",
        "name": "张三",
        "type": "STUDENT",
        "grade": "五年级",
        "class": "1班",
        "gender": "MALE",
        "phone": "13800138000",
        "email": "zhangsan@example.com",
        "avatarUrl": "https://cdn.example.com/avatars/100.jpg",
        "hasFaceFeature": true,
        "currentBorrowCount": 3,
        "totalBorrowCount": 127,
        "status": "ACTIVE",
        "createdAt": "2025-01-15T10:00:00"
      }
    ],
    "total": 1
  }
}
```

#### 8.1.2 根据读者证号查询

**接口**: `GET /readers/readerNo/{readerNo}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "readerId": 100,
    "readerNo": "RD20250001",
    "name": "张三",
    "type": "STUDENT",
    "grade": "五年级",
    "class": "1班",
    "gender": "MALE",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "avatarUrl": "https://cdn.example.com/avatars/100.jpg",
    "maxBorrow": 5,
    "borrowDays": 30,
    "currentBorrowCount": 3,
    "totalBorrowCount": 127,
    "status": "ACTIVE",
    "hasFaceFeature": true,
    "hasOverdue": false,
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

#### 8.1.3 创建读者

**接口**: `POST /readers`

**请求体**:
```json
{
  "name": "张三",
  "type": "STUDENT",
  "grade": "五年级",
  "class": "1班",
  "gender": "MALE",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "idCard": "440106200801011234"
}
```

**响应**:
```json
{
  "code": 201,
  "message": "读者创建成功",
  "data": {
    "readerId": 100,
    "readerNo": "RD20250001"
  }
}
```

#### 8.1.4 更新读者信息

**接口**: `PUT /readers/{readerId}`

**请求体**:
```json
{
  "phone": "13800138001",
  "email": "zhangsan_new@example.com",
  "grade": "六年级",
  "class": "2班"
}
```

#### 8.1.5 删除(注销)读者

**接口**: `DELETE /readers/{readerId}`

**请求参数**:
```
?reason=GRADUATED
```

#### 8.1.6 批量导入读者

**接口**: `POST /readers/batch-import`

**请求体**: multipart/form-data
- file: Excel文件

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalCount": 50,
    "successCount": 48,
    "failCount": 2,
    "errors": [
      {"row": 5, "reason": "手机号格式错误"},
      {"row": 23, "reason": "读者证号重复"}
    ]
  }
}
```

#### 8.1.7 批量注销读者

**接口**: `POST /readers/batch-deactivate`

**请求体**:
```json
{
  "grade": "六年级",
  "reason": "GRADUATED"
}
```

### 8.2 人脸特征管理

#### 8.2.1 采集人脸特征

**接口**: `POST /readers/{readerId}/face-features`

**请求体**: multipart/form-data
- faceImage: 人脸照片文件

**响应**:
```json
{
  "code": 200,
  "data": {
    "faceFeatureId": "FACE-20251014001",
    "quality": 0.95,
    "capturedAt": "2025-10-14T10:30:00"
  }
}
```

#### 8.2.2 更新人脸特征

**接口**: `PUT /readers/{readerId}/face-features`

#### 8.2.3 删除人脸特征

**接口**: `DELETE /readers/{readerId}/face-features`

#### 8.2.4 人脸识别登录

**接口**: `POST /readers/face-recognition`

**请求体**: multipart/form-data
- faceImage: 人脸照片文件

**响应**:
```json
{
  "code": 200,
  "data": {
    "readerId": 100,
    "readerNo": "RD20250001",
    "name": "张三",
    "confidence": 0.98
  }
}
```

### 8.3 读者统计

#### 8.3.1 读者借阅统计

**接口**: `GET /readers/{readerId}/statistics`

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalBorrowCount": 127,
    "currentBorrowCount": 3,
    "overdueCount": 0,
    "reservationCount": 1,
    "averageBorrowDays": 25,
    "favoriteCategories": [
      {"category": "I247", "categoryName": "小说", "count": 45},
      {"category": "K", "categoryName": "历史地理", "count": 32}
    ],
    "readingStreak": 15
  }
}
```

#### 8.3.2 读者阅读报告

**接口**: `GET /readers/{readerId}/reading-report`

**请求参数**:
```
?year=2025&month=10
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "period": "2025-10",
    "booksRead": 8,
    "totalPages": 2560,
    "averageRating": 4.5,
    "favoriteGenres": ["科幻", "历史", "经典文学"],
    "readingGoal": {
      "target": 10,
      "completed": 8,
      "progress": 0.80
    },
    "achievements": [
      {"id": 1, "name": "连续阅读30天", "earnedAt": "2025-10-10"}
    ]
  }
}
```

---

## 9. 推荐服务API

**基础路径**: `/api/v1/recommend`

### 9.1 个性化推荐

#### 9.1.1 首页推荐

**接口**: `GET /recommend/homepage`

**请求参数**:
```
?readerId=100&limit=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "forYou": [
      {
        "bookId": 1,
        "title": "三体",
        "author": "刘慈欣",
        "coverUrl": "https://cdn.example.com/covers/santi.jpg",
        "rating": 4.8,
        "score": 0.95,
        "reason": "根据您的阅读历史推荐",
        "algorithm": "COLLABORATIVE_FILTERING"
      }
    ],
    "trending": [
      {
        "bookId": 2,
        "title": "活着",
        "author": "余华",
        "coverUrl": "https://cdn.example.com/covers/huozhe.jpg",
        "rating": 4.9,
        "borrowCount": 89,
        "reason": "本周热门图书"
      }
    ],
    "newArrivals": [
      {
        "bookId": 3,
        "title": "人类简史",
        "author": "尤瓦尔·赫拉利",
        "coverUrl": "https://cdn.example.com/covers/sapiens.jpg",
        "arrivedAt": "2025-10-01"
      }
    ]
  }
}
```

#### 9.1.2 图书详情页推荐

**接口**: `GET /recommend/similar`

**请求参数**:
```
?bookId=1&limit=10
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "alsoViewed": [
      {
        "bookId": 4,
        "title": "三体II:黑暗森林",
        "author": "刘慈欣",
        "coverUrl": "https://cdn.example.com/covers/santi2.jpg",
        "similarity": 0.92
      }
    ],
    "similarBooks": [
      {
        "bookId": 5,
        "title": "基地",
        "author": "艾萨克·阿西莫夫",
        "coverUrl": "https://cdn.example.com/covers/foundation.jpg",
        "similarity": 0.85,
        "reason": "同为科幻经典"
      }
    ]
  }
}
```

#### 9.1.3 搜索推荐

**接口**: `GET /recommend/search-suggestions`

**请求参数**:
```
?query=科幻&limit=10
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "suggestions": ["科幻小说", "科幻电影", "科幻世界"],
    "relatedBooks": [
      {
        "bookId": 1,
        "title": "三体",
        "author": "刘慈欣"
      }
    ]
  }
}
```

#### 9.1.4 主题推荐

**接口**: `GET /recommend/themes/{themeId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "themeId": "WORLD_BOOK_DAY",
    "themeName": "世界读书日推荐",
    "description": "在世界读书日,为您精选经典好书",
    "books": [
      {
        "bookId": 1,
        "title": "红楼梦",
        "author": "曹雪芹",
        "coverUrl": "https://cdn.example.com/covers/hlm.jpg"
      }
    ]
  }
}
```

### 9.2 推荐反馈

#### 9.2.1 记录推荐点击

**接口**: `POST /recommend/feedback/click`

**请求体**:
```json
{
  "recommendationId": "REC-20251014001",
  "bookId": 1,
  "readerId": 100
}
```

#### 9.2.2 记录推荐借阅

**接口**: `POST /recommend/feedback/borrow`

**请求体**:
```json
{
  "recommendationId": "REC-20251014001",
  "bookId": 1,
  "readerId": 100
}
```

#### 9.2.3 推荐评分

**接口**: `POST /recommend/feedback/rating`

**请求体**:
```json
{
  "recommendationId": "REC-20251014001",
  "rating": 5,
  "feedback": "推荐很准确"
}
```

---

## 10. 智能问答服务API

**基础路径**: `/api/v1/nlp`

### 10.1 问答接口

#### 10.1.1 提交问题

**接口**: `POST /nlp/qa`

**请求体**:
```json
{
  "question": "《三体》这本书在哪个书架?",
  "readerId": 100,
  "sessionId": "SESSION-20251014001"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "answer": "《三体》位于科幻小说区,A区3层,索书号I247.55/2",
    "intent": "BOOK_LOCATION_QUERY",
    "entities": [
      {"type": "BOOK_TITLE", "value": "三体"}
    ],
    "relatedBooks": [
      {
        "bookId": 1,
        "title": "三体",
        "location": "A区3层",
        "callNumber": "I247.55/2",
        "status": "AVAILABLE"
      }
    ],
    "confidence": 0.95,
    "sessionId": "SESSION-20251014001"
  }
}
```

#### 10.1.2 多轮对话

**接口**: `POST /nlp/qa/multi-turn`

**请求体**:
```json
{
  "message": "那这本书现在能借吗?",
  "sessionId": "SESSION-20251014001",
  "readerId": 100
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "answer": "《三体》目前有3本在架,您可以借阅。",
    "context": {
      "bookId": 1,
      "bookTitle": "三体"
    },
    "suggestedActions": [
      {
        "type": "BORROW",
        "label": "立即借阅",
        "action": "/circulation/borrow"
      }
    ]
  }
}
```

### 10.2 语音识别

#### 10.2.1 语音转文字

**接口**: `POST /nlp/asr`

**请求体**: multipart/form-data
- audioFile: 音频文件(wav/mp3)

**响应**:
```json
{
  "code": 200,
  "data": {
    "text": "帮我找《三体》这本书",
    "confidence": 0.92
  }
}
```

### 10.3 文本处理

#### 10.3.1 意图识别

**接口**: `POST /nlp/intent-recognition`

**请求体**:
```json
{
  "text": "我能借几本书?"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "intent": "BORROW_RULE_QUERY",
    "confidence": 0.95
  }
}
```

#### 10.3.2 实体抽取

**接口**: `POST /nlp/entity-extraction`

**请求体**:
```json
{
  "text": "刘慈欣的《三体》在哪?"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "entities": [
      {"type": "AUTHOR", "value": "刘慈欣", "startPos": 0, "endPos": 3},
      {"type": "BOOK_TITLE", "value": "三体", "startPos": 5, "endPos": 7}
    ]
  }
}
```

---

## 11. 数据分析服务API

**基础路径**: `/api/v1/analytics`

### 11.1 馆情分析

#### 11.1.1 借阅概览

**接口**: `GET /analytics/overview`

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalBooks": 50000,
    "totalCopies": 75000,
    "availableCopies": 68500,
    "borrowedCopies": 6500,
    "totalReaders": 3000,
    "activeReaders": 1200,
    "todayBorrows": 156,
    "todayReturns": 142,
    "todayVisitors": 489
  }
}
```

#### 11.1.2 借阅趋势

**接口**: `GET /analytics/borrow-trends`

**请求参数**:
```
?timeRange=LAST_30_DAYS&granularity=DAILY
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "labels": ["2025-09-15", "2025-09-16", ..., "2025-10-14"],
    "borrowCounts": [150, 160, 145, ..., 156],
    "returnCounts": [148, 155, 150, ..., 142]
  }
}
```

#### 11.1.3 分类统计

**接口**: `GET /analytics/category-stats`

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "category": "I",
      "categoryName": "文学",
      "bookCount": 15000,
      "borrowCount": 3456,
      "percentage": 0.30
    },
    {
      "category": "K",
      "categoryName": "历史地理",
      "bookCount": 8000,
      "borrowCount": 2100,
      "percentage": 0.16
    }
  ]
}
```

### 11.2 读者分析

#### 11.2.1 读者画像

**接口**: `GET /analytics/reader-profile/{readerId}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "readerId": 100,
    "readerType": "STUDENT",
    "grade": "五年级",
    "borrowFrequency": "HIGH",
    "favoriteCategories": [
      {"category": "I247", "percentage": 0.35},
      {"category": "K", "percentage": 0.25}
    ],
    "readingHabits": {
      "averageBorrowDuration": 25,
      "preferredBorrowTime": "WEEKDAY_AFTERNOON",
      "overdueRate": 0.02
    },
    "recommendations": {
      "ctr": 0.45,
      "conversionRate": 0.28
    }
  }
}
```

#### 11.2.2 读者排行

**接口**: `GET /analytics/reader-rankings`

**请求参数**:
```
?rankBy=BORROW_COUNT&timeRange=THIS_YEAR&limit=20
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "rank": 1,
      "readerId": 100,
      "readerNo": "RD20250001",
      "name": "张三",
      "borrowCount": 127,
      "grade": "五年级1班"
    }
  ]
}
```

### 11.3 图书分析

#### 11.3.1 图书排行

**接口**: `GET /analytics/book-rankings`

**请求参数**:
```
?rankBy=BORROW_COUNT&category=I&timeRange=THIS_MONTH&limit=20
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "rank": 1,
      "bookId": 1,
      "title": "三体",
      "author": "刘慈欣",
      "coverUrl": "https://cdn.example.com/covers/santi.jpg",
      "borrowCount": 89,
      "rating": 4.8
    }
  ]
}
```

#### 11.3.2 零借阅分析

**接口**: `GET /analytics/zero-borrow`

**请求参数**:
```
?days=365
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalZeroBorrowBooks": 3500,
    "zeroBorrowRate": 0.07,
    "books": [
      {
        "bookId": 123,
        "title": "xxx",
        "author": "xxx",
        "category": "xxx",
        "arrivedAt": "2020-01-15",
        "daysSinceArrival": 1734,
        "recommendedAction": "DISCARD"
      }
    ]
  }
}
```

#### 11.3.3 图书周转率

**接口**: `GET /analytics/book-turnover`

**响应**:
```json
{
  "code": 200,
  "data": {
    "overallTurnoverRate": 2.5,
    "categoryTurnover": [
      {"category": "I", "categoryName": "文学", "turnoverRate": 3.2},
      {"category": "K", "categoryName": "历史地理", "turnoverRate": 2.1}
    ]
  }
}
```

### 11.4 预测分析

#### 11.4.1 借阅趋势预测

**接口**: `GET /analytics/predictions/borrow-trends`

**请求参数**:
```
?futureMonths=3
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "predictions": [
      {"month": "2025-11", "predictedBorrowCount": 4500, "confidence": 0.85},
      {"month": "2025-12", "predictedBorrowCount": 4200, "confidence": 0.82},
      {"month": "2026-01", "predictedBorrowCount": 3800, "confidence": 0.78}
    ]
  }
}
```

#### 11.4.2 采购建议

**接口**: `GET /analytics/predictions/purchase-suggestions`

**响应**:
```json
{
  "code": 200,
  "data": {
    "suggestions": [
      {
        "category": "I247",
        "categoryName": "小说",
        "recommendedCount": 50,
        "reason": "该分类借阅率高,现有库存周转快",
        "priority": "HIGH",
        "estimatedBudget": 1500.00
      }
    ]
  }
}
```

---

## 12. 文件服务API

**基础路径**: `/api/v1/files`

### 12.1 文件上传

#### 12.1.1 上传图片

**接口**: `POST /files/upload/image`

**请求体**: multipart/form-data
- file: 图片文件
- type: 用途类型(avatar/cover/face/other)

**响应**:
```json
{
  "code": 200,
  "data": {
    "fileId": "FILE-20251014001",
    "fileName": "avatar.jpg",
    "fileUrl": "https://cdn.example.com/images/avatar.jpg",
    "fileSize": 102400,
    "mimeType": "image/jpeg"
  }
}
```

#### 12.1.2 批量上传

**接口**: `POST /files/upload/batch`

**请求体**: multipart/form-data
- files[]: 多个文件

**响应**:
```json
{
  "code": 200,
  "data": {
    "successCount": 8,
    "failCount": 0,
    "files": [
      {
        "fileId": "FILE-20251014001",
        "fileName": "image1.jpg",
        "fileUrl": "https://cdn.example.com/images/image1.jpg"
      }
    ]
  }
}
```

### 12.2 OCR识别

#### 12.2.1 ISBN识别

**接口**: `POST /files/ocr/isbn`

**请求体**: multipart/form-data
- image: 图书封面或条码照片

**响应**:
```json
{
  "code": 200,
  "data": {
    "isbn": "9787536692930",
    "confidence": 0.98
  }
}
```

#### 12.2.2 封面识别

**接口**: `POST /files/ocr/book-cover`

**请求体**: multipart/form-data
- image: 图书封面照片

**响应**:
```json
{
  "code": 200,
  "data": {
    "title": "三体",
    "author": "刘慈欣",
    "isbn": "9787536692930",
    "confidence": 0.92
  }
}
```

---

## 13. 通知服务API

**基础路径**: `/api/v1/notifications`

### 13.1 消息推送

#### 13.1.1 发送到期提醒

**接口**: `POST /notifications/due-reminders`

**请求体**:
```json
{
  "readerId": 100,
  "circulationIds": [1, 2, 3],
  "channels": ["WECHAT", "SMS"]
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "notificationId": "NOTIF-20251014001",
    "status": "SENT",
    "sentChannels": ["WECHAT"],
    "failedChannels": []
  }
}
```

#### 13.1.2 发送预约到书通知

**接口**: `POST /notifications/reservation-available`

**请求体**:
```json
{
  "reservationId": 1,
  "readerId": 100,
  "bookTitle": "三体"
}
```

#### 13.1.3 查询通知历史

**接口**: `GET /notifications`

**请求参数**:
```
?readerId=100&type=DUE_REMINDER&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "notificationId": "NOTIF-20251014001",
        "type": "DUE_REMINDER",
        "title": "图书到期提醒",
        "content": "您借阅的《三体》将于3天后到期,请及时归还",
        "channel": "WECHAT",
        "status": "SENT",
        "sentAt": "2025-10-14T10:00:00",
        "readAt": null
      }
    ]
  }
}
```

---

## 14. 搜索服务API

**基础路径**: `/api/v1/search`

### 14.1 全文检索

#### 14.1.1 图书搜索

**接口**: `GET /search/books`

**请求参数**:
```
?q=三体&category=I&sortBy=RELEVANCE&pageNum=1&pageSize=20
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "bookId": 1,
        "title": "三体",
        "author": "刘慈欣",
        "publisher": "重庆出版社",
        "isbn": "9787536692930",
        "category": "I247.55",
        "coverUrl": "https://cdn.example.com/covers/santi.jpg",
        "summary": "地球文明向宇宙发出第一声啼鸣...",
        "availableCopies": 3,
        "rating": 4.8,
        "relevanceScore": 0.98
      }
    ],
    "total": 1,
    "took": 45
  }
}
```

#### 14.1.2 搜索建议

**接口**: `GET /search/suggestions`

**请求参数**:
```
?q=三&limit=10
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "suggestions": [
      "三体",
      "三国演义",
      "三字经"
    ]
  }
}
```

#### 14.1.3 语义搜索

**接口**: `POST /search/semantic`

**请求体**:
```json
{
  "query": "有关人工智能伦理的书",
  "limit": 20
}
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "bookId": 50,
      "title": "人工智能伦理学",
      "author": "xxx",
      "semanticScore": 0.92
    }
  ]
}
```

---

## 15. AI能力服务API

### 15.1 人脸识别服务

**基础路径**: `/api/v1/vision`

#### 15.1.1 人脸检测

**接口**: `POST /vision/face-detect`

**请求体**: multipart/form-data
- image: 人脸照片

**响应**:
```json
{
  "code": 200,
  "data": {
    "faceCount": 1,
    "faces": [
      {
        "faceBox": {"x": 100, "y": 150, "width": 200, "height": 250},
        "quality": 0.95,
        "age": 11,
        "gender": "MALE"
      }
    ]
  }
}
```

#### 15.1.2 人脸比对

**接口**: `POST /vision/face-compare`

**请求体**: multipart/form-data
- face1: 第一张人脸照片
- face2: 第二张人脸照片

**响应**:
```json
{
  "code": 200,
  "data": {
    "similarity": 0.98,
    "isMatch": true
  }
}
```

#### 15.1.3 人脸搜索

**接口**: `POST /vision/face-search`

**请求体**: multipart/form-data
- face: 待搜索的人脸照片

**响应**:
```json
{
  "code": 200,
  "data": {
    "matches": [
      {
        "readerId": 100,
        "readerNo": "RD20250001",
        "name": "张三",
        "similarity": 0.98
      }
    ]
  }
}
```

---

## 16. API网关路由配置

### 16.1 路由规则

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 认证服务
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=2

        # 系统服务
        - id: system-service
          uri: lb://system-service
          predicates:
            - Path=/api/v1/system/**
          filters:
            - StripPrefix=2
            - Auth

        # 图书服务
        - id: book-service
          uri: lb://book-service
          predicates:
            - Path=/api/v1/books/**
          filters:
            - StripPrefix=2
            - Auth

        # 流通服务
        - id: circulation-service
          uri: lb://circulation-service
          predicates:
            - Path=/api/v1/circulation/**
          filters:
            - StripPrefix=2
            - Auth

        # 读者服务
        - id: reader-service
          uri: lb://reader-service
          predicates:
            - Path=/api/v1/readers/**
          filters:
            - StripPrefix=2
            - Auth

        # 推荐服务
        - id: recommend-service
          uri: lb://recommend-service
          predicates:
            - Path=/api/v1/recommend/**
          filters:
            - StripPrefix=2

        # NLP服务
        - id: nlp-service
          uri: lb://nlp-service
          predicates:
            - Path=/api/v1/nlp/**
          filters:
            - StripPrefix=2

        # 数据分析服务
        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/v1/analytics/**
          filters:
            - StripPrefix=2
            - Auth

        # 文件服务
        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/v1/files/**
          filters:
            - StripPrefix=2

        # 通知服务
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - StripPrefix=2
            - Auth

        # 搜索服务
        - id: search-service
          uri: lb://search-service
          predicates:
            - Path=/api/v1/search/**
          filters:
            - StripPrefix=2

        # 视觉服务
        - id: vision-service
          uri: lb://vision-service
          predicates:
            - Path=/api/v1/vision/**
          filters:
            - StripPrefix=2
```

### 16.2 全局过滤器

1. **认证过滤器** (AuthFilter)
   - 验证JWT Token
   - 提取用户信息
   - 设置请求头: X-User-Id, X-User-Role

2. **限流过滤器** (RateLimitFilter)
   - 基于Redis的令牌桶算法
   - 默认限制: 1000次/分钟/IP

3. **日志过滤器** (LoggingFilter)
   - 记录请求/响应日志
   - 生成TraceId

4. **跨域过滤器** (CorsFilter)
   - 允许跨域请求

---

## 17. 错误码规范

### 17.1 错误码结构

格式: `XXYYYY`
- XX: 模块代码
- YYYY: 具体错误

### 17.2 模块代码

| 模块代码 | 模块名称 |
|---------|---------|
| 01 | 认证鉴权 |
| 02 | 系统管理 |
| 03 | 图书管理 |
| 04 | 流通管理 |
| 05 | 读者管理 |
| 06 | 推荐服务 |
| 07 | 智能问答 |
| 08 | 数据分析 |
| 09 | 文件服务 |
| 10 | 通知服务 |
| 11 | 搜索服务 |
| 12 | 视觉服务 |
| 99 | 系统级错误 |

### 17.3 常见错误码

| 错误码 | 说明 |
|-------|------|
| 010001 | 用户名或密码错误 |
| 010002 | Token已过期 |
| 010003 | Token无效 |
| 010004 | 验证码错误 |
| 030001 | ISBN已存在 |
| 030002 | 图书不存在 |
| 030003 | 条码号重复 |
| 040001 | 读者不存在 |
| 040002 | 图书已借出 |
| 040003 | 达到借阅上限 |
| 040004 | 存在逾期图书 |
| 040005 | 图书不可续借 |
| 050001 | 读者证号已存在 |
| 050002 | 人脸特征质量不合格 |
| 990001 | 系统内部错误 |
| 990002 | 数据库错误 |
| 990003 | 第三方服务错误 |

---

## 18. 性能与限流

### 18.1 性能指标

| 接口类型 | 响应时间要求 | 并发要求 |
|---------|-------------|---------|
| 查询类API | < 500ms | 500 QPS |
| 创建类API | < 1s | 200 QPS |
| 批量操作 | < 5s | 50 QPS |
| AI推荐 | < 500ms | 100 QPS |
| OCR识别 | < 2s | 20 QPS |

### 18.2 限流策略

#### 18.2.1 全局限流

- 基于IP: 1000次/分钟
- 基于用户: 500次/分钟

#### 18.2.2 接口级限流

| 接口路径 | 限流策略 |
|---------|---------|
| /auth/login | 10次/分钟/IP |
| /auth/captcha | 20次/分钟/IP |
| /files/upload/** | 100次/小时/用户 |
| /vision/face-** | 50次/小时/用户 |

#### 18.2.3 熔断策略

- 错误率阈值: 50%
- 最小请求数: 20
- 熔断时长: 10秒

---

## 附录

### A. API快速索引

**认证鉴权**
- POST /api/v1/auth/login - 登录
- POST /api/v1/auth/logout - 登出
- POST /api/v1/auth/refresh - 刷新Token

**系统管理**
- GET /api/v1/system/users - 查询用户
- GET /api/v1/system/departments - 查询部门
- GET /api/v1/system/configs - 获取配置

**图书管理**
- GET /api/v1/books - 查询图书
- POST /api/v1/books - 创建编目
- POST /api/v1/books/{id}/copies - 创建副本

**流通管理**
- POST /api/v1/circulation/borrow - 借阅
- POST /api/v1/circulation/return - 归还
- POST /api/v1/circulation/renew - 续借
- POST /api/v1/circulation/reservations - 预约

**读者管理**
- GET /api/v1/readers - 查询读者
- POST /api/v1/readers - 创建读者
- POST /api/v1/readers/{id}/face-features - 采集人脸

**推荐服务**
- GET /api/v1/recommend/homepage - 首页推荐
- GET /api/v1/recommend/similar - 相似推荐

**智能问答**
- POST /api/v1/nlp/qa - 智能问答

**数据分析**
- GET /api/v1/analytics/overview - 借阅概览
- GET /api/v1/analytics/borrow-trends - 借阅趋势

---

**文档结束**

*本API设计文档基于国创睿峰图书馆管理系统PRD编写,遵循RESTful规范和微服务架构最佳实践。*
