# Mock API 集成进度追踪

## 已完成的Mock API模块 ✅

### 1. 认证模块 (Auth)
**文件**: `mock/msw/handlers/auth.ts`, `src/api/auth.js`
- ✅ POST `/api/v1/auth/login` - 用户登录
- ✅ POST `/api/v1/auth/logout` - 用户登出
- ✅ POST `/api/v1/auth/refresh` - 刷新Token
- ✅ GET `/api/v1/auth/user/info` - 获取用户信息
- ✅ PUT `/api/v1/auth/password` - 修改密码

**对接页面**: `src/views/login/index.vue`

### 2. 分析统计模块 (Analytics)
**文件**: `mock/msw/handlers/analytics.ts`, `src/api/analytics.js`
- ✅ GET `/api/v1/analytics/overview` - 概览统计
- ✅ GET `/api/v1/analytics/borrow-trends` - 借阅趋势
- ✅ GET `/api/v1/analytics/category-stats` - 分类统计
- ✅ GET `/api/v1/analytics/book-rankings` - 图书排行榜
- ✅ GET `/api/v1/analytics/reader-rankings` - 活跃读者排行
- ✅ GET `/api/v1/analytics/collection-analysis` - 馆藏资源分析
- ✅ GET `/api/v1/analytics/recent-activities` - 最近活动

**对接页面**: `src/views/dashboard/index.vue`

### 3. 系统管理模块 (System)
**文件**: `mock/msw/handlers/system.ts`, `src/api/system.js`
- ✅ GET `/api/v1/system/departments` - 部门列表(分页+搜索)
- ✅ GET `/api/v1/system/departments/tree` - 部门树形结构
- ✅ GET `/api/v1/system/departments/:id` - 部门详情
- ✅ POST `/api/v1/system/departments` - 创建部门
- ✅ PUT `/api/v1/system/departments/:id` - 更新部门
- ✅ DELETE `/api/v1/system/departments/:id` - 删除部门
- ✅ DELETE `/api/v1/system/departments/batch` - 批量删除

**对接页面**: `src/views/system/departments.vue`

---

## 待实现的Mock API模块 ⏳

### 4. 图书管理模块 (Books) - 高优先级
**需要创建**: `mock/msw/handlers/books.ts`, `src/api/books.js`

#### API端点设计:
```
GET    /api/v1/books              - 图书列表(分页+搜索)
GET    /api/v1/books/:id          - 图书详情
POST   /api/v1/books              - 新增图书
PUT    /api/v1/books/:id          - 更新图书
DELETE /api/v1/books/:id          - 删除图书
DELETE /api/v1/books/batch        - 批量删除
GET    /api/v1/books/categories   - 图书分类列表
POST   /api/v1/books/import       - 导入图书数据
```

**对接页面**:
- `src/views/books/list.vue` - 图书列表
- `src/views/books/catalog.vue` - 图书编目
- `src/views/books/collection.vue` - 馆藏管理
- `src/views/books/inventory.vue` - 盘点管理

### 5. 读者管理模块 (Readers) - 高优先级
**需要创建**: `mock/msw/handlers/readers.ts`, `src/api/readers.js`

#### API端点设计:
```
GET    /api/v1/readers            - 读者列表(分页+搜索)
GET    /api/v1/readers/:id        - 读者详情
POST   /api/v1/readers            - 新增读者
PUT    /api/v1/readers/:id        - 更新读者
DELETE /api/v1/readers/:id        - 删除读者
DELETE /api/v1/readers/batch      - 批量删除
GET    /api/v1/readers/types      - 读者类型列表
POST   /api/v1/readers/:id/card   - 办理借阅证
```

**对接页面**:
- `src/views/readers/students.vue` - 学生读者
- `src/views/readers/teachers.vue` - 教师读者
- `src/views/readers/card.vue` - 借阅证管理

### 6. 流通管理模块 (Circulation) - 高优先级
**需要创建**: `mock/msw/handlers/circulation.ts`, `src/api/circulation.js`

#### API端点设计:
```
POST   /api/v1/circulation/borrow      - 图书借出
POST   /api/v1/circulation/return      - 图书归还
POST   /api/v1/circulation/renew       - 续借
GET    /api/v1/circulation/records     - 流通记录(分页)
GET    /api/v1/circulation/reservations - 预约列表
POST   /api/v1/circulation/reserve     - 预约图书
PUT    /api/v1/circulation/reserve/:id - 处理预约
GET    /api/v1/circulation/overdue     - 逾期记录
```

**对接页面**:
- `src/views/circulation/borrow.vue` - 图书借出
- `src/views/circulation/return.vue` - 图书归还
- `src/views/circulation/records.vue` - 流通记录
- `src/views/circulation/reservations.vue` - 预约管理

### 7. 用户管理模块 (Users) - 中优先级
**需要创建**: `mock/msw/handlers/users.ts` (或扩展system.ts)

#### API端点设计:
```
GET    /api/v1/system/users       - 用户列表
GET    /api/v1/system/users/:id   - 用户详情
POST   /api/v1/system/users       - 创建用户
PUT    /api/v1/system/users/:id   - 更新用户
DELETE /api/v1/system/users/:id   - 删除用户
```

**对接页面**:
- `src/views/system/users.vue` - 用户管理

### 8. AI功能模块 (AI) - 低优先级
**需要创建**: `mock/msw/handlers/ai.ts`, `src/api/ai.js`

#### API端点设计:
```
GET    /api/v1/ai/recommend       - AI推荐图书
POST   /api/v1/ai/chat            - AI对话
GET    /api/v1/ai/analytics       - AI分析
```

**对接页面**:
- `src/views/ai/recommend.vue` - AI推荐
- `src/views/ai/chat.vue` - AI助手
- `src/views/ai/analytics.vue` - AI分析

---

## 实施计划

### 阶段1: 核心业务模块 (当前阶段)
1. ✅ Dashboard页面Mock API - 已完成
2. ⏳ 图书管理Mock API - 进行中
3. ⏳ 读者管理Mock API
4. ⏳ 流通管理Mock API

### 阶段2: 系统管理模块
5. ⏳ 用户管理Mock API
6. ✅ 部门管理Mock API - 已完成

### 阶段3: 扩展功能模块
7. ⏳ AI功能Mock API
8. ⏳ 系统配置Mock API
9. ⏳ 角色权限Mock API

---

## Mock数据规划

### 图书数据 (Books)
- 100+ 图书记录
- 包含: ISBN, 标题, 作者, 出版社, 分类, 价格, 库存等
- 已生成: `mock/data/books.ts`

### 读者数据 (Readers)
- 需要生成: 学生读者(50条) + 教师读者(20条)
- 包含: 读者编号, 姓名, 类型, 院系, 年级, 联系方式等
- 文件: `mock/data/readers.ts` (待创建)

### 流通数据 (Circulation)
- 需要生成: 借阅记录(200条) + 预约记录(30条)
- 包含: 借阅单号, 读者ID, 图书ID, 借出/归还时间, 状态等
- 文件: `mock/data/circulation.ts` (待创建)

---

## 技术要点

### Mock响应格式统一
所有API响应使用统一格式:
```typescript
{
  code: 200,
  message: 'success',
  data: {...},
  timestamp: 1234567890,
  traceId: 'uuid-xxxx'
}
```

### 分页响应格式
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

### Mock数据持久化
- 使用内存存储,页面刷新后重置
- 支持CRUD操作的实时反映
- 延迟300ms模拟网络请求

---

## 下一步行动

1. **创建图书管理Mock API** - `books.ts` + `api/books.js`
2. **创建读者管理Mock API** - `readers.ts` + `api/readers.js`
3. **创建流通管理Mock API** - `circulation.ts` + `api/circulation.js`
4. **逐页面对接Mock API** - 修改Vue组件调用API
5. **测试验证** - 确保所有功能正常工作

---

## 当前进度

**总体进度**: 3/11 模块完成 (27%)

**页面对接进度**:
- ✅ Login (登录)
- ✅ Dashboard (仪表板)
- ✅ Departments (部门管理)
- ⏳ Books (图书管理) - 0/4
- ⏳ Readers (读者管理) - 0/3
- ⏳ Circulation (流通管理) - 0/4
- ⏳ Users (用户管理) - 0/1
- ⏳ AI (AI功能) - 0/3

**下次更新**: 完成图书管理模块后
