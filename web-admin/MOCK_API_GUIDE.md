# Mock API 使用指南

## 概述

本项目已配置完整的Mock API服务，使用MSW (Mock Service Worker) 实现，可在不依赖后端的情况下进行前端开发和演示。

## 启用Mock服务

Mock服务在开发环境下自动启用，无需额外配置。

### 验证Mock服务状态

1. 启动开发服务器:
```bash
npm run dev
```

2. 打开浏览器控制台,应该看到:
```
[MSW] Mock Service Worker started
```

3. 访问任何API接口,都会返回模拟数据

## Mock数据说明

### 1. 认证模块 (`/api/v1/auth/*`)

#### 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | 管理员 | 拥有所有权限 |
| librarian | lib123 | 馆员 | 拥有流通管理和查看权限 |
| operator | op123 | 操作员 | 仅拥有借还书权限 |

#### 可用接口

- `POST /api/v1/auth/login` - 登录
- `POST /api/v1/auth/logout` - 退出
- `GET /api/v1/auth/user/info` - 获取当前用户信息
- `POST /api/v1/auth/change-password` - 修改密码
- `PUT /api/v1/auth/user/info` - 更新用户信息

### 2. 图书管理模块 (`/api/v1/books/*`)

#### 数据说明

- 包含100本精选图书数据
- 覆盖多个分类(文学、计算机、历史等)
- 包含真实的ISBN、书名、作者、出版社等信息
- 支持搜索、分页、过滤等功能

#### 可用接口

- `GET /api/v1/books` - 获取图书列表(支持分页、搜索、过滤)
- `GET /api/v1/books/:id` - 获取图书详情
- `POST /api/v1/books` - 新增图书
- `PUT /api/v1/books/:id` - 更新图书
- `DELETE /api/v1/books/:id` - 删除图书
- `DELETE /api/v1/books/batch` - 批量删除图书
- `GET /api/v1/books/categories` - 获取图书分类
- `POST /api/v1/books/import` - 导入图书
- `GET /api/v1/books/stats` - 获取图书统计数据

### 3. 读者管理模块 (`/api/v1/readers/*`)

#### 数据说明

- 包含200个读者数据
- 包含学生、教师、职工、公众读者等类型
- 模拟真实的读者信息(姓名、证号、借阅记录等)

#### 可用接口

- `GET /api/v1/readers` - 获取读者列表(支持分页、搜索、过滤)
- `GET /api/v1/readers/:id` - 获取读者详情
- `POST /api/v1/readers` - 新增读者
- `PUT /api/v1/readers/:id` - 更新读者
- `DELETE /api/v1/readers/:id` - 删除读者
- `DELETE /api/v1/readers/batch` - 批量删除读者
- `GET /api/v1/readers/types` - 获取读者类型
- `POST /api/v1/readers/:id/card` - 办理/更新借阅证
- `PUT /api/v1/readers/:id/status` - 更新读者状态
- `GET /api/v1/readers/stats` - 获取读者统计数据

### 4. 流通管理模块 (`/api/v1/circulation/*`)

#### 数据说明

- 包含500条流通记录
- 包含100条预约记录
- 模拟借阅、归还、续借、预约等完整流程

#### 可用接口

- `GET /api/v1/circulation/records` - 获取流通记录列表
- `POST /api/v1/circulation/borrow` - 借书
- `POST /api/v1/circulation/return` - 还书
- `POST /api/v1/circulation/renew` - 续借
- `GET /api/v1/circulation/reservations` - 获取预约列表
- `POST /api/v1/circulation/reserve` - 预约图书
- `POST /api/v1/circulation/cancel-reservation` - 取消预约
- `GET /api/v1/circulation/stats` - 获取流通统计数据

### 5. 数据分析模块 (`/api/v1/analytics/*`)

#### 数据说明

- 提供丰富的统计分析数据
- 包含借阅趋势、分类统计、排行榜等
- 数据随机生成,每次刷新会有变化

#### 可用接口

- `GET /api/v1/analytics/overview` - 获取借阅概览
- `GET /api/v1/analytics/borrow-trends` - 获取借阅趋势
- `GET /api/v1/analytics/category-stats` - 获取分类统计
- `GET /api/v1/analytics/book-rankings` - 获取图书排行榜
- `GET /api/v1/analytics/reader-rankings` - 获取读者排行榜
- `GET /api/v1/analytics/collection-analysis` - 获取馆藏资源分析
- `GET /api/v1/analytics/recent-activities` - 获取最近活动
- `GET /api/v1/analytics/grade-preferences` - 获取年级借阅偏好
- `GET /api/v1/analytics/yearly-circulation` - 获取年度流通统计

## 自定义Mock数据

### 修改现有数据

编辑 `src/mock/data/` 目录下的文件:

- `books.js` - 图书数据
- `readers.js` - 读者数据
- `circulation.js` - 流通记录数据
- `analytics.js` - 统计分析数据

### 添加新的API接口

1. 在 `src/mock/handlers/` 目录下创建新的handler文件
2. 在 `src/mock/browser.js` 中引入并添加到handlers数组
3. 重启开发服务器

示例:

```javascript
// src/mock/handlers/custom.js
import { http, HttpResponse } from 'msw'

export const customHandlers = [
  http.get('/api/v1/custom', () => {
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { /* your data */ }
    })
  })
]
```

```javascript
// src/mock/browser.js
import { customHandlers } from './handlers/custom'

export const handlers = [
  ...authHandlers,
  ...booksHandlers,
  ...customHandlers // 添加新的handlers
]
```

## 注意事项

1. Mock服务仅在开发环境启用,生产构建时会自动移除
2. Mock数据存储在内存中,刷新页面后会重置
3. 某些复杂的业务逻辑可能未完全模拟
4. 如需持久化数据,可以使用localStorage或IndexedDB

## 调试技巧

### 查看Mock请求

打开浏览器控制台的Network选项卡,可以看到所有被Mock拦截的请求。

### 临时禁用Mock

修改 `src/main.js`:

```javascript
// 注释掉这段代码
// if (import.meta.env.MODE === 'development') {
//   import('./mock/browser').then(({ worker }) => {
//     worker.start()
//   })
// }
```

### Mock数据统计

当前Mock数据量:

- 图书: 100本
- 读者: 200人
- 流通记录: 500条
- 预约记录: 100条
- 用户账号: 3个

## 演示场景

### 场景1: 图书管理员日常工作

1. 使用账号 `librarian / lib123` 登录
2. 访问"图书借出"页面,扫描读者证和图书条码
3. 查看流通记录,处理逾期图书

### 场景2: 系统管理员运营分析

1. 使用账号 `admin / admin123` 登录
2. 访问"借阅概览"查看实时数据
3. 访问"数据分析"查看趋势图表
4. 查看图书和读者排行榜

### 场景3: 读者办证

1. 使用账号 `operator / op123` 登录
2. 访问"读者证办理"页面
3. 填写读者信息,上传照片
4. 人脸识别注册(Mock模拟)

## 常见问题

### Q: 为什么API请求没有被Mock?

A: 检查以下几点:
1. 确认开发服务器已启动
2. 检查浏览器控制台是否有 `[MSW] Mock Service Worker started` 提示
3. 确认请求URL与Mock handlers中的路径匹配

### Q: 如何查看Mock返回的数据?

A: 打开浏览器控制台的Network选项卡,点击具体的请求,查看Response内容。

### Q: Mock数据可以修改吗?

A: 可以通过API修改,但刷新页面后会重置。如需持久化,需要自行实现存储逻辑。

## 技术栈

- [MSW (Mock Service Worker)](https://mswjs.io/) - API模拟框架
- [@faker-js/faker](https://fakerjs.dev/) - 假数据生成器
- dayjs - 日期处理库

## 相关文档

- [MSW 官方文档](https://mswjs.io/docs/)
- [Faker.js 官方文档](https://fakerjs.dev/guide/)
