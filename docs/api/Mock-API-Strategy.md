# Mock API 开发策略文档

**版本**: v1.0
**日期**: 2025-10-14
**策略**: 前端优先,Mock API驱动开发

---

## 目录

1. [策略概述](#1-策略概述)
2. [技术方案选择](#2-技术方案选择)
3. [Mock数据设计原则](#3-mock数据设计原则)
4. [实施步骤](#4-实施步骤)
5. [Mock Server配置](#5-mock-server配置)
6. [Mock数据示例](#6-mock数据示例)
7. [前端集成方案](#7-前端集成方案)
8. [Mock到真实API的切换](#8-mock到真实api的切换)
9. [团队协作流程](#9-团队协作流程)

---

## 1. 策略概述

### 1.1 开发策略

```
阶段1: Mock API + 前端开发 (并行)
  ├─ 前端: 基于Mock API开发所有页面和交互逻辑
  ├─ 后端: 独立开发真实API和业务逻辑
  └─ 时间: 4-6周

阶段2: 联调切换
  ├─ 前端: 切换Mock API为真实API
  ├─ 后端: 提供真实API接口
  └─ 时间: 1-2周

阶段3: 集成测试
  └─ 端到端测试、性能测试
```

### 1.2 优势

✅ **前后端解耦**: 前端不依赖后端进度,可独立快速迭代
✅ **用户体验验证**: 快速构建完整原型,验证交互流程
✅ **并行开发**: 前端、后端、数据库可同时开发
✅ **测试友好**: Mock数据可控,方便测试边界场景
✅ **文档即合约**: Mock API即接口文档,减少沟通成本

---

## 2. 技术方案选择

### 2.1 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|-----|------|------|--------|
| **MSW (Mock Service Worker)** | 拦截网络请求,无侵入,生产环境零影响 | 需要学习成本 | ⭐⭐⭐⭐⭐ |
| **Vite Mock Plugin** | 与Vite深度集成,配置简单 | 仅开发环境 | ⭐⭐⭐⭐ |
| **JSON Server** | 快速搭建RESTful API,零代码 | 功能有限 | ⭐⭐⭐ |
| **Axios Mock Adapter** | 拦截Axios请求,轻量 | 与Axios耦合 | ⭐⭐⭐ |
| **独立Mock Server** | 完全模拟真实环境 | 需要额外维护 | ⭐⭐⭐⭐ |

### 2.2 推荐方案: **MSW + Vite Mock Plugin**

**组合使用原因**:
- MSW: 用于复杂场景和浏览器环境Mock
- Vite Mock Plugin: 用于简单场景和快速开发

---

## 3. Mock数据设计原则

### 3.1 真实性原则

Mock数据应尽可能接近真实数据:

✅ **正确的数据类型**: 字符串、数字、布尔值、日期格式
✅ **合理的数据范围**: 如借阅量0-1000,评分0-5
✅ **真实的中文内容**: 使用真实的书名、作者名、读者姓名
✅ **符合业务逻辑**: 如逾期图书的到期日期必须是过去时间

### 3.2 完整性原则

Mock数据应覆盖所有场景:

✅ **正常场景**: 成功响应
✅ **异常场景**: 错误响应(404, 500, 业务错误)
✅ **边界场景**: 空列表、超长文本、特殊字符
✅ **分页场景**: 多页数据、最后一页、空页

### 3.3 可维护性原则

✅ **数据工厂模式**: 使用函数生成Mock数据,便于复用
✅ **统一数据源**: 集中管理Mock数据文件
✅ **版本同步**: Mock数据结构与API文档保持一致

---

## 4. 实施步骤

### Step 1: 安装依赖

```bash
cd web-admin

# 安装MSW
npm install msw --save-dev

# 安装Vite Mock Plugin
npm install vite-plugin-mock --save-dev

# 安装Mock数据生成库
npm install @faker-js/faker --save-dev
```

### Step 2: 初始化MSW

```bash
# 生成Service Worker文件
npx msw init public/ --save
```

### Step 3: 创建Mock目录结构

```
web-admin/
├── mock/
│   ├── index.ts                 # Mock入口
│   ├── msw/
│   │   ├── handlers/
│   │   │   ├── auth.ts          # 认证相关Mock
│   │   │   ├── books.ts         # 图书相关Mock
│   │   │   ├── circulation.ts   # 流通相关Mock
│   │   │   ├── readers.ts       # 读者相关Mock
│   │   │   ├── system.ts        # 系统相关Mock
│   │   │   └── analytics.ts     # 数据分析Mock
│   │   └── browser.ts           # MSW浏览器配置
│   ├── data/
│   │   ├── books.ts             # 图书Mock数据
│   │   ├── readers.ts           # 读者Mock数据
│   │   ├── circulation.ts       # 流通Mock数据
│   │   └── factory.ts           # 数据工厂函数
│   └── config.ts                # Mock配置
```

### Step 4: 配置Vite

### Step 5: 开发Mock数据和Handler

### Step 6: 前端集成Mock

### Step 7: 开发完成后切换真实API

---

## 5. Mock Server配置

### 5.1 Vite配置 (vite.config.ts)

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig(({ command }) => {
  return {
    plugins: [
      vue(),
      viteMockServe({
        mockPath: 'mock',
        enable: command === 'serve',  // 仅在开发环境启用
        watchFiles: true,  // 监听Mock文件变化
        logger: true,  // 打印Mock日志
      })
    ]
  }
})
```

### 5.2 MSW配置 (mock/msw/browser.ts)

```typescript
import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

// 创建MSW Worker
export const worker = setupWorker(...handlers)

// 启动配置
export const startMockServer = () => {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    worker.start({
      onUnhandledRequest: 'warn',  // 未处理请求时警告
      serviceWorker: {
        url: '/mockServiceWorker.js'
      }
    })
    console.log('🚀 Mock Server Started')
  }
}
```

### 5.3 Mock入口 (mock/index.ts)

```typescript
import { MockMethod } from 'vite-plugin-mock'
import authMock from './handlers/auth'
import booksMock from './handlers/books'
import circulationMock from './handlers/circulation'
import readersMock from './handlers/readers'
import systemMock from './handlers/system'
import analyticsMock from './handlers/analytics'

export default [
  ...authMock,
  ...booksMock,
  ...circulationMock,
  ...readersMock,
  ...systemMock,
  ...analyticsMock,
] as MockMethod[]
```

### 5.4 环境配置 (.env.development)

```env
# 是否启用Mock
VITE_USE_MOCK=true

# API基础路径(Mock模式下无效)
VITE_API_BASE_URL=http://localhost:8080

# Mock延迟(ms),模拟网络延迟
VITE_MOCK_DELAY=300
```

---

## 6. Mock数据示例

### 6.1 数据工厂 (mock/data/factory.ts)

```typescript
import { faker } from '@faker-js/faker/locale/zh_CN'

// 通用响应包装
export const successResponse = (data: any) => ({
  code: 200,
  message: 'success',
  data,
  timestamp: Date.now(),
  traceId: faker.string.uuid()
})

export const errorResponse = (code: number, message: string) => ({
  code,
  message,
  data: null,
  timestamp: Date.now(),
  traceId: faker.string.uuid()
})

// 分页响应包装
export const pageResponse = (records: any[], total: number, pageNum: number, pageSize: number) => ({
  code: 200,
  data: {
    records,
    total,
    pageNum,
    pageSize,
    pages: Math.ceil(total / pageSize)
  },
  timestamp: Date.now()
})

// 生成图书数据
export const generateBook = (id?: number) => ({
  bookId: id || faker.number.int({ min: 1, max: 10000 }),
  isbn: faker.string.numeric(13),
  title: faker.helpers.arrayElement([
    '三体', '活着', '红楼梦', '百年孤独', '人类简史',
    '围城', '平凡的世界', '追风筝的人', '解忧杂货店', '小王子'
  ]),
  author: faker.person.fullName(),
  publisher: faker.helpers.arrayElement([
    '人民文学出版社', '上海译文出版社', '中信出版社', '机械工业出版社'
  ]),
  publishDate: faker.date.past({ years: 10 }).toISOString().split('T')[0],
  category: faker.helpers.arrayElement(['I247', 'K', 'B', 'TP', 'F']),
  callNumber: `I247.55/${faker.number.int({ min: 1, max: 999 })}`,
  price: faker.number.float({ min: 10, max: 200, fractionDigits: 2 }),
  coverUrl: `https://picsum.photos/seed/${faker.number.int({ min: 1, max: 1000 })}/300/400`,
  summary: faker.lorem.paragraph(),
  keywords: ['小说', '文学', '经典'].join(','),
  tags: faker.helpers.arrayElements(['畅销', '经典', '获奖', '推荐'], 2),
  totalCopies: faker.number.int({ min: 1, max: 10 }),
  availableCopies: faker.number.int({ min: 0, max: 8 }),
  borrowCount: faker.number.int({ min: 0, max: 500 }),
  rating: faker.number.float({ min: 3.5, max: 5.0, fractionDigits: 1 }),
  status: 'AVAILABLE',
  createdAt: faker.date.past({ years: 2 }).toISOString()
})

// 生成读者数据
export const generateReader = (id?: number) => ({
  readerId: id || faker.number.int({ min: 1, max: 5000 }),
  readerNo: `RD${faker.string.numeric(8)}`,
  name: faker.person.fullName(),
  type: faker.helpers.arrayElement(['STUDENT', 'TEACHER']),
  grade: faker.helpers.arrayElement(['一年级', '二年级', '三年级', '四年级', '五年级', '六年级']),
  class: faker.helpers.arrayElement(['1班', '2班', '3班', '4班']),
  gender: faker.helpers.arrayElement(['MALE', 'FEMALE']),
  phone: faker.phone.number(),
  email: faker.internet.email(),
  avatarUrl: `https://i.pravatar.cc/150?img=${faker.number.int({ min: 1, max: 70 })}`,
  hasFaceFeature: faker.datatype.boolean(),
  currentBorrowCount: faker.number.int({ min: 0, max: 5 }),
  totalBorrowCount: faker.number.int({ min: 0, max: 200 }),
  status: 'ACTIVE',
  createdAt: faker.date.past({ years: 3 }).toISOString()
})

// 生成流通记录
export const generateCirculation = (id?: number) => {
  const borrowDate = faker.date.past({ years: 1 })
  const dueDate = new Date(borrowDate.getTime() + 30 * 24 * 60 * 60 * 1000)
  const isReturned = faker.datatype.boolean()
  const returnDate = isReturned ? faker.date.between({ from: borrowDate, to: new Date() }) : null
  const isOverdue = !isReturned && new Date() > dueDate

  return {
    circulationId: id || faker.number.int({ min: 1, max: 50000 }),
    barcode: `BK${faker.string.numeric(6)}`,
    bookTitle: faker.helpers.arrayElement(['三体', '活着', '红楼梦', '百年孤独']),
    coverUrl: `https://picsum.photos/seed/${faker.number.int({ min: 1, max: 1000 })}/300/400`,
    readerNo: `RD${faker.string.numeric(8)}`,
    readerName: faker.person.fullName(),
    borrowDate: borrowDate.toISOString(),
    dueDate: dueDate.toISOString(),
    returnDate: returnDate?.toISOString() || null,
    status: isReturned ? 'RETURNED' : 'BORROWED',
    isOverdue,
    overdueDays: isOverdue ? Math.floor((new Date().getTime() - dueDate.getTime()) / (24 * 60 * 60 * 1000)) : 0,
    renewCount: faker.number.int({ min: 0, max: 2 })
  }
}

// 生成部门数据
export const generateDepartment = (id?: number) => ({
  id: id || faker.number.int({ min: 1, max: 100 }),
  deptCode: `DEPT${faker.string.numeric(3)}`,
  deptName: faker.helpers.arrayElement([
    '技术部', '编目部', '流通部', '参考咨询部', '行政管理部'
  ]),
  parentId: faker.helpers.arrayElement([null, 1, 2]),
  deptLevel: faker.number.int({ min: 1, max: 3 }),
  deptPath: `/1/${faker.number.int({ min: 1, max: 10 })}`,
  leaderName: faker.person.fullName(),
  phone: faker.phone.number(),
  email: faker.internet.email(),
  status: 'ACTIVE',
  description: faker.lorem.sentence(),
  createdAt: faker.date.past({ years: 1 }).toISOString()
})
```

### 6.2 图书Mock Handler (mock/handlers/books.ts)

```typescript
import { MockMethod } from 'vite-plugin-mock'
import { generateBook, pageResponse, successResponse, errorResponse } from '../data/factory'

// 生成100本图书数据
const mockBooks = Array.from({ length: 100 }, (_, i) => generateBook(i + 1))

export default [
  // 查询图书列表
  {
    url: '/api/v1/books',
    method: 'get',
    timeout: 300,
    response: ({ query }: any) => {
      const { pageNum = 1, pageSize = 20, keyword = '' } = query

      // 模拟搜索
      let filteredBooks = mockBooks
      if (keyword) {
        filteredBooks = mockBooks.filter(book =>
          book.title.includes(keyword) ||
          book.author.includes(keyword)
        )
      }

      const start = (pageNum - 1) * pageSize
      const end = start + parseInt(pageSize)
      const records = filteredBooks.slice(start, end)

      return pageResponse(records, filteredBooks.length, parseInt(pageNum), parseInt(pageSize))
    }
  },

  // 根据ID查询图书
  {
    url: '/api/v1/books/:id',
    method: 'get',
    response: ({ query }: any) => {
      const book = mockBooks.find(b => b.bookId === parseInt(query.id))
      if (book) {
        return successResponse(book)
      }
      return errorResponse(404, '图书不存在')
    }
  },

  // 创建图书编目
  {
    url: '/api/v1/books',
    method: 'post',
    response: ({ body }: any) => {
      const newBook = {
        ...body,
        bookId: mockBooks.length + 1,
        callNumber: `${body.category}/${mockBooks.length + 1}`,
        totalCopies: 0,
        availableCopies: 0,
        borrowCount: 0,
        rating: 0,
        status: 'AVAILABLE',
        createdAt: new Date().toISOString()
      }
      mockBooks.push(newBook)

      return successResponse({
        bookId: newBook.bookId,
        callNumber: newBook.callNumber
      })
    }
  },

  // 更新图书信息
  {
    url: '/api/v1/books/:id',
    method: 'put',
    response: ({ query, body }: any) => {
      const index = mockBooks.findIndex(b => b.bookId === parseInt(query.id))
      if (index !== -1) {
        mockBooks[index] = { ...mockBooks[index], ...body }
        return successResponse({ message: '更新成功' })
      }
      return errorResponse(404, '图书不存在')
    }
  },

  // 删除图书
  {
    url: '/api/v1/books/:id',
    method: 'delete',
    response: ({ query }: any) => {
      const index = mockBooks.findIndex(b => b.bookId === parseInt(query.id))
      if (index !== -1) {
        mockBooks.splice(index, 1)
        return successResponse({ message: '删除成功' })
      }
      return errorResponse(404, '图书不存在')
    }
  },

  // 图书排行榜
  {
    url: '/api/v1/analytics/book-rankings',
    method: 'get',
    response: () => {
      const topBooks = mockBooks
        .sort((a, b) => b.borrowCount - a.borrowCount)
        .slice(0, 20)
        .map((book, index) => ({
          rank: index + 1,
          ...book
        }))

      return successResponse(topBooks)
    }
  }
] as MockMethod[]
```

### 6.3 认证Mock Handler (mock/handlers/auth.ts)

```typescript
import { MockMethod } from 'vite-plugin-mock'
import { successResponse, errorResponse } from '../data/factory'

export default [
  // 登录
  {
    url: '/api/v1/auth/login',
    method: 'post',
    timeout: 500,
    response: ({ body }: any) => {
      const { username, password } = body

      // 模拟登录验证
      if (username === 'admin' && password === 'admin123') {
        return successResponse({
          token: 'mock-jwt-token-' + Date.now(),
          refreshToken: 'mock-refresh-token',
          expiresIn: 7200,
          user: {
            userId: 1,
            username: 'admin',
            realName: '系统管理员',
            role: 'ADMIN',
            permissions: ['*']
          }
        })
      }

      return errorResponse(401, '用户名或密码错误')
    }
  },

  // 登出
  {
    url: '/api/v1/auth/logout',
    method: 'post',
    response: () => {
      return successResponse({ message: '登出成功' })
    }
  },

  // 获取验证码
  {
    url: '/api/v1/auth/captcha',
    method: 'get',
    response: () => {
      return successResponse({
        captchaKey: 'mock-captcha-key',
        captchaImage: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjQwIj48dGV4dCB4PSIxMCIgeT0iMjUiPjEyMzQ8L3RleHQ+PC9zdmc+'
      })
    }
  }
] as MockMethod[]
```

### 6.4 系统管理Mock Handler (mock/handlers/system.ts)

```typescript
import { MockMethod } from 'vite-plugin-mock'
import { generateDepartment, pageResponse, successResponse, errorResponse } from '../data/factory'

// 默认6个部门
const mockDepartments = [
  {
    id: 1,
    deptCode: 'ROOT',
    deptName: '广州城市图书馆',
    parentId: null,
    deptLevel: 1,
    deptPath: '/1',
    leaderName: '馆长',
    phone: '020-12345678',
    email: 'library@example.com',
    status: 'ACTIVE',
    description: '顶级机构',
    createdAt: '2025-01-01T00:00:00'
  },
  {
    id: 2,
    deptCode: 'ADMIN',
    deptName: '行政管理部',
    parentId: 1,
    deptLevel: 2,
    deptPath: '/1/2',
    leaderName: '李主任',
    phone: '020-12345679',
    email: 'admin@example.com',
    status: 'ACTIVE',
    description: '负责行政管理工作',
    createdAt: '2025-01-01T00:00:00'
  },
  {
    id: 3,
    deptCode: 'TECH',
    deptName: '技术服务部',
    parentId: 1,
    deptLevel: 2,
    deptPath: '/1/3',
    leaderName: '张工',
    phone: '020-12345680',
    email: 'tech@example.com',
    status: 'ACTIVE',
    description: '负责图书馆技术服务',
    createdAt: '2025-01-01T00:00:00'
  },
  {
    id: 4,
    deptCode: 'CIRCULATION',
    deptName: '流通部',
    parentId: 1,
    deptLevel: 2,
    deptPath: '/1/4',
    leaderName: '王老师',
    phone: '020-12345681',
    email: 'circulation@example.com',
    status: 'ACTIVE',
    description: '负责图书借还业务',
    createdAt: '2025-01-01T00:00:00'
  },
  {
    id: 5,
    deptCode: 'CATALOG',
    deptName: '编目部',
    parentId: 1,
    deptLevel: 2,
    deptPath: '/1/5',
    leaderName: '赵老师',
    phone: '020-12345682',
    email: 'catalog@example.com',
    status: 'ACTIVE',
    description: '负责图书编目工作',
    createdAt: '2025-01-01T00:00:00'
  },
  {
    id: 6,
    deptCode: 'REFERENCE',
    deptName: '参考咨询部',
    parentId: 1,
    deptLevel: 2,
    deptPath: '/1/6',
    leaderName: '刘老师',
    phone: '020-12345683',
    email: 'reference@example.com',
    status: 'ACTIVE',
    description: '负责读者咨询服务',
    createdAt: '2025-01-01T00:00:00'
  }
]

export default [
  // 查询部门列表
  {
    url: '/api/v1/system/departments',
    method: 'get',
    response: ({ query }: any) => {
      const { pageNum = 1, pageSize = 20, deptName = '', status = '' } = query

      let filtered = mockDepartments
      if (deptName) {
        filtered = filtered.filter(d => d.deptName.includes(deptName))
      }
      if (status) {
        filtered = filtered.filter(d => d.status === status)
      }

      const start = (pageNum - 1) * pageSize
      const end = start + parseInt(pageSize)
      const records = filtered.slice(start, end)

      return pageResponse(records, filtered.length, parseInt(pageNum), parseInt(pageSize))
    }
  },

  // 创建部门
  {
    url: '/api/v1/system/departments',
    method: 'post',
    response: ({ body }: any) => {
      const newDept = {
        ...body,
        id: mockDepartments.length + 1,
        deptLevel: 2,
        deptPath: `/1/${mockDepartments.length + 1}`,
        status: 'ACTIVE',
        createdAt: new Date().toISOString()
      }
      mockDepartments.push(newDept)

      return successResponse({
        id: newDept.id,
        deptCode: newDept.deptCode
      })
    }
  },

  // 更新部门
  {
    url: '/api/v1/system/departments/:id',
    method: 'put',
    response: ({ query, body }: any) => {
      const index = mockDepartments.findIndex(d => d.id === parseInt(query.id))
      if (index !== -1) {
        mockDepartments[index] = { ...mockDepartments[index], ...body }
        return successResponse({ message: '更新成功' })
      }
      return errorResponse(404, '部门不存在')
    }
  },

  // 删除部门
  {
    url: '/api/v1/system/departments/:id',
    method: 'delete',
    response: ({ query }: any) => {
      const index = mockDepartments.findIndex(d => d.id === parseInt(query.id))
      if (index !== -1) {
        mockDepartments.splice(index, 1)
        return successResponse({ message: '删除成功' })
      }
      return errorResponse(404, '部门不存在')
    }
  },

  // 获取系统配置
  {
    url: '/api/v1/system/configs',
    method: 'get',
    response: () => {
      return successResponse({
        libraryName: '广州城市图书馆',
        studentMaxBorrow: 5,
        studentBorrowDays: 30,
        teacherMaxBorrow: 10,
        teacherBorrowDays: 60,
        overdueFinePerDay: 0.10,
        reservationDays: 7,
        notificationEnabled: true
      })
    }
  }
] as MockMethod[]
```

---

## 7. 前端集成方案

### 7.1 主入口配置 (src/main.ts)

```typescript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)

// 开发环境启用Mock
if (import.meta.env.VITE_USE_MOCK === 'true') {
  // 动态导入MSW
  import('../mock/msw/browser').then(({ startMockServer }) => {
    startMockServer()
  })
}

app.use(createPinia())
app.use(router)
app.mount('#app')
```

### 7.2 Axios配置支持Mock (src/utils/request.ts)

```typescript
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const service: AxiosInstance = axios.create({
  // Mock模式下,baseURL会被MSW拦截,实际不会发送到后端
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: any) => {
    const userStore = useUserStore()

    // 添加Token
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }

    // Mock模式日志
    if (import.meta.env.VITE_USE_MOCK === 'true') {
      console.log('🔵 [Mock Request]', config.method?.toUpperCase(), config.url, config.data || config.params)
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // Mock模式日志
    if (import.meta.env.VITE_USE_MOCK === 'true') {
      console.log('🟢 [Mock Response]', response.config.url, res)
    }

    // 统一错误处理
    if (res.code !== 200 && res.code !== 201) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    console.error('请求错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

### 7.3 API调用示例

所有API调用保持不变,Mock会自动拦截:

```typescript
// src/api/department.ts
import request from '@/utils/request'

export function getDepartmentList(params: any) {
  return request({
    url: '/api/v1/system/departments',
    method: 'get',
    params
  })
}

// 使用
const fetchData = async () => {
  try {
    const res = await getDepartmentList({ pageNum: 1, pageSize: 20 })
    console.log('部门数据:', res.data)
  } catch (error) {
    console.error('获取失败:', error)
  }
}
```

---

## 8. Mock到真实API的切换

### 8.1 一键切换

**方法1: 环境变量切换**

```bash
# 开发模式 - 使用Mock
VITE_USE_MOCK=true npm run dev

# 开发模式 - 使用真实API
VITE_USE_MOCK=false npm run dev
```

**方法2: 配置文件切换**

修改 `.env.development`:

```env
# 切换为false即可使用真实API
VITE_USE_MOCK=false

# 真实API地址
VITE_API_BASE_URL=http://localhost:8080
```

### 8.2 渐进式切换

支持部分接口使用Mock,部分使用真实API:

```typescript
// mock/config.ts
export const mockConfig = {
  // 启用的Mock模块
  enabledModules: {
    auth: false,      // 已完成,使用真实API
    books: true,      // 开发中,使用Mock
    circulation: true,
    readers: true,
    system: false     // 已完成,使用真实API
  }
}

// mock/msw/browser.ts
import { mockConfig } from '../config'

export const startMockServer = () => {
  // 根据配置过滤Handler
  const enabledHandlers = handlers.filter(handler => {
    const moduleName = getModuleFromUrl(handler.info.path)
    return mockConfig.enabledModules[moduleName] !== false
  })

  worker.start({ handlers: enabledHandlers })
}
```

### 8.3 联调检查清单

切换前检查:

- [ ] 所有Mock数据结构与API文档一致
- [ ] 所有接口路径与后端路由匹配
- [ ] 请求参数命名和类型正确
- [ ] 响应数据格式统一
- [ ] 错误码定义一致

---

## 9. 团队协作流程

### 9.1 开发流程

```
1. API设计阶段
   ├─ 产品/后端: 编写API文档
   └─ 前端: 基于文档创建Mock Handler

2. 并行开发阶段
   ├─ 前端: 使用Mock API开发所有页面
   └─ 后端: 开发真实API接口

3. 联调阶段
   ├─ 后端: 部署到测试环境
   ├─ 前端: 切换VITE_USE_MOCK=false
   └─ 联合测试和修复问题

4. 上线阶段
   └─ 生产环境自动禁用Mock
```

### 9.2 Mock数据维护

**责任人**: 前端负责Mock数据的创建和维护

**更新时机**:
- API文档变更时同步更新Mock
- 新增接口时立即创建Mock Handler
- Bug修复后更新Mock数据

**版本控制**:
- Mock代码纳入Git版本管理
- Mock数据变更需要Code Review
- 重要Mock数据添加注释说明

### 9.3 沟通机制

**每日站会**:
- 前端同步Mock API覆盖进度
- 后端同步真实API开发进度
- 识别接口变更和风险

**接口变更流程**:
1. 后端发起变更需求
2. 更新API文档
3. 前端同步更新Mock Handler
4. 双方确认后执行

---

## 10. 常见问题FAQ

### Q1: Mock数据需要和真实数据一模一样吗?

A: 数据结构必须一致,但数据内容可以简化。重点保证:
- 字段名和类型一致
- 必填字段不为空
- 数组和对象结构正确

### Q2: 如何模拟网络延迟和加载状态?

A: 在Mock Handler中添加timeout:

```typescript
{
  url: '/api/v1/books',
  method: 'get',
  timeout: 1000,  // 模拟1秒延迟
  response: () => { ... }
}
```

### Q3: 如何模拟错误场景?

A: 创建特定条件触发的错误响应:

```typescript
{
  url: '/api/v1/books',
  method: 'post',
  response: ({ body }) => {
    // 模拟ISBN重复错误
    if (body.isbn === '9999999999999') {
      return errorResponse(400, 'ISBN已存在')
    }
    return successResponse({ ... })
  }
}
```

### Q4: Mock会影响生产环境吗?

A: 不会。生产构建时:
- `VITE_USE_MOCK`默认为false
- MSW代码不会打包进生产bundle
- 零性能影响

### Q5: 如何调试Mock数据?

A: 多种方式:
1. 浏览器控制台查看Mock日志
2. 使用Vue DevTools查看API请求
3. MSW DevTools浏览器扩展
4. 在Mock Handler中添加console.log

---

## 附录

### A. Mock数据生成器推荐

| 工具 | 用途 | 网址 |
|-----|------|------|
| Faker.js | 生成各种Mock数据 | https://fakerjs.dev/ |
| Mock.js | 中文Mock数据 | http://mockjs.com/ |
| JSON Generator | 在线生成JSON | https://json-generator.com/ |
| Lorem Picsum | 占位图片 | https://picsum.photos/ |

### B. 参考资源

- [MSW官方文档](https://mswjs.io/)
- [Vite Mock Plugin](https://github.com/vbenjs/vite-plugin-mock)
- [Faker.js中文文档](https://fakerjs.dev/zh_CN/)

---

**文档结束**

*Mock API策略让前后端开发真正解耦,大幅提升开发效率!*
