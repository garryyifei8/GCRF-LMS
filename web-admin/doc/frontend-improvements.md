# GCRF图书馆管理系统 - 前端错误处理与UI优化文档

**创建日期**: 2025-12-01
**版本**: 1.0.0
**负责人**: Frontend Development Team

---

## 概述

本文档记录了GCRF图书馆管理系统前端的错误处理机制和UI优化改进，主要包括统一错误处理、Loading组件库、以及图书编目和库存盘点功能的完善。

---

## 1. 统一错误处理系统

### 1.1 errorHandler.js

**文件路径**: `/web-admin/src/utils/errorHandler.js`

#### 功能特性

1. **多层次错误分类**
   - 业务错误 (BUSINESS)
   - 网络错误 (NETWORK)
   - 认证错误 (AUTH)
   - 权限错误 (PERMISSION)
   - 验证错误 (VALIDATION)
   - 系统错误 (SYSTEM)
   - 超时错误 (TIMEOUT)
   - 未知错误 (UNKNOWN)

2. **HTTP状态码映射**

   ```javascript
   const HTTP_STATUS_MAP = {
     400: { type: ErrorType.VALIDATION, message: '请求参数错误' },
     401: { type: ErrorType.AUTH, message: '登录已过期，请重新登录' },
     403: { type: ErrorType.PERMISSION, message: '权限不足，无法访问' }
     // ... 更多映射
   }
   ```

3. **业务错误码映射**
   - 1000-1999: 通用错误
   - 2000-2999: 认证错误
   - 3000-3999: 权限错误
   - 4000-4999: 读者模块错误
   - 5000-5999: 图书模块错误
   - 6000-6999: 流通模块错误

4. **智能错误处理**
   - 认证错误自动跳转登录
   - 权限错误自动返回首页
   - 网络错误友好提示
   - 验证错误详细展示

#### 核心方法

```javascript
// 处理响应错误
errorHandler.handleResponseError(error)

// 处理业务错误码
errorHandler.handleBusinessCode(response)

// 全局错误处理（Vue errorHandler）
globalErrorHandler(error, vm, info)

// Promise错误处理（window.onunhandledrejection）
unhandledRejectionHandler(event)
```

#### 使用示例

```javascript
import { errorHandler } from '@/utils/errorHandler'

// 在request.js中集成
service.interceptors.response.use(
  async (response) => {
    // 处理业务错误码
    return errorHandler.handleBusinessCode(response.data)
  },
  async (error) => {
    // 处理响应错误
    return errorHandler.handleResponseError(error)
  }
)
```

---

## 2. Loading组件库

### 2.1 SkeletonLoader 骨架屏组件

**文件路径**: `/web-admin/src/components/Loading/SkeletonLoader.vue`

#### Props

| 属性     | 类型    | 默认值  | 说明                                        |
| -------- | ------- | ------- | ------------------------------------------- |
| type     | String  | 'table' | 骨架屏类型: table, card, form, list, custom |
| rows     | Number  | 5       | 行数                                        |
| columns  | Number  | 6       | 列数（仅table类型）                         |
| animated | Boolean | true    | 是否显示动画                                |

#### 使用示例

```vue
<template>
  <!-- 表格骨架屏 -->
  <SkeletonLoader v-if="loading" type="table" :rows="10" :columns="8" />

  <!-- 卡片骨架屏 -->
  <SkeletonLoader type="card" />

  <!-- 表单骨架屏 -->
  <SkeletonLoader type="form" :rows="6" />

  <!-- 列表骨架屏 -->
  <SkeletonLoader type="list" :rows="8" />
</template>
```

### 2.2 PageLoading 页面加载组件

**文件路径**: `/web-admin/src/components/Loading/PageLoading.vue`

#### Props

| 属性       | 类型          | 默认值                     | 说明           |
| ---------- | ------------- | -------------------------- | -------------- |
| fullscreen | Boolean       | false                      | 是否全屏显示   |
| mask       | Boolean       | true                       | 是否显示遮罩   |
| text       | String        | '加载中...'                | 加载文本       |
| tip        | String        | ''                         | 提示信息       |
| icon       | Object/String | null                       | 自定义图标组件 |
| background | String        | 'rgba(255, 255, 255, 0.9)' | 背景色         |

#### 使用示例

```vue
<template>
  <!-- 全屏加载 -->
  <PageLoading fullscreen text="加载中..." tip="请稍候..." />

  <!-- 局部加载 -->
  <div style="position: relative; height: 400px">
    <PageLoading text="数据加载中..." />
  </div>
</template>
```

### 2.3 ButtonLoading 按钮加载组件

**文件路径**: `/web-admin/src/components/Loading/ButtonLoading.vue`

#### Props

| 属性 | 类型   | 默认值    | 说明                        |
| ---- | ------ | --------- | --------------------------- |
| text | String | ''        | 加载文本                    |
| size | String | 'default' | 大小: small, default, large |

#### 使用示例

```vue
<template>
  <el-button type="primary" :loading="submitting" :disabled="submitting">
    <ButtonLoading v-if="submitting" text="保存中..." size="default" />
    <span v-else>保存</span>
  </el-button>
</template>
```

---

## 3. request.js 增强

### 3.1 Token自动刷新

**文件路径**: `/web-admin/src/utils/request.js`

#### 功能特性

1. **Token刷新机制**
   - 检测到401错误自动刷新Token
   - 防止并发请求重复刷新
   - 刷新失败自动跳转登录

2. **请求队列管理**
   - 刷新Token时将请求加入队列
   - Token刷新成功后重试队列请求
   - 刷新失败时清空队列

3. **统一错误处理集成**
   - 业务错误码统一处理
   - HTTP状态码智能映射
   - 网络错误友好提示

#### 核心逻辑

```javascript
// Token刷新
const refreshToken = async () => {
  const userStore = useUserStore()
  try {
    // 调用刷新Token的API
    const res = await axios.post('/api/auth/refresh', {
      refreshToken: userStore.refreshToken
    })
    userStore.setToken(res.data.token)
    return res.data.token
  } catch (error) {
    userStore.logout()
    router.push({ name: 'Login' })
    throw error
  }
}

// 响应拦截器
service.interceptors.response.use(
  async (response) => {
    // Token过期，尝试刷新
    if (res.code === 401 && !response.config._retry) {
      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await refreshToken()
        // 重试当前请求和队列请求
        retryRequestsQueue(newToken)
      }
    }
    // 使用统一错误处理器
    return errorHandler.handleBusinessCode(res)
  },
  async (error) => {
    // 使用统一错误处理器
    return errorHandler.handleResponseError(error)
  }
)
```

---

## 4. 图书编目功能完善

### 4.1 catalog.vue 增强

**文件路径**: `/web-admin/src/views/books/catalog.vue`

#### 主要改进

1. **ISBN智能查询**
   - 实时调用第三方API查询图书信息
   - 自动填充图书元数据（书名、作者、出版社等）
   - 查询失败智能提示，可切换手工编目

2. **分类动态加载**

   ```javascript
   // 从API加载分类树
   const loadCategories = async () => {
     const res = await getCategoryTree()
     categoryOptions.value = flattenCategories(res.data)
   }
   ```

3. **表单验证增强**
   - ISBN格式验证（支持ISBN-10和ISBN-13）
   - 必填字段实时校验
   - 提交前完整性检查

4. **用户体验优化**
   - 智能编目/手工编目双模式
   - ISBN查询Loading状态
   - 保存Loading状态
   - 保存成功提示并自动跳转

#### API集成

```javascript
// 新增API方法
import {
  searchBookByISBN, // ISBN查询
  createBook, // 创建图书
  getCategoryTree, // 获取分类树
  generateBarcodes // 生成条码
} from '@/api/books'

// ISBN查询
const handleISBNSearch = async () => {
  try {
    const res = await searchBookByISBN(isbn)
    Object.assign(bookForm, res.data)
    ElMessage.success('图书信息获取成功')
  } catch (error) {
    ElMessageBox.confirm('未查询到图书信息，是否切换到手工编目模式？')
  }
}

// 提交表单
const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await createBook(submitData)
    ElMessage.success('图书添加成功')
    router.push('/books/list')
  } finally {
    submitting.value = false
  }
}
```

---

## 5. 库存盘点功能完善

### 5.1 inventory.vue 增强

**文件路径**: `/web-admin/src/views/books/inventory.vue`

#### 主要改进

1. **盘点任务管理**
   - 创建盘点任务（全部/分类/位置/条码范围）
   - 开始/暂停/恢复/完成任务
   - 任务进度实时显示
   - 差异自动统计

2. **分页支持**

   ```javascript
   const pagination = reactive({
     pageNum: 1,
     pageSize: 10,
     total: 0
   })

   const handlePageChange = (page) => {
     pagination.pageNum = page
     loadTaskList()
   }
   ```

3. **详情对话框**
   - 任务基本信息展示
   - 差异明细表格
   - 差异类型分类（缺失/多余/状态不符）
   - 加载状态优化

4. **报告导出**
   ```javascript
   const handleExportReport = async (row) => {
     const blob = await exportInventoryReport(row.id)
     // 创建下载链接
     const url = window.URL.createObjectURL(blob)
     const link = document.createElement('a')
     link.download = `盘点报告_${row.taskNo}.xlsx`
     link.click()
   }
   ```

#### API集成

```javascript
import {
  getInventoryTasks, // 获取任务列表
  createInventoryTask, // 创建任务
  startInventoryTask, // 开始任务
  pauseInventoryTask, // 暂停任务
  resumeInventoryTask, // 恢复任务
  exportInventoryReport, // 导出报告
  getInventoryDiffs // 获取差异明细
} from '@/api/inventory'
```

### 5.2 inventory.js API

**文件路径**: `/web-admin/src/api/inventory.js`

#### 提供的API方法

| 方法                  | 说明                     |
| --------------------- | ------------------------ |
| getInventoryTasks     | 获取盘点任务列表（分页） |
| getInventoryTaskById  | 获取任务详情             |
| createInventoryTask   | 创建盘点任务             |
| updateInventoryTask   | 更新任务                 |
| deleteInventoryTask   | 删除任务                 |
| startInventoryTask    | 开始盘点                 |
| pauseInventoryTask    | 暂停盘点                 |
| resumeInventoryTask   | 恢复盘点                 |
| completeInventoryTask | 完成盘点                 |
| cancelInventoryTask   | 取消盘点                 |
| exportInventoryReport | 导出报告                 |
| getInventoryDiffs     | 获取差异明细             |
| recordInventoryResult | 记录盘点结果             |

---

## 6. 最佳实践

### 6.1 错误处理规范

```javascript
// ✅ 推荐：使用统一错误处理
try {
  const res = await someAPI()
  // 业务逻辑
} catch (error) {
  // 错误已由errorHandler统一处理
  console.error('操作失败:', error)
}

// ❌ 不推荐：手动处理每个错误
try {
  const res = await someAPI()
} catch (error) {
  if (error.status === 401) {
    ElMessage.error('登录过期')
    router.push('/login')
  } else if (error.status === 403) {
    ElMessage.error('权限不足')
  }
  // ... 重复代码
}
```

### 6.2 Loading状态规范

```javascript
// ✅ 推荐：使用专用Loading组件
const loading = ref(false)

// 骨架屏（首次加载）
<SkeletonLoader v-if="loading && list.length === 0" />

// 表格Loading（刷新数据）
<el-table v-loading="loading" :data="list" />

// 按钮Loading（提交操作）
<el-button :loading="submitting">
  <ButtonLoading v-if="submitting" text="保存中..." />
  <span v-else>保存</span>
</el-button>
```

### 6.3 API调用规范

```javascript
// ✅ 推荐：清晰的loading状态管理
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDataAPI()
    if (res.code === 200) {
      list.value = res.data.records
    }
  } catch (error) {
    // 错误已由errorHandler处理
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false // 确保loading状态正确恢复
  }
}
```

### 6.4 用户交互规范

```javascript
// ✅ 推荐：清晰的用户反馈
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteAPI(row.id)
        ElMessage.success('删除成功')
        loadData() // 刷新列表
      } catch (error) {
        // 错误已由errorHandler处理
      }
    })
    .catch(() => {}) // 用户取消
}
```

---

## 7. 文件清单

### 新增文件

```
web-admin/
├── src/
│   ├── utils/
│   │   └── errorHandler.js                 # 统一错误处理器
│   ├── components/
│   │   └── Loading/
│   │       ├── SkeletonLoader.vue          # 骨架屏组件
│   │       ├── PageLoading.vue             # 页面加载组件
│   │       ├── ButtonLoading.vue           # 按钮加载组件
│   │       └── index.js                    # 组件导出
│   └── api/
│       └── inventory.js                    # 盘点API
└── doc/
    └── frontend-improvements.md            # 本文档
```

### 修改文件

```
web-admin/
├── src/
│   ├── utils/
│   │   └── request.js                      # 集成errorHandler和Token刷新
│   ├── api/
│   │   └── books.js                        # 新增ISBN查询等API
│   └── views/
│       └── books/
│           ├── catalog.vue                 # 完善图书编目功能
│           └── inventory.vue               # 完善库存盘点功能
```

---

## 8. 后续计划

### 8.1 功能增强

- [ ] 实现批量导入图书功能
- [ ] 添加图书二维码生成与打印
- [ ] 完善盘点任务调度（定时任务）
- [ ] 实现差异处理工作流

### 8.2 性能优化

- [ ] 大列表虚拟滚动优化
- [ ] 图片懒加载与预加载
- [ ] API请求去重与缓存
- [ ] 组件按需加载

### 8.3 用户体验

- [ ] 添加操作引导（新手导览）
- [ ] 快捷键支持
- [ ] 离线缓存支持
- [ ] 主题定制功能

---

## 9. 技术栈

- **框架**: Vue 3 (Composition API)
- **UI库**: Element Plus
- **HTTP客户端**: Axios
- **状态管理**: Pinia
- **路由**: Vue Router
- **构建工具**: Vite

---

## 10. 联系方式

**问题反馈**: 请提交到项目Issue
**文档更新**: 2025-12-01
**维护团队**: GCRF Frontend Team

---

**注意事项**:

1. 所有错误处理已统一，避免重复编写错误处理逻辑
2. Loading组件已统一，保持视觉一致性
3. API调用需遵循最佳实践，确保用户体验
4. 后端API接口需按照约定返回标准格式
