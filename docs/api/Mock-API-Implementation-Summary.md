# Mock API 实施总结

## 实施概述

根据Mock-API-Strategy.md中的方案，已完成Mock API基础设施的实施。该实现使用MSW (Mock Service Worker) 来拦截浏览器级别的HTTP请求，为前端开发提供完整的Mock数据支持。

## 已完成的工作

### 1. 依赖安装
- ✅ 安装 `msw` - Mock Service Worker核心库
- ✅ 安装 `@faker-js/faker` - 生成真实感的中文Mock数据

### 2. MSW初始化
- ✅ 初始化MSW service worker到 `public/` 目录
- ✅ 创建 `mockServiceWorker.js` 文件
- ✅ 更新 `package.json` 配置MSW worker目录

### 3. Mock目录结构
```
web-admin/
├── mock/
│   ├── data/                    # Mock数据模块
│   │   ├── factory.ts           # 数据工厂函数
│   │   ├── departments.ts       # 部门模拟数据（30条）
│   │   ├── books.ts             # 图书模拟数据（100条）
│   │   └── auth.ts              # 认证相关数据
│   └── msw/                     # MSW配置
│       ├── handlers/            # API处理器
│       │   ├── auth.ts          # 认证API handlers
│       │   └── system.ts        # 系统管理API handlers
│       └── browser.ts           # MSW浏览器配置
```

### 4. 数据工厂函数 (`mock/data/factory.ts`)

实现了以下工厂函数：
- `successResponse<T>()` - 成功响应格式
- `errorResponse()` - 错误响应格式
- `pageResponse<T>()` - 分页响应格式
- `generateDepartment()` - 生成部门数据
- `generateUser()` - 生成用户数据
- `generateBook()` - 生成图书数据
- `generateReader()` - 生成读者数据
- `generateCirculation()` - 生成借阅记录数据

所有数据使用Faker.js中文语言包生成，确保数据真实性。

### 5. Mock数据集

#### 部门数据 (`mock/data/departments.ts`)
- 10条精心设计的部门数据（包含根部门、一级部门、二级部门）
- 20条随机生成的测试部门数据
- 包含完整的树形结构关系
- 提供查询辅助函数：`findDepartmentById()`, `findDepartmentsByParentId()`

#### 图书数据 (`mock/data/books.ts`)
- 100条图书模拟数据
- 包含流行书籍名称、作者、出版社等真实信息
- 提供查询辅助函数：`findBookById()`

#### 认证数据 (`mock/data/auth.ts`)
- 3个预设用户账号：
  - `admin/admin123` - 系统管理员
  - `librarian/librarian123` - 图书管理员
  - `director/director123` - 部门主任
- Token生成和验证机制
- 完整的权限配置

### 6. MSW Handlers实现

#### 认证API Handlers (`mock/msw/handlers/auth.ts`)
- ✅ POST `/api/v1/auth/login` - 用户登录
- ✅ POST `/api/v1/auth/logout` - 用户登出
- ✅ POST `/api/v1/auth/refresh` - 刷新Token
- ✅ GET `/api/v1/auth/user/info` - 获取当前用户信息
- ✅ PUT `/api/v1/auth/password` - 修改密码

#### 系统管理API Handlers (`mock/msw/handlers/system.ts`)
- ✅ GET `/api/v1/system/departments` - 获取部门列表（分页）
  - 支持关键词搜索（deptName, deptCode, leader）
  - 支持状态筛选
  - 支持分页参数
- ✅ GET `/api/v1/system/departments/tree` - 获取部门树形结构
- ✅ GET `/api/v1/system/departments/:id` - 获取部门详情
- ✅ POST `/api/v1/system/departments` - 创建部门
  - 验证必填字段
  - 检查编码重复
- ✅ PUT `/api/v1/system/departments/:id` - 更新部门
  - 防止循环父子关系
  - 检查编码重复
- ✅ DELETE `/api/v1/system/departments/:id` - 删除部门
  - 检查是否有子部门
- ✅ DELETE `/api/v1/system/departments/batch` - 批量删除部门

### 7. MSW Browser配置 (`mock/msw/browser.ts`)
- ✅ 合并所有handlers
- ✅ 创建MSW worker实例
- ✅ 实现 `startMockServer()` 函数
  - 根据环境变量控制启动
  - 友好的控制台日志输出
  - 错误处理
- ✅ 实现 `stopMockServer()` 函数

### 8. 主应用集成 (`src/main.js`)
```javascript
// 启动Mock服务器（仅在开发环境且配置启用时）
if (import.meta.env.VITE_USE_MOCK === 'true') {
  import('../mock/msw/browser').then(({ startMockServer }) => {
    startMockServer()
  })
}
```

### 9. 环境变量配置 (`.env.development`)
```env
# 是否启用Mock数据（true: 使用Mock API, false: 使用真实后端API）
VITE_USE_MOCK=true

# API基础路径 - Mock模式下会被MSW拦截
VITE_API_BASE_URL=http://localhost:8080

# 应用标题
VITE_APP_TITLE=国创睿峰智能图书馆管理系统

# Mock延迟时间（毫秒）
VITE_MOCK_DELAY=300
```

## 使用方法

### 启动Mock模式
1. 确保 `.env.development` 中 `VITE_USE_MOCK=true`
2. 启动开发服务器：`npm run dev`
3. 打开浏览器访问应用
4. 在浏览器控制台查看Mock服务器启动日志：
   ```
   🚀 Mock Server Started
   📦 Mock API Base URL: http://localhost:8080
   ✅ Handlers registered: 11
   ```

### 切换到真实后端
1. 修改 `.env.development` 中 `VITE_USE_MOCK=false`
2. 确保 `VITE_API_BASE_URL` 指向真实后端地址（如 `http://localhost:8084`）
3. 重启开发服务器

### 测试Mock API

#### 测试登录
```javascript
// 在浏览器控制台执行
fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'admin123'
  })
}).then(r => r.json()).then(console.log)
```

#### 测试部门列表
```javascript
// 获取分页列表
fetch('http://localhost:8080/api/v1/system/departments?pageNum=1&pageSize=10')
  .then(r => r.json())
  .then(console.log)

// 搜索部门
fetch('http://localhost:8080/api/v1/system/departments?keyword=流通')
  .then(r => r.json())
  .then(console.log)
```

## 技术特性

### 1. 真实的网络延迟模拟
- 所有API请求都有200-400ms的延迟
- 模拟真实网络环境
- 可通过 `VITE_MOCK_DELAY` 配置

### 2. 完整的CRUD操作
- 创建、读取、更新、删除操作都有完整实现
- 包含数据验证逻辑
- 包含业务规则检查（如循环引用检查、子部门检查等）

### 3. 中文真实数据
- 使用Faker.js中文语言包
- 生成真实的中文姓名、地址、公司名等
- 符合中国本地化需求

### 4. 统一响应格式
所有API响应遵循统一格式：
```typescript
{
  code: 200,
  message: 'success',
  data: {...},
  timestamp: 1704067200000,
  traceId: 'uuid-xxxx'
}
```

### 5. 完整的分页支持
```typescript
{
  code: 200,
  data: {
    records: [...],
    total: 100,
    pageNum: 1,
    pageSize: 20,
    pages: 5
  }
}
```

### 6. 状态持久化
- Mock数据在页面刷新前保持修改状态
- 创建、更新、删除操作会实时反映在数据集中
- 页面刷新后恢复初始状态

## 现有功能验证

### 部门管理页面
已存在的部门管理页面 (`src/views/system/departments.vue`) 可以直接使用Mock API：

1. ✅ 部门列表查询（分页）
2. ✅ 部门搜索
3. ✅ 新增部门
4. ✅ 编辑部门
5. ✅ 删除部门
6. ✅ 批量删除

访问路径：`http://localhost:3011/#/system/departments`

## 下一步工作

### 待添加的Mock Handlers

根据API-Design.md文档，还需要实现以下Mock handlers：

1. **图书管理API** (`mock/msw/handlers/books.ts`)
   - 图书CRUD操作
   - 图书搜索和筛选
   - 图书分类管理

2. **借阅管理API** (`mock/msw/handlers/circulation.ts`)
   - 借阅、归还操作
   - 续借操作
   - 借阅历史查询
   - 逾期管理

3. **读者管理API** (`mock/msw/handlers/readers.ts`)
   - 读者CRUD操作
   - 读者卡管理
   - 读者信用管理

4. **数据分析API** (`mock/msw/handlers/analytics.ts`)
   - 统计数据
   - 趋势分析
   - 报表数据

5. **文件上传API** (`mock/msw/handlers/files.ts`)
   - 图书封面上传
   - 文件下载

6. **通知API** (`mock/msw/handlers/notifications.ts`)
   - 系统通知
   - 消息推送

### 前端页面开发
基于Mock API，可以开始开发以下前端页面：

1. **认证相关**
   - 登录页面
   - 忘记密码页面

2. **图书管理**
   - 图书列表页
   - 图书详情页
   - 图书编辑页

3. **借阅管理**
   - 借阅管理页
   - 归还管理页
   - 借阅历史页

4. **读者管理**
   - 读者列表页
   - 读者详情页
   - 读者卡管理页

5. **数据分析**
   - 数据大屏
   - 统计报表页

## 优势总结

1. **前后端并行开发** - 前端团队不再受后端API开发进度限制
2. **快速迭代** - 修改Mock数据和API行为非常便捷
3. **离线开发** - 不需要运行后端服务即可开发和测试
4. **真实体验** - 模拟网络延迟和真实数据，接近生产环境
5. **易于切换** - 一键切换Mock/真实API，无需修改代码
6. **完整测试** - 可以测试各种边界情况和错误场景

## 文件清单

### 新增文件
```
web-admin/
├── public/mockServiceWorker.js          # MSW service worker（自动生成）
├── mock/
│   ├── data/
│   │   ├── factory.ts                   # 数据工厂函数
│   │   ├── departments.ts               # 部门模拟数据
│   │   ├── books.ts                     # 图书模拟数据
│   │   └── auth.ts                      # 认证数据
│   └── msw/
│       ├── handlers/
│       │   ├── auth.ts                  # 认证API handlers
│       │   └── system.ts                # 系统管理API handlers
│       └── browser.ts                   # MSW配置
```

### 修改文件
```
web-admin/
├── .env.development                     # 添加VITE_USE_MOCK等配置
├── src/main.js                          # 集成Mock服务器启动
└── package.json                         # MSW依赖和配置
```

## 参考文档
- [Mock-API-Strategy.md](./Mock-API-Strategy.md) - Mock API策略完整方案
- [API-Design.md](./API-Design.md) - API设计文档
- [MSW官方文档](https://mswjs.io/)
- [Faker.js文档](https://fakerjs.dev/)
