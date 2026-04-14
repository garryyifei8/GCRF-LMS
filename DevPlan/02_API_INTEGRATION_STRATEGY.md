# GCRF图书馆管理系统 - 统一API集成策略

**版本**: 1.0.0
**创建日期**: 2025-11-03
**策略目标**: 定义前后端API契约标准,实现统一集成规范
**参考文档**: `01_SYSTEM_STATUS_ANALYSIS.md`

---

## 📋 目录

1. [执行摘要](#执行摘要)
2. [OpenAPI 3.0规范定义](#openapi-30规范定义)
3. [API契约标准](#api契约标准)
4. [认证授权流程](#认证授权流程)
5. [错误处理规范](#错误处理规范)
6. [响应格式标准化](#响应格式标准化)
7. [API版本管理](#api版本管理)
8. [API网关路由配置](#api网关路由配置)
9. [Mock到真实API迁移计划](#mock到真实api迁移计划)
10. [实施路线图](#实施路线图)

---

## 📊 执行摘要

### 策略目标

1. **统一API规范**: 基于OpenAPI 3.0定义所有API契约
2. **前后端协作**: 通过API规范实现前后端并行开发
3. **自动化测试**: 基于API契约自动生成集成测试
4. **文档自动生成**: 自动生成Swagger UI交互式文档
5. **减少沟通成本**: API契约作为唯一真实来源 (Single Source of Truth)

### 关键收益

| 收益               | 量化指标 | 说明                        |
| ------------------ | -------- | --------------------------- |
| **开发效率提升**   | +40%     | 前后端并行开发,减少等待时间 |
| **API一致性**      | 100%     | 统一规范,消除歧义           |
| **集成测试自动化** | +80%     | 自动生成契约测试            |
| **文档维护成本**   | -60%     | 文档自动生成,始终最新       |
| **沟通成本**       | -50%     | API契约明确,减少会议        |

### 实施范围

- **微服务数量**: 7个 (Gateway, Auth, Book, Circulation, Reader, System, Notification)
- **API端点总数**: 128个 (38个已实现 + 90个待实现)
- **OpenAPI规范**: 7个YAML文件
- **Swagger UI**: 1个统一入口
- **预计工作量**: 1-2周

---

## 🎯 第一部分: OpenAPI 3.0规范定义

### 1.1 OpenAPI 3.0简介

**OpenAPI 3.0** (原名Swagger) 是REST API的行业标准规范。

**核心优势**:

- ✅ **机器可读**: 工具可自动解析
- ✅ **代码生成**: 自动生成客户端SDK
- ✅ **文档生成**: 自动生成Swagger UI
- ✅ **测试生成**: 自动生成契约测试
- ✅ **版本管理**: 支持API版本演进

**官方文档**: https://swagger.io/specification/

---

### 1.2 OpenAPI规范结构

#### 标准文件结构

```yaml
openapi: 3.0.3 # OpenAPI版本
info: # API元信息
  title: API名称
  version: 1.0.0
  description: API描述
  contact:
    name: GCRF Team
    email: support@gcrf.com
servers: # API服务器地址
  - url: http://localhost:8080
    description: 本地开发环境
  - url: https://api.gcrf.com
    description: 生产环境
paths: # API端点定义
  /api/v1/books:
    get: # HTTP方法
      summary: 查询图书列表
      tags: [图书管理]
      parameters: [...] # 请求参数
      responses: # 响应定义
        "200":
          description: 成功
          content:
            application/json:
              schema: { ... } # 响应Schema
components: # 可复用组件
  schemas: { ... } # 数据模型
  securitySchemes: { ... } # 认证方案
  responses: { ... } # 通用响应
  parameters: { ... } # 通用参数
security: # 全局安全配置
  - BearerAuth: []
```

---

### 1.3 GCRF项目OpenAPI规范文件结构

**目录结构**:

```
DevPlan/05_API_SPECIFICATIONS/
├── README.md                     # 规范使用说明
├── common/
│   ├── schemas.yaml              # 通用数据模型 (Result<T>, Page<T>)
│   ├── security.yaml             # 认证授权定义
│   ├── errors.yaml               # 错误响应定义
│   └── parameters.yaml           # 通用参数定义
├── gateway-api.yaml              # Gateway Service API
├── auth-api.yaml                 # Auth Service API
├── book-api.yaml                 # Book Service API
├── circulation-api.yaml          # Circulation Service API
├── reader-api.yaml               # Reader Service API
├── system-api.yaml               # System Service API
└── notification-api.yaml         # Notification Service API
```

**文件说明**:

- **common/**: 所有服务共享的定义 (通过`$ref`引用)
- **{service}-api.yaml**: 每个微服务独立的API规范
- **README.md**: 规范使用指南

---

### 1.4 通用数据模型定义 (common/schemas.yaml)

#### 统一响应格式 `Result<T>`

```yaml
# common/schemas.yaml
components:
  schemas:
    # 统一响应包装器
    Result:
      type: object
      required: [code, message, data, timestamp]
      properties:
        code:
          type: integer
          example: 200
          description: |
            业务状态码:
            - 200: 成功
            - 400: 参数错误
            - 401: 未认证
            - 403: 无权限
            - 404: 资源不存在
            - 500: 服务器错误
        message:
          type: string
          example: "success"
          description: 提示信息
        data:
          description: 业务数据 (具体类型由各API定义)
        timestamp:
          type: string
          format: date-time
          example: "2025-11-03T10:00:00Z"
          description: 响应时间戳

    # 分页响应
    PageResult:
      allOf:
        - $ref: "#/components/schemas/Result"
        - type: object
          properties:
            data:
              $ref: "#/components/schemas/PageData"

    PageData:
      type: object
      required: [records, total, pageNum, pageSize, pages]
      properties:
        records:
          type: array
          items: {}
          description: 当前页数据列表
        total:
          type: integer
          example: 100
          description: 总记录数
        pageNum:
          type: integer
          example: 1
          description: 当前页码 (从1开始)
        pageSize:
          type: integer
          example: 20
          description: 每页大小
        pages:
          type: integer
          example: 5
          description: 总页数
        hasNext:
          type: boolean
          example: true
          description: 是否有下一页
        hasPrevious:
          type: boolean
          example: false
          description: 是否有上一页

    # 错误响应
    ErrorResult:
      allOf:
        - $ref: "#/components/schemas/Result"
        - type: object
          properties:
            code:
              type: integer
              enum: [400, 401, 403, 404, 500]
            data:
              type: object
              nullable: true
              description: 错误详情 (可选)
              properties:
                field:
                  type: string
                  description: 错误字段 (参数校验错误时)
                reason:
                  type: string
                  description: 错误原因
                traceId:
                  type: string
                  description: 追踪ID (便于排查问题)
```

---

### 1.5 通用安全定义 (common/security.yaml)

```yaml
# common/security.yaml
components:
  securitySchemes:
    # JWT Bearer Token认证
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        JWT Token认证,格式: `Bearer {token}`

        获取Token:
        1. POST /api/v1/auth/login (用户名密码登录)
        2. 响应中获取 data.token
        3. 在后续请求Header中添加: `Authorization: Bearer {token}`

        Token有效期: 24小时
        刷新Token: POST /api/v1/auth/refresh

    # API Key认证 (可选,用于服务间调用)
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
      description: |
        API Key认证 (用于服务间调用)
        格式: `X-API-Key: {api_key}`

security:
  - BearerAuth: [] # 全局默认使用JWT认证
```

---

### 1.6 通用错误定义 (common/errors.yaml)

```yaml
# common/errors.yaml
components:
  responses:
    # 400 参数错误
    BadRequest:
      description: 请求参数错误
      content:
        application/json:
          schema:
            $ref: "schemas.yaml#/components/schemas/ErrorResult"
          example:
            code: 400
            message: "参数校验失败"
            data:
              field: "email"
              reason: "邮箱格式不正确"
            timestamp: "2025-11-03T10:00:00Z"

    # 401 未认证
    Unauthorized:
      description: 未认证或Token已过期
      content:
        application/json:
          schema:
            $ref: "schemas.yaml#/components/schemas/ErrorResult"
          example:
            code: 401
            message: "未登录或Token已过期"
            data:
              reason: "JWT token expired"
            timestamp: "2025-11-03T10:00:00Z"

    # 403 无权限
    Forbidden:
      description: 无权限访问
      content:
        application/json:
          schema:
            $ref: "schemas.yaml#/components/schemas/ErrorResult"
          example:
            code: 403
            message: "权限不足"
            data:
              reason: "需要 BOOK_DELETE 权限"
            timestamp: "2025-11-03T10:00:00Z"

    # 404 资源不存在
    NotFound:
      description: 资源不存在
      content:
        application/json:
          schema:
            $ref: "schemas.yaml#/components/schemas/ErrorResult"
          example:
            code: 404
            message: "资源不存在"
            data:
              reason: "图书ID 12345 不存在"
            timestamp: "2025-11-03T10:00:00Z"

    # 500 服务器错误
    InternalServerError:
      description: 服务器内部错误
      content:
        application/json:
          schema:
            $ref: "schemas.yaml#/components/schemas/ErrorResult"
          example:
            code: 500
            message: "服务器内部错误"
            data:
              traceId: "3c7e8f9a-1b2d-4e5f-8c9d-1a2b3c4d5e6f"
              reason: "数据库连接失败"
            timestamp: "2025-11-03T10:00:00Z"
```

---

### 1.7 通用参数定义 (common/parameters.yaml)

```yaml
# common/parameters.yaml
components:
  parameters:
    # 分页参数
    PageNum:
      name: pageNum
      in: query
      required: false
      schema:
        type: integer
        minimum: 1
        default: 1
      description: 页码 (从1开始)

    PageSize:
      name: pageSize
      in: query
      required: false
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
      description: 每页大小 (最大100)

    # 搜索参数
    Keyword:
      name: keyword
      in: query
      required: false
      schema:
        type: string
        maxLength: 100
      description: 搜索关键词

    # 排序参数
    SortField:
      name: sortField
      in: query
      required: false
      schema:
        type: string
        example: "createTime"
      description: 排序字段

    SortOrder:
      name: sortOrder
      in: query
      required: false
      schema:
        type: string
        enum: [asc, desc]
        default: desc
      description: 排序方向

    # 时间范围参数
    StartTime:
      name: startTime
      in: query
      required: false
      schema:
        type: string
        format: date-time
      description: 开始时间 (ISO 8601格式)

    EndTime:
      name: endTime
      in: query
      required: false
      schema:
        type: string
        format: date-time
      description: 结束时间 (ISO 8601格式)
```

---

### 1.8 示例: Book Service API规范 (book-api.yaml)

```yaml
# book-api.yaml
openapi: 3.0.3
info:
  title: GCRF图书管理服务 API
  version: 1.0.0
  description: |
    图书管理服务提供图书的增删改查、分类管理、库存管理等功能

    **认证方式**: JWT Bearer Token
    **API前缀**: `/api/v1/books`
  contact:
    name: GCRF Team
    email: dev@gcrf.com

servers:
  - url: http://localhost:8080
    description: 本地开发环境 (通过Gateway)
  - url: http://localhost:8082
    description: 本地开发环境 (直连服务)
  - url: https://api.gcrf.com
    description: 生产环境

tags:
  - name: 图书管理
    description: 图书CRUD操作
  - name: 分类管理
    description: 图书分类管理
  - name: 库存管理
    description: 图书库存管理

paths:
  # ==================== 图书管理 ====================
  /api/v1/books:
    get:
      summary: 分页查询图书列表
      tags: [图书管理]
      operationId: queryBooks
      parameters:
        - $ref: "common/parameters.yaml#/components/parameters/PageNum"
        - $ref: "common/parameters.yaml#/components/parameters/PageSize"
        - $ref: "common/parameters.yaml#/components/parameters/Keyword"
        - name: categoryId
          in: query
          required: false
          schema:
            type: integer
          description: 分类ID
        - name: status
          in: query
          required: false
          schema:
            type: string
            enum: [available, borrowed, reserved, maintenance]
          description: |
            图书状态:
            - available: 可借
            - borrowed: 已借出
            - reserved: 已预约
            - maintenance: 维护中
      responses:
        "200":
          description: 查询成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: "common/schemas.yaml#/components/schemas/PageResult"
                  - type: object
                    properties:
                      data:
                        type: object
                        properties:
                          records:
                            type: array
                            items:
                              $ref: "#/components/schemas/BookVO"
              example:
                code: 200
                message: "success"
                data:
                  records:
                    - id: 1
                      isbn: "978-7-111-42252-1"
                      title: "深入理解计算机系统"
                      author: "Randal E. Bryant"
                      publisher: "机械工业出版社"
                      publishYear: 2015
                      category: "计算机科学"
                      totalStock: 10
                      availableStock: 3
                      status: "available"
                      coverUrl: "https://img.gcrf.com/books/covers/1.jpg"
                      createTime: "2025-01-01T08:00:00Z"
                  total: 100
                  pageNum: 1
                  pageSize: 20
                  pages: 5
                  hasNext: true
                  hasPrevious: false
                timestamp: "2025-11-03T10:00:00Z"
        "400":
          $ref: "common/errors.yaml#/components/responses/BadRequest"
        "401":
          $ref: "common/errors.yaml#/components/responses/Unauthorized"
        "500":
          $ref: "common/errors.yaml#/components/responses/InternalServerError"

    post:
      summary: 创建图书
      tags: [图书管理]
      operationId: createBook
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/BookCreateRequest"
            example:
              isbn: "978-7-115-42803-3"
              title: "设计模式:可复用面向对象软件的基础"
              author: "Erich Gamma, Richard Helm"
              publisher: "人民邮电出版社"
              publishYear: 2018
              categoryId: 1
              totalStock: 5
              price: 59.00
              description: "经典设计模式书籍"
      responses:
        "200":
          description: 创建成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: "common/schemas.yaml#/components/schemas/Result"
                  - type: object
                    properties:
                      data:
                        $ref: "#/components/schemas/BookDetailVO"
        "400":
          $ref: "common/errors.yaml#/components/responses/BadRequest"
        "401":
          $ref: "common/errors.yaml#/components/responses/Unauthorized"
        "403":
          $ref: "common/errors.yaml#/components/responses/Forbidden"

  /api/v1/books/{id}:
    get:
      summary: 查询图书详情
      tags: [图书管理]
      operationId: getBook
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
          description: 图书ID
      responses:
        "200":
          description: 查询成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: "common/schemas.yaml#/components/schemas/Result"
                  - type: object
                    properties:
                      data:
                        $ref: "#/components/schemas/BookDetailVO"
        "404":
          $ref: "common/errors.yaml#/components/responses/NotFound"

    put:
      summary: 更新图书
      tags: [图书管理]
      operationId: updateBook
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/BookUpdateRequest"
      responses:
        "200":
          description: 更新成功
        "400":
          $ref: "common/errors.yaml#/components/responses/BadRequest"
        "404":
          $ref: "common/errors.yaml#/components/responses/NotFound"

    delete:
      summary: 删除图书
      tags: [图书管理]
      operationId: deleteBook
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        "200":
          description: 删除成功
        "404":
          $ref: "common/errors.yaml#/components/responses/NotFound"

# ==================== 数据模型 ====================
components:
  schemas:
    # 图书VO (列表展示)
    BookVO:
      type: object
      required:
        [id, isbn, title, author, publisher, totalStock, availableStock, status]
      properties:
        id:
          type: integer
          example: 1
        isbn:
          type: string
          example: "978-7-111-42252-1"
        title:
          type: string
          example: "深入理解计算机系统"
        author:
          type: string
          example: "Randal E. Bryant"
        publisher:
          type: string
          example: "机械工业出版社"
        publishYear:
          type: integer
          example: 2015
        category:
          type: string
          example: "计算机科学"
        totalStock:
          type: integer
          example: 10
        availableStock:
          type: integer
          example: 3
        status:
          type: string
          enum: [available, borrowed, reserved, maintenance]
        coverUrl:
          type: string
          format: uri
          nullable: true
        createTime:
          type: string
          format: date-time

    # 图书详情VO
    BookDetailVO:
      allOf:
        - $ref: "#/components/schemas/BookVO"
        - type: object
          properties:
            categoryId:
              type: integer
            description:
              type: string
              nullable: true
            price:
              type: number
              format: double
              example: 89.00
            location:
              type: string
              example: "A-01-03-12"
              description: 图书馆位置编码
            updateTime:
              type: string
              format: date-time

    # 创建图书请求
    BookCreateRequest:
      type: object
      required: [isbn, title, author, publisher, categoryId, totalStock]
      properties:
        isbn:
          type: string
          pattern: '^978-\d{1,5}-\d{1,7}-\d{1,7}-\d{1}$'
          example: "978-7-115-42803-3"
        title:
          type: string
          minLength: 1
          maxLength: 200
        author:
          type: string
          minLength: 1
          maxLength: 100
        publisher:
          type: string
          minLength: 1
          maxLength: 100
        publishYear:
          type: integer
          minimum: 1900
          maximum: 2100
        categoryId:
          type: integer
        totalStock:
          type: integer
          minimum: 1
        price:
          type: number
          format: double
          minimum: 0
        description:
          type: string
          maxLength: 1000
          nullable: true

    # 更新图书请求
    BookUpdateRequest:
      type: object
      properties:
        title:
          type: string
          minLength: 1
          maxLength: 200
        author:
          type: string
        publisher:
          type: string
        publishYear:
          type: integer
        categoryId:
          type: integer
        description:
          type: string
        price:
          type: number

  securitySchemes:
    BearerAuth:
      $ref: "common/security.yaml#/components/securitySchemes/BearerAuth"

security:
  - BearerAuth: []
```

**文件位置**: `DevPlan/05_API_SPECIFICATIONS/book-api.yaml`

---

## 🔐 第二部分: API契约标准

### 2.1 契约驱动开发 (Contract-Driven Development)

**核心原则**:

1. **API优先** (API First): 先设计API契约,再实现
2. **契约即文档**: OpenAPI规范作为唯一真实来源
3. **并行开发**: 前后端基于契约并行开发
4. **自动化测试**: 基于契约自动生成测试

**开发流程**:

```
1. 需求分析 → 2. API设计 (OpenAPI) → 3. 契约评审 → 4. 并行开发
   ↓                                                          ↓
   前端: Mock Server基于OpenAPI                          后端: 实现API
   ↓                                                          ↓
5. 契约测试 → 6. 集成测试 → 7. 部署上线
```

---

### 2.2 前后端契约约定

#### 通用约定

| 约定项       | 标准                       | 说明                               |
| ------------ | -------------------------- | ---------------------------------- |
| **协议**     | HTTPS (生产) / HTTP (开发) | 生产环境强制HTTPS                  |
| **格式**     | JSON                       | `Content-Type: application/json`   |
| **编码**     | UTF-8                      | 中文正常显示                       |
| **时区**     | UTC+0                      | 统一使用UTC时间,前端转换为本地时区 |
| **日期格式** | ISO 8601                   | `2025-11-03T10:00:00Z`             |
| **分页起始** | 1                          | 页码从1开始 (不是0)                |
| **ID类型**   | Long                       | 64位整数                           |
| **布尔值**   | true/false                 | 小写 (不是True/False)              |
| **空值**     | null                       | 不返回字段或返回null               |

---

### 2.3 请求规范

#### HTTP方法语义

| 方法       | 语义     | 幂等性    | 请求体 | 说明                    |
| ---------- | -------- | --------- | ------ | ----------------------- |
| **GET**    | 查询资源 | ✅ 幂等   | ❌ 无  | 用于查询,不修改数据     |
| **POST**   | 创建资源 | ❌ 非幂等 | ✅ 有  | 创建新资源,返回新ID     |
| **PUT**    | 完整更新 | ✅ 幂等   | ✅ 有  | 完整替换资源            |
| **PATCH**  | 部分更新 | ❌ 非幂等 | ✅ 有  | 部分更新字段 (暂不使用) |
| **DELETE** | 删除资源 | ✅ 幂等   | ❌ 无  | 逻辑删除 (软删除)       |

#### URL路径规范

**规则**:

1. 使用小写字母
2. 单词间用连字符 `-` (不是下划线 `_`)
3. 资源名用复数形式
4. 层级不超过3层

**示例**:

```
✅ 正确:
GET  /api/v1/books                  # 查询图书列表
GET  /api/v1/books/123              # 查询图书详情
POST /api/v1/books                  # 创建图书
PUT  /api/v1/books/123              # 更新图书
DELETE /api/v1/books/123            # 删除图书
GET  /api/v1/circulation/records    # 查询借阅记录

❌ 错误:
GET  /api/v1/book                   # 单数形式
GET  /api/v1/Books                  # 大写字母
GET  /api/v1/circulation_records    # 下划线
GET  /api/v1/system/roles/1/users/2/permissions  # 层级过深
```

#### 请求参数规范

**Query参数** (GET请求):

- 用于查询条件、分页、排序
- 参数名使用camelCase (驼峰命名)
- 可选参数提供默认值

**Path参数** (路径参数):

- 用于标识特定资源
- 必填参数
- 使用`{id}`占位符

**Body参数** (POST/PUT请求):

- JSON格式
- 使用camelCase命名
- 必填字段标注`@NotNull`, `@NotBlank`

**示例**:

```http
# Query参数
GET /api/v1/books?pageNum=1&pageSize=20&keyword=设计模式&categoryId=1

# Path参数
GET /api/v1/books/{id}
PUT /api/v1/books/{id}

# Body参数
POST /api/v1/books
Content-Type: application/json

{
  "isbn": "978-7-115-42803-3",
  "title": "设计模式",
  "author": "Erich Gamma",
  "categoryId": 1,
  "totalStock": 5
}
```

---

### 2.4 响应规范

#### 统一响应格式

**成功响应** (200):

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },              // 业务数据
  "timestamp": "2025-11-03T10:00:00Z"
}
```

**分页响应**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],           // 当前页数据
    "total": 100,               // 总记录数
    "pageNum": 1,               // 当前页码
    "pageSize": 20,             // 每页大小
    "pages": 5,                 // 总页数
    "hasNext": true,            // 是否有下一页
    "hasPrevious": false        // 是否有上一页
  },
  "timestamp": "2025-11-03T10:00:00Z"
}
```

**错误响应** (400/401/403/404/500):

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": {
    "field": "email",
    "reason": "邮箱格式不正确"
  },
  "timestamp": "2025-11-03T10:00:00Z"
}
```

#### HTTP状态码使用

| 状态码                        | 说明       | 何时使用                          |
| ----------------------------- | ---------- | --------------------------------- |
| **200 OK**                    | 成功       | 所有成功请求 (包括创建/更新/删除) |
| **400 Bad Request**           | 参数错误   | 参数校验失败,业务规则校验失败     |
| **401 Unauthorized**          | 未认证     | Token缺失/无效/过期               |
| **403 Forbidden**             | 无权限     | 无操作权限                        |
| **404 Not Found**             | 资源不存在 | 查询/更新/删除的资源不存在        |
| **500 Internal Server Error** | 服务器错误 | 数据库异常,NPE等未捕获异常        |

**注意**: GCRF项目统一使用HTTP 200 + 业务code区分成功/失败,但仍建议遵循RESTful规范使用正确的HTTP状态码。

---

## 🔐 第三部分: 认证授权流程

### 3.1 JWT认证流程

#### 认证流程图

```
┌──────────┐                  ┌─────────────┐                  ┌──────────────┐
│  前端    │                  │  Gateway    │                  │ Auth Service │
│ (Vue 3)  │                  │  (8080)     │                  │   (8081)     │
└────┬─────┘                  └──────┬──────┘                  └──────┬───────┘
     │                               │                                │
     │ 1. POST /api/v1/auth/login   │                                │
     │ {username, password}          │                                │
     ├──────────────────────────────>│  2. 转发请求                   │
     │                               ├───────────────────────────────>│
     │                               │                                │
     │                               │            3. 验证用户名密码      │
     │                               │            4. 生成JWT Token     │
     │                               │            5. 存入Redis         │
     │                               │                                │
     │                               │  6. 返回Token                  │
     │  7. 返回响应                   │<───────────────────────────────│
     │<──────────────────────────────│                                │
     │ {code:200, data:{token}}      │                                │
     │                               │                                │
     │ 8. 保存Token到localStorage    │                                │
     │                               │                                │
     │ 9. GET /api/v1/books         │                                │
     │ Header: Authorization:        │                                │
     │         Bearer {token}        │                                │
     ├──────────────────────────────>│  10. 验证Token                 │
     │                               │      (从Redis查询)              │
     │                               │                                │
     │                               │  11. 转发请求                   │
     │                               ├───────────────────────────────>│
     │                               │              (Book Service)    │
     │                               │                                │
     │  12. 返回数据                  │<───────────────────────────────│
     │<──────────────────────────────│                                │
     │                               │                                │
     │ 13. Token过期 (24小时后)       │                                │
     │ POST /api/v1/auth/refresh     │                                │
     │ Header: Authorization:        │                                │
     │         Bearer {expired_token}│                                │
     ├──────────────────────────────>│  14. 刷新Token                 │
     │                               ├───────────────────────────────>│
     │                               │                                │
     │                               │  15. 生成新Token                │
     │  16. 返回新Token               │<───────────────────────────────│
     │<──────────────────────────────│                                │
     │                               │                                │
```

---

### 3.2 JWT Token结构

#### Token格式

```
Header.Payload.Signature
```

**示例**:

```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwi...
```

#### Header (头部)

```json
{
  "alg": "HS512", // 签名算法
  "typ": "JWT" // Token类型
}
```

#### Payload (载荷)

```json
{
  "sub": "admin", // 主题 (用户名)
  "userId": 1, // 用户ID
  "userType": "ADMIN", // 用户类型
  "iat": 1699000000, // 签发时间 (Issued At)
  "exp": 1699086400 // 过期时间 (Expiration)
}
```

#### Signature (签名)

```
HMACSHA512(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret_key  // 密钥 (保存在配置中心Nacos)
)
```

---

### 3.3 前端Token管理

#### Token存储

**推荐方式**: `localStorage`

- ✅ 跨标签页共享
- ✅ 刷新页面不丢失
- ⚠️ XSS攻击风险 (需防范)

**备选方式**: `sessionStorage`

- ✅ 关闭标签页自动清除
- ❌ 不跨标签页共享

**不推荐**: `Cookie`

- ⚠️ CSRF攻击风险

#### Axios拦截器配置

```javascript
// src/utils/request.js
import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
});

// 请求拦截器: 添加Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// 响应拦截器: 处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data;

    // 成功响应
    if (res.code === 200) {
      return res;
    }

    // 业务错误
    ElMessage.error(res.message || "请求失败");
    return Promise.reject(new Error(res.message || "请求失败"));
  },
  (error) => {
    // 401: Token过期或无效
    if (error.response?.status === 401) {
      ElMessage.error("登录已过期,请重新登录");
      localStorage.removeItem("access_token");
      router.push("/login");
      return Promise.reject(error);
    }

    // 403: 无权限
    if (error.response?.status === 403) {
      ElMessage.error("权限不足");
      return Promise.reject(error);
    }

    // 其他错误
    ElMessage.error(error.response?.data?.message || "网络错误");
    return Promise.reject(error);
  },
);

export default request;
```

---

### 3.4 后端Token验证

#### Gateway全局过滤器

```java
// gateway-service/src/main/java/com/gcrf/library/gateway/filter/AuthFilter.java
@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 白名单: 不需要认证的路径
    private static final List<String> WHITELIST = Arrays.asList(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径直接放行
        if (WHITELIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少Token");
        }

        token = token.substring(7);  // 去除 "Bearer "

        // 验证Token
        try {
            // 1. 验证Token签名和过期时间
            Claims claims = jwtUtils.parseToken(token);

            // 2. 检查Redis中是否存在 (防止已登出的Token被使用)
            String key = "token:" + claims.get("userId");
            String redisToken = redisTemplate.opsForValue().get(key);
            if (redisToken == null || !redisToken.equals(token)) {
                return unauthorized(exchange, "Token已失效");
            }

            // 3. 将用户信息传递给下游服务
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", claims.get("userId").toString())
                .header("X-Username", claims.getSubject())
                .header("X-User-Type", claims.get("userType").toString())
                .build();

            return chain.filter(exchange.mutate().request(request).build());

        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, "Token已过期");
        } catch (JwtException e) {
            return unauthorized(exchange, "Token无效");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
            message,
            LocalDateTime.now()
        );

        DataBuffer buffer = exchange.getResponse()
            .bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;  // 优先级高,先执行
    }
}
```

---

## 🚨 第四部分: 错误处理规范

### 4.1 错误码体系

#### 错误码分类

| 范围        | 类别       | 说明                | 示例               |
| ----------- | ---------- | ------------------- | ------------------ |
| **200**     | 成功       | 请求成功            | 200                |
| **400-499** | 客户端错误 | 参数错误,权限不足等 | 400, 401, 403, 404 |
| **500-599** | 服务端错误 | 服务器内部错误      | 500, 503           |

#### 详细错误码定义

| Code    | 说明       | HTTP状态码 | 前端处理           |
| ------- | ---------- | ---------- | ------------------ |
| **200** | 成功       | 200        | 正常处理           |
| **400** | 参数错误   | 400        | 显示错误提示       |
| **401** | 未认证     | 401        | 跳转登录页         |
| **403** | 无权限     | 403        | 显示权限不足提示   |
| **404** | 资源不存在 | 404        | 显示资源不存在提示 |
| **500** | 服务器错误 | 500        | 显示系统错误提示   |
| **503** | 服务不可用 | 503        | 显示服务维护提示   |

---

### 4.2 后端异常处理

#### 全局异常处理器

```java
// common-web/src/main/java/com/gcrf/library/common/web/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.ok(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidationException(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldError().getField();
        String message = e.getBindingResult().getFieldError().getDefaultMessage();

        log.warn("参数校验失败: field={}, message={}", field, message);

        Map<String, String> data = new HashMap<>();
        data.put("field", field);
        data.put("reason", message);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(400, "参数校验失败", data));
    }

    /**
     * 资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Object>> handleNotFoundException(ResourceNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Result.error(404, e.getMessage()));
    }

    /**
     * 未认证异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Object>> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("未认证: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Result.error(401, e.getMessage()));
    }

    /**
     * 无权限异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<Object>> handleForbiddenException(ForbiddenException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Result.error(403, e.getMessage()));
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Object>> handleSystemException(SystemException e) {
        log.error("系统异常: ", e);

        String traceId = MDC.get("traceId");  // 从日志上下文获取追踪ID
        Map<String, String> data = new HashMap<>();
        data.put("traceId", traceId);
        data.put("reason", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "服务器内部错误", data));
    }

    /**
     * 未捕获异常 (兜底)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        log.error("未捕获异常: ", e);

        String traceId = MDC.get("traceId");
        Map<String, String> data = new HashMap<>();
        data.put("traceId", traceId);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "服务器内部错误", data));
    }
}
```

---

### 4.3 前端错误处理

#### Axios响应拦截器 (详细版)

```javascript
// src/utils/request.js
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router";

request.interceptors.response.use(
  (response) => {
    const res = response.data;

    // 成功响应 (code === 200)
    if (res.code === 200) {
      return res;
    }

    // 业务错误 (code !== 200)
    handleBusinessError(res);
    return Promise.reject(new Error(res.message || "请求失败"));
  },
  (error) => {
    // HTTP错误
    handleHttpError(error);
    return Promise.reject(error);
  },
);

// 处理业务错误 (code !== 200)
function handleBusinessError(res) {
  switch (res.code) {
    case 400:
      // 参数错误: 显示具体字段错误
      if (res.data && res.data.field) {
        ElMessage.error(`${res.data.field}: ${res.data.reason}`);
      } else {
        ElMessage.error(res.message || "参数错误");
      }
      break;

    case 401:
      // 未认证: 跳转登录页
      ElMessageBox.confirm("登录已过期,请重新登录", "提示", {
        confirmButtonText: "重新登录",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        localStorage.removeItem("access_token");
        router.push("/login");
      });
      break;

    case 403:
      // 无权限: 显示权限提示
      ElMessage.error("权限不足,无法执行该操作");
      break;

    case 404:
      // 资源不存在
      ElMessage.error(res.message || "资源不存在");
      break;

    case 500:
      // 服务器错误: 显示追踪ID
      let message = "服务器内部错误";
      if (res.data && res.data.traceId) {
        message += ` (追踪ID: ${res.data.traceId})`;
      }
      ElMessage.error(message);
      break;

    default:
      ElMessage.error(res.message || "未知错误");
  }
}

// 处理HTTP错误
function handleHttpError(error) {
  if (!error.response) {
    ElMessage.error("网络错误,请检查网络连接");
    return;
  }

  const status = error.response.status;
  const data = error.response.data;

  switch (status) {
    case 401:
      ElMessage.error("未登录或登录已过期");
      localStorage.removeItem("access_token");
      router.push("/login");
      break;

    case 403:
      ElMessage.error("权限不足");
      break;

    case 404:
      ElMessage.error("请求的资源不存在");
      break;

    case 500:
    case 503:
      ElMessage.error(data?.message || "服务器错误");
      break;

    default:
      ElMessage.error(data?.message || `请求失败 (${status})`);
  }
}
```

---

## 📦 第五部分: 响应格式标准化

### 5.1 统一响应包装器

#### Result<T> 类定义

```java
// common-core/src/main/java/com/gcrf/library/common/result/Result.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // 成功响应 (无数据)
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null, LocalDateTime.now());
    }

    // 成功响应 (有数据)
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, LocalDateTime.now());
    }

    // 成功响应 (自定义消息)
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, LocalDateTime.now());
    }

    // 错误响应 (默认500)
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, LocalDateTime.now());
    }

    // 错误响应 (指定code)
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now());
    }

    // 错误响应 (带数据)
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data, LocalDateTime.now());
    }
}
```

---

### 5.2 分页响应包装器

#### PageResult<T> 类定义

```java
// common-core/src/main/java/com/gcrf/library/common/result/PageResult.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;       // 当前页数据
    private Long total;            // 总记录数
    private Integer pageNum;       // 当前页码
    private Integer pageSize;      // 每页大小
    private Integer pages;         // 总页数
    private Boolean hasNext;       // 是否有下一页
    private Boolean hasPrevious;   // 是否有上一页

    // 从MyBatis Plus的Page对象转换
    public static <T> PageResult<T> from(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        result.setPages((int) page.getPages());
        result.setHasNext(page.getCurrent() < page.getPages());
        result.setHasPrevious(page.getCurrent() > 1);
        return result;
    }

    // 空分页结果
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0L);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(0);
        result.setHasNext(false);
        result.setHasPrevious(false);
        return result;
    }
}
```

---

## 🔄 第六部分: API版本管理

### 6.1 版本策略

**GCRF项目版本策略**: URL路径版本控制

**格式**: `/api/v{major}/...`

**示例**:

- v1: `/api/v1/books`
- v2: `/api/v2/books` (未来)

**版本升级规则**:

1. **向后兼容**: 不升级版本 (v1保持)
2. **破坏性变更**: 升级版本 (v1→v2)
3. **v1和v2共存**: 允许,逐步迁移

**破坏性变更示例**:

- 修改响应字段名
- 删除API端点
- 修改请求参数必填性

---

## 🌐 第七部分: API网关路由配置

### 7.1 Gateway路由规则

**配置文件**: `gateway-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ==================== Auth Service ====================
        - id: auth-service
          uri: lb://auth-service # 负载均衡,从Nacos获取实例
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0 # 不去除前缀

        # ==================== Book Service ====================
        - id: book-service
          uri: lb://book-service
          predicates:
            - Path=/api/v1/books/**
          filters:
            - StripPrefix=0

        # ==================== Circulation Service ====================
        - id: circulation-service
          uri: lb://circulation-service
          predicates:
            - Path=/api/v1/circulation/**
          filters:
            - StripPrefix=0

        # ==================== Reader Service ====================
        - id: reader-service
          uri: lb://reader-service
          predicates:
            - Path=/api/v1/readers/**
          filters:
            - StripPrefix=0

        # ==================== System Service ====================
        - id: system-service
          uri: lb://system-service
          predicates:
            - Path=/api/v1/system/**
          filters:
            - StripPrefix=0

        # ==================== Notification Service ====================
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - StripPrefix=0

      # 全局CORS配置
      globalcors:
        cors-configurations:
          "[/**]":
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: false
```

---

### 7.2 前端API Base URL配置

**环境配置文件**: `web-admin/.env.development`

```bash
# 开发环境
VITE_API_BASE_URL=http://localhost:8080

# 是否使用Mock API
VITE_USE_MOCK=false
```

**生产环境**: `web-admin/.env.production`

```bash
# 生产环境
VITE_API_BASE_URL=https://api.gcrf.com

# 关闭Mock
VITE_USE_MOCK=false
```

**Axios配置**: `web-admin/src/utils/request.js`

```javascript
import axios from "axios";

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

export default request;
```

---

## 🔀 第八部分: Mock到真实API迁移计划

### 8.1 当前Mock API清单

| 模块            | Mock文件                  | Mock端点数 | 真实API完成度  |
| --------------- | ------------------------- | ---------- | -------------- |
| **Auth**        | `handlers/auth.js`        | 11个       | ✅ 100%        |
| **Books**       | `handlers/books.js`       | 8个        | ⏳ 87% (7/8)   |
| **Circulation** | `handlers/circulation.js` | 12个       | ⏳ 41% (5/12)  |
| **Readers**     | `handlers/readers.js`     | 10个       | ✅ 100%        |
| **System**      | `handlers/system.js`      | 15个       | ⏳ 33% (5/15)  |
| **AI**          | `handlers/ai.js`          | 5个        | ❌ 0%          |
| **Profile**     | `handlers/profile.js`     | 3个        | ❌ 0%          |
| **总计**        | 7个文件                   | 64个       | ⏳ 58% (37/64) |

---

### 8.2 迁移策略

#### 迁移原则

1. **逐模块迁移**: 一次迁移一个模块 (不是一个API)
2. **后端优先**: 先完成后端API,再关闭Mock
3. **保留Mock代码**: 迁移后不删除Mock代码,便于回滚
4. **灰度切换**: 通过环境变量控制是否使用Mock

#### 迁移步骤

**Step 1: 后端API开发完成**

- 完成对应模块的所有API端点
- 单元测试覆盖80%+
- 集成测试通过

**Step 2: 前端适配**

- 对比Mock API和真实API响应格式
- 修改前端代码 (如有必要)
- 更新API调用参数

**Step 3: 联调测试**

- 启动后端服务
- 关闭Mock (设置`VITE_USE_MOCK=false`)
- 端到端测试核心流程

**Step 4: 问题修复**

- 记录联调问题
- 修复前后端Bug
- 重新测试

**Step 5: 上线**

- 代码Review
- 合并到develop分支
- 部署到测试环境

---

### 8.3 迁移优先级

| 优先级           | 模块        | 迁移时机   | 预计工作量 |
| ---------------- | ----------- | ---------- | ---------- |
| **P0 - 立即**    | Auth        | ✅ 已完成  | -          |
| **P0 - 第1周**   | Books       | 后端完成后 | 2天        |
| **P0 - 第2周**   | Readers     | 后端完成后 | 2天        |
| **P0 - 第3周**   | Circulation | 后端完成后 | 3天        |
| **P1 - 第4-5周** | System      | 后端完成后 | 3天        |
| **P1 - 第5周**   | Profile     | 后端完成后 | 1天        |
| **P2 - 待定**    | AI          | 后端完成后 | 2天        |

---

### 8.4 Mock与真实API切换机制

#### MSW配置 (web-admin/src/main.js)

```javascript
import { createApp } from "vue";
import App from "./App.vue";

const app = createApp(App);

// 开发环境且启用Mock
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === "true") {
  import("./mock/browser").then(({ worker }) => {
    worker
      .start({
        onUnhandledRequest: "bypass", // 未匹配的请求放行 (打到真实后端)
      })
      .then(() => {
        console.log("[MSW] Mock Service Worker started");
        app.mount("#app");
      });
  });
} else {
  app.mount("#app");
}
```

**切换步骤**:

1. 修改`.env.development`: `VITE_USE_MOCK=false`
2. 重启Vite Dev Server: `npm run dev`
3. 所有API请求将打到真实后端 (http://localhost:8080)

---

## 📋 第九部分: OpenAPI规范生成工具

### 9.1 SpringDoc OpenAPI (推荐)

**Maven依赖**:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

**配置**:

```yaml
# application.yml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  info:
    title: GCRF Book Service API
    version: 1.0.0
    description: 图书管理服务API文档
```

**访问地址**:

- OpenAPI JSON: `http://localhost:8082/v3/api-docs`
- Swagger UI: `http://localhost:8082/swagger-ui.html`

---

### 9.2 注解示例

```java
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "图书管理", description = "图书的增删改查、搜索等接口")
public class BookController {

    @GetMapping
    @Operation(summary = "分页查询图书", description = "支持关键词搜索、分类筛选等")
    @Parameter(name = "pageNum", description = "页码", example = "1")
    @Parameter(name = "keyword", description = "搜索关键词", example = "设计模式")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    public Result<PageResult<BookVO>> queryBooks(
        @Valid BookQueryRequest request
    ) {
        // ...
    }
}
```

---

## 🚀 第十部分: 实施路线图

### 10.1 Phase 1: OpenAPI规范创建 (Week 1)

**任务清单**:

- [ ] 创建`DevPlan/05_API_SPECIFICATIONS/`目录结构
- [ ] 创建通用数据模型 (`common/schemas.yaml`)
- [ ] 创建通用安全定义 (`common/security.yaml`)
- [ ] 创建通用错误定义 (`common/errors.yaml`)
- [ ] 创建通用参数定义 (`common/parameters.yaml`)
- [ ] 创建7个服务的OpenAPI规范YAML文件:
  - [ ] `gateway-api.yaml` (路由配置文档)
  - [ ] `auth-api.yaml` (认证授权API)
  - [ ] `book-api.yaml` (图书管理API)
  - [ ] `circulation-api.yaml` (流通管理API)
  - [ ] `reader-api.yaml` (读者管理API)
  - [ ] `system-api.yaml` (系统管理API)
  - [ ] `notification-api.yaml` (通知服务API)
- [ ] 创建`README.md` (规范使用说明)

**交付物**:

- 7个OpenAPI 3.0 YAML文件
- 4个通用定义文件
- 1个README文档

**预计工作量**: 3-5天

---

### 10.2 Phase 2: Swagger UI集成 (Week 1-2)

**任务清单**:

- [ ] 为7个微服务添加SpringDoc OpenAPI依赖
- [ ] 配置Swagger UI (每个服务独立访问)
- [ ] 配置Gateway聚合Swagger (统一入口)
- [ ] 验证OpenAPI规范与实际API一致性

**Swagger访问地址**:

- Gateway: `http://localhost:8080/swagger-ui.html`
- Auth: `http://localhost:8081/swagger-ui.html`
- Book: `http://localhost:8082/swagger-ui.html`
- Circulation: `http://localhost:8083/swagger-ui.html`
- Reader: `http://localhost:8084/swagger-ui.html`
- System: `http://localhost:8085/swagger-ui.html`
- Notification: `http://localhost:8086/swagger-ui.html`

**预计工作量**: 2天

---

### 10.3 Phase 3: Mock API迁移 (Week 2-5)

**迁移顺序**:

**Week 2**: Auth + Books + Readers (已完成服务)

- [ ] 关闭Auth Mock → 连接真实后端
- [ ] 完成Book Service剩余API (8个 → 15个)
- [ ] 关闭Books Mock → 连接真实后端
- [ ] 完成Reader Service剩余API (10个 → 20个)
- [ ] 关闭Readers Mock → 连接真实后端
- [ ] 端到端测试: 登录 → 查询图书 → 查询读者

**Week 3**: Circulation (核心流程)

- [ ] 完成Circulation Service剩余API (5个 → 17个)
- [ ] 关闭Circulation Mock → 连接真实后端
- [ ] 端到端测试: 借阅 → 归还 → 续借

**Week 4-5**: System (管理功能)

- [ ] 完成System Service剩余API (5个 → 40个)
- [ ] 关闭System Mock → 连接真实后端
- [ ] 端到端测试: 用户管理 → 角色管理 → 权限管理

**预计工作量**: 3-4周

---

### 10.4 Phase 4: API契约测试 (Week 5-6)

**任务清单**:

- [ ] 集成Pact / Spring Cloud Contract
- [ ] 为128个API端点生成契约测试
- [ ] 集成到CI/CD Pipeline
- [ ] 自动化执行契约测试

**测试覆盖**:

- Auth Service: 11个API
- Book Service: 15个API
- Circulation Service: 17个API
- Reader Service: 20个API
- System Service: 40个API
- Notification Service: 25个API

**预计工作量**: 1-2周

---

### 10.5 关键里程碑

| 里程碑                   | 时间   | 标志                                       |
| ------------------------ | ------ | ------------------------------------------ |
| **M1: OpenAPI规范完成**  | Week 1 | 7个YAML文件创建完成                        |
| **M2: Swagger UI上线**   | Week 2 | 统一文档入口可访问                         |
| **M3: 核心模块集成完成** | Week 3 | Auth+Books+Readers+Circulation连接真实后端 |
| **M4: 管理模块集成完成** | Week 5 | System模块连接真实后端                     |
| **M5: 契约测试上线**     | Week 6 | 128个API契约测试自动化                     |
| **M6: 生产环境部署**     | Week 8 | 完整系统上线                               |

---

## 📝 附录: 参考资源

### OpenAPI 3.0官方资源

- **OpenAPI规范**: https://swagger.io/specification/
- **Swagger Editor**: https://editor.swagger.io/ (在线编辑器)
- **Swagger UI**: https://swagger.io/tools/swagger-ui/
- **SpringDoc OpenAPI**: https://springdoc.org/

### GCRF项目相关文档

- **系统现状分析**: `DevPlan/01_SYSTEM_STATUS_ANALYSIS.md`
- **技术架构文档**: `backend/doc/architect.md`
- **项目状态总结**: `PROJECT_STATUS_SUMMARY.md`
- **快速开始指南**: `QUICKSTART.md`

---

## 🎯 总结

### 核心策略

1. **OpenAPI 3.0为核心**: 定义所有API契约
2. **前后端契约驱动**: 并行开发,减少等待
3. **统一响应格式**: `Result<T>` + `PageResult<T>`
4. **JWT认证流程**: Bearer Token + Redis存储
5. **错误处理标准化**: 统一错误码 + 详细错误信息
6. **逐模块迁移**: Mock → 真实API,降低风险

### 预期收益

- ✅ **开发效率**: 提升40%
- ✅ **API一致性**: 100%
- ✅ **集成测试**: 自动化80%
- ✅ **文档维护**: 成本降低60%
- ✅ **沟通成本**: 降低50%

### 下一步

1. **立即创建**: OpenAPI规范文件 (Week 1)
2. **集成Swagger UI**: 统一文档入口 (Week 1-2)
3. **Mock迁移**: 逐模块连接真实后端 (Week 2-5)
4. **契约测试**: 自动化API测试 (Week 5-6)

---

**创建人**: Claude Code Agent
**创建日期**: 2025-11-03
**下一步**: 创建集成测试计划 (`03_INTEGRATION_TESTING_PLAN.md`)
