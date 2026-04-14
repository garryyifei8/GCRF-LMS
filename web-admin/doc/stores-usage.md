# Pinia Stores 使用文档

本文档说明如何在GCRF图书馆管理系统中使用Pinia状态管理。

## 目录

- [概述](#概述)
- [Store列表](#store列表)
- [使用方法](#使用方法)
- [持久化说明](#持久化说明)
- [最佳实践](#最佳实践)

---

## 概述

项目使用Pinia作为状态管理工具，所有Store都遵循以下规范：

1. 使用Composition API风格 (`defineStore` + setup函数)
2. 明确区分State、Getters、Actions
3. 使用localStorage/sessionStorage进行持久化
4. 实现缓存机制减少API调用
5. 提供reset方法清理数据

---

## Store列表

### 1. useUserStore (`stores/user.js`)

**用途**: 用户认证、权限管理

**主要功能**:

- 用户登录/登出
- 保存用户信息和Token
- 权限检查

**示例**:

```javascript
import { useUserStore } from '@/stores'

const userStore = useUserStore()

// 登录
await userStore.login({ username: 'admin', password: '123456' })

// 获取用户信息
console.log(userStore.userInfo)

// 检查权限
if (userStore.hasPermission('book:create')) {
  // ...
}

// 登出
await userStore.logout()
```

**持久化**: localStorage (key: 'user')

---

### 2. useBookStore (`stores/book.js`)

**用途**: 图书相关数据缓存

**主要功能**:

- 图书分类缓存 (5分钟有效期)
- 热门图书缓存 (5分钟有效期)
- 最近访问的图书 (最多20条)
- 搜索历史 (最多10条)

**示例**:

```javascript
import { useBookStore } from '@/stores'

const bookStore = useBookStore()

// 加载图书分类
await bookStore.loadCategories()
console.log(bookStore.categories)

// 获取分类选项（下拉框）
const options = bookStore.categoryOptions

// 根据分类编码获取名称
const categoryName = bookStore.getCategoryName('TP')

// 加载热门图书
await bookStore.loadHotBooks(10)

// 添加最近访问
bookStore.addRecentBook({
  id: 1,
  title: 'JavaScript高级程序设计',
  author: 'Nicholas C. Zakas',
  isbn: '978-7-115-27579-0'
})

// 添加搜索历史
bookStore.addSearchHistory('JavaScript')

// 强制刷新缓存
await bookStore.loadCategories(true)
```

**持久化**: localStorage (key: 'book')

- 只持久化: recentBooks, searchHistory
- 不持久化: categories, hotBooks (缓存数据)

---

### 3. useReaderStore (`stores/reader.js`)

**用途**: 读者相关数据缓存和操作状态

**主要功能**:

- 读者类型配置缓存 (10分钟有效期)
- 当前操作读者信息
- 最近操作的读者 (最多20条)
- 搜索历史 (最多10条)
- 计算读者借阅能力

**示例**:

```javascript
import { useReaderStore } from '@/stores'

const readerStore = useReaderStore()

// 加载读者类型
await readerStore.loadReaderTypes()
console.log(readerStore.readerTypes)

// 获取读者类型选项（下拉框）
const typeOptions = readerStore.readerTypeOptions

// 根据类型编码获取类型信息
const typeInfo = readerStore.getReaderTypeByCode('student')

// 设置当前操作的读者（用于借还书）
readerStore.setCurrentReader({
  id: 1,
  cardNumber: 'R001',
  realName: '张三',
  readerType: 'student',
  maxBorrowCount: 5,
  currentBorrowCount: 2
})

// 检查读者是否可借书
const capacity = readerStore.currentReaderBorrowCapacity
console.log(capacity.canBorrow) // true
console.log(capacity.remainingCount) // 3

// 清除当前读者
readerStore.clearCurrentReader()

// 添加搜索历史
readerStore.addSearchHistory('张三')
```

**持久化**: localStorage (key: 'reader')

- 只持久化: searchHistory, recentReaders
- 不持久化: readerTypes (缓存数据), currentReader (临时状态)

---

### 4. useCirculationStore (`stores/circulation.js`)

**用途**: 流通业务操作状态管理

**主要功能**:

- 借阅购物车（批量借书）
- 归还购物车（批量还书）
- 借阅/归还操作状态
- 罚款规则配置
- 借阅规则配置
- 罚款计算

**示例**:

```javascript
import { useCirculationStore } from '@/stores'

const circulationStore = useCirculationStore()

// 添加图书到借阅购物车
circulationStore.addToBorrowCart({
  id: 1,
  title: 'JavaScript高级程序设计',
  author: 'Nicholas C. Zakas',
  isbn: '978-7-115-27579-0',
  barcode: 'BC001'
})

console.log(circulationStore.borrowCartCount) // 1

// 开始借阅流程
circulationStore.startBorrow(readerInfo)

// 完成借阅
circulationStore.endBorrow(true)

// 添加借阅记录到归还购物车
circulationStore.addToReturnCart({
  id: 100,
  bookId: 1,
  bookTitle: 'JavaScript高级程序设计',
  barcode: 'BC001',
  borrowDate: '2024-10-01',
  dueDate: '2024-10-31',
  isOverdue: true,
  overdueDays: 5
})

// 计算逾期罚款
const fine = circulationStore.calculateOverdueFine(5)
console.log(fine) // 2.5 (5天 * 0.5元/天)

// 加载罚款规则
await circulationStore.loadFineRules()

// 更新罚款规则
circulationStore.updateFineRules({
  overduePerDay: 1.0 // 修改为1元/天
})

// 清空购物车
circulationStore.clearBorrowCart()
circulationStore.clearReturnCart()
```

**持久化**: sessionStorage (key: 'circulation')

- 只持久化: borrowCart, returnCart
- 不持久化: 操作状态、规则配置
- 使用sessionStorage: 关闭标签页即清除，避免数据残留

---

### 5. useSystemStore (`stores/system.js`)

**用途**: 系统配置、角色权限等全局数据

**主要功能**:

- 系统配置（主题、分页、格式等）
- 角色列表缓存 (30分钟有效期)
- 权限树缓存 (30分钟有效期)
- 借阅规则配置（按读者类型）
- 罚款规则配置

**示例**:

```javascript
import { useSystemStore } from '@/stores'

const systemStore = useSystemStore()

// 获取系统配置
console.log(systemStore.config.systemName)
console.log(systemStore.config.pageSize) // 20

// 更新配置
systemStore.updateConfig({
  pageSize: 50,
  theme: 'dark'
})

// 切换主题
systemStore.toggleTheme()

// 切换侧边栏
systemStore.toggleSidebar()

// 加载角色列表
await systemStore.loadRoles()
console.log(systemStore.roles)

// 获取角色选项（下拉框）
const roleOptions = systemStore.roleOptions

// 根据角色编码获取名称
const roleName = systemStore.getRoleName('ADMIN')

// 加载权限树
await systemStore.loadPermissionTree()

// 根据读者类型获取借阅规则
const studentRules = systemStore.getBorrowRuleByType('student')
console.log(studentRules.maxBorrowCount) // 5
console.log(studentRules.borrowDays) // 30

// 更新借阅规则
systemStore.updateBorrowRules('student', {
  maxBorrowCount: 10
})

// 更新罚款规则
systemStore.updateFineRules({
  overduePerDay: 1.0
})

// 重置配置
systemStore.resetConfig()
```

**持久化**: localStorage (key: 'system')

- 只持久化: config (系统配置)
- 不持久化: roles, permissionTree, 业务规则 (缓存数据)

---

## 使用方法

### 1. 在组件中使用

```vue
<script setup>
import { useBookStore, useReaderStore } from '@/stores'

const bookStore = useBookStore()
const readerStore = useReaderStore()

// 在 onMounted 中加载数据
onMounted(async () => {
  await bookStore.loadCategories()
  await readerStore.loadReaderTypes()
})
</script>

<template>
  <el-select v-model="categoryCode">
    <el-option
      v-for="opt in bookStore.categoryOptions"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>
</template>
```

### 2. 在API请求前后使用

```javascript
// 搜索图书前添加搜索历史
async function searchBooks(keyword) {
  bookStore.addSearchHistory(keyword)

  const response = await getBooks({ keyword })
  return response.data
}

// 查看图书详情时记录访问
async function viewBookDetail(bookId) {
  const book = await getBookById(bookId)
  bookStore.addRecentBook(book.data)
  return book.data
}
```

### 3. 在借还书流程中使用

```javascript
// 借书流程
async function borrowBooks(reader) {
  const circulationStore = useCirculationStore()

  try {
    // 开始借阅流程
    circulationStore.startBorrow(reader)

    // 调用借书API
    const books = circulationStore.borrowOperation.books
    for (const book of books) {
      await borrowBook({ readerId: reader.id, bookId: book.id })
    }

    // 成功完成
    circulationStore.endBorrow(true)
    ElMessage.success('借书成功')
  } catch (error) {
    // 失败，保留购物车
    circulationStore.endBorrow(false)
    ElMessage.error('借书失败')
  }
}
```

---

## 持久化说明

### 持久化策略

项目使用自定义的persist配置（需要配合pinia-plugin-persistedstate插件）：

```javascript
{
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'store-name',      // localStorage key
        storage: localStorage,   // 或 sessionStorage
        paths: ['field1', 'field2'] // 只持久化指定字段
      }
    ]
  }
}
```

### 持久化分类

| Store       | 存储方式       | 持久化字段                   | 说明                         |
| ----------- | -------------- | ---------------------------- | ---------------------------- |
| user        | localStorage   | 全部字段                     | 用户信息需要跨会话保存       |
| book        | localStorage   | recentBooks, searchHistory   | 只保存用户历史，缓存不持久化 |
| reader      | localStorage   | searchHistory, recentReaders | 只保存用户历史               |
| circulation | sessionStorage | borrowCart, returnCart       | 购物车仅在会话期间有效       |
| system      | localStorage   | config                       | 只保存系统配置               |

### 安装持久化插件

如果需要持久化功能，需要安装pinia-plugin-persistedstate:

```bash
npm install pinia-plugin-persistedstate
```

在 `main.js` 中配置:

```javascript
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
```

**注意**: 目前项目中userStore已经使用了persist配置，但可能还未安装插件。如果需要持久化功能，请按上述步骤安装。

---

## 最佳实践

### 1. 缓存策略

**自动缓存**: Store会自动缓存数据，并设置有效期。在有效期内重复调用load方法会直接返回缓存。

```javascript
// 第一次调用，会请求API
await bookStore.loadCategories()

// 5分钟内再次调用，直接返回缓存，不请求API
await bookStore.loadCategories()

// 强制刷新
await bookStore.loadCategories(true)
```

**缓存有效期**:

- bookStore: 5分钟
- readerStore: 10分钟
- systemStore: 30分钟

### 2. 何时使用Store

**应该使用Store的场景**:

- 需要在多个页面共享的数据
- 需要缓存以减少API调用
- 需要跨组件传递的操作状态
- 需要持久化的用户偏好

**不应该使用Store的场景**:

- 只在单个组件内使用的数据
- 实时性要求极高的数据
- 频繁变化的临时数据
- 大量列表数据（应该分页加载）

### 3. 清理数据

**登出时清理**:

```javascript
async function logout() {
  const userStore = useUserStore()
  const bookStore = useBookStore()
  const readerStore = useReaderStore()
  const circulationStore = useCirculationStore()
  const systemStore = useSystemStore()

  // 调用各Store的reset方法
  bookStore.reset()
  readerStore.reset()
  circulationStore.reset()
  systemStore.reset()

  // 最后登出
  await userStore.logout()
}
```

**切换用户时清理**:

```javascript
// 切换用户前清理业务数据，保留系统配置
bookStore.clearCache()
readerStore.clearCurrentReader()
circulationStore.reset()
```

### 4. 错误处理

```javascript
try {
  await bookStore.loadCategories()
} catch (error) {
  console.error('加载分类失败:', error)
  ElMessage.error('加载分类失败，请重试')

  // 可以使用默认值或空数组
  bookStore.categories = []
}
```

### 5. TypeScript支持

如果项目使用TypeScript，可以为Store定义类型：

```typescript
import type { Store } from 'pinia'

export interface BookState {
  categories: Category[]
  categoriesLoadedAt: number | null
  categoriesLoading: boolean
  // ...
}

export type BookStore = Store<'book', BookState, BookGetters, BookActions>
```

---

## 调试技巧

### 1. 在Vue DevTools中查看

安装Vue DevTools后，可以在Pinia标签中查看所有Store的状态。

### 2. 在控制台中访问

```javascript
// 在浏览器控制台中
import { useBookStore } from '@/stores'
const bookStore = useBookStore()
console.log(bookStore.$state)
```

### 3. 清除持久化数据

```javascript
// 清除localStorage
localStorage.removeItem('book')
localStorage.removeItem('reader')
localStorage.removeItem('user')
localStorage.removeItem('system')

// 清除sessionStorage
sessionStorage.removeItem('circulation')

// 或者清空所有
localStorage.clear()
sessionStorage.clear()
```

---

## 常见问题

### Q: 为什么有些数据不持久化？

A: 缓存数据（如categories、readerTypes）每次都应该从服务器获取最新数据，只在内存中缓存一段时间。只有用户行为历史（如recentBooks、searchHistory）才需要持久化。

### Q: sessionStorage和localStorage有什么区别？

A:

- localStorage: 永久保存，除非手动清除
- sessionStorage: 关闭标签页即清除

circulation使用sessionStorage是因为借还书购物车是临时操作，不应该跨会话保留。

### Q: 如何强制刷新缓存？

A: 所有load方法都支持force参数：

```javascript
await bookStore.loadCategories(true)
await readerStore.loadReaderTypes(true)
```

### Q: 缓存失效后会自动刷新吗？

A: 不会。缓存失效后，下次调用load方法时会重新请求API。

---

## 更新日志

- 2024-11-30: 创建stores/book.js, stores/reader.js, stores/circulation.js, stores/system.js
- 初始版本包含完整的缓存机制、持久化配置和业务逻辑
