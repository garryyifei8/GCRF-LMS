# Pinia Stores 快速开始

## 5分钟快速上手

### 1. 安装持久化插件（可选但推荐）

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/web-admin
npm install pinia-plugin-persistedstate
```

### 2. 修改 main.js

在 `src/main.js` 中添加持久化插件：

```javascript
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// 创建Pinia实例并配置持久化插件
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 注册 Pinia
app.use(pinia)
```

### 3. 在组件中使用

```vue
<script setup>
import { useBookStore, useReaderStore } from '@/stores'

const bookStore = useBookStore()
const readerStore = useReaderStore()

// 加载数据
onMounted(async () => {
  await bookStore.loadCategories()
  await readerStore.loadReaderTypes()
})
</script>

<template>
  <!-- 使用分类下拉框 -->
  <el-select v-model="categoryCode">
    <el-option
      v-for="opt in bookStore.categoryOptions"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>

  <!-- 使用读者类型下拉框 -->
  <el-select v-model="readerType">
    <el-option
      v-for="opt in readerStore.readerTypeOptions"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>
</template>
```

## 常用功能

### 图书管理 (bookStore)

```javascript
import { useBookStore } from '@/stores'
const bookStore = useBookStore()

// 加载分类
await bookStore.loadCategories()

// 获取分类名称
bookStore.getCategoryName('TP') // "计算机科学"

// 添加搜索历史
bookStore.addSearchHistory('JavaScript')

// 记录最近访问
bookStore.addRecentBook(book)
```

### 读者管理 (readerStore)

```javascript
import { useReaderStore } from '@/stores'
const readerStore = useReaderStore()

// 加载读者类型
await readerStore.loadReaderTypes()

// 设置当前读者
readerStore.setCurrentReader(reader)

// 检查借阅能力
const capacity = readerStore.currentReaderBorrowCapacity
if (capacity.canBorrow) {
  console.log(`还可以借 ${capacity.remainingCount} 本书`)
}
```

### 流通管理 (circulationStore)

```javascript
import { useCirculationStore } from '@/stores'
const circulationStore = useCirculationStore()

// 借书流程
circulationStore.addToBorrowCart(book)
circulationStore.startBorrow(reader)
// ... 调用API
circulationStore.endBorrow(true)

// 计算罚款
const fine = circulationStore.calculateOverdueFine(5) // 5天
console.log(fine) // 2.5元
```

### 系统配置 (systemStore)

```javascript
import { useSystemStore } from '@/stores'
const systemStore = useSystemStore()

// 切换主题
systemStore.toggleTheme()

// 获取借阅规则
const rules = systemStore.getBorrowRuleByType('student')
console.log(rules.maxBorrowCount) // 5
```

## 完整文档

- 使用文档: `doc/stores-usage.md`
- 实战示例: `doc/stores-example.md`
- 持久化配置: `doc/pinia-persist-setup.md`
- 开发总结: `doc/stores-README.md`

## 立即测试

在浏览器控制台运行：

```javascript
import { useBookStore } from '@/stores'
const bookStore = useBookStore()

// 添加搜索历史
bookStore.addSearchHistory('测试')

// 刷新页面
location.reload()

// 检查是否持久化成功
const bookStore = useBookStore()
console.log(bookStore.searchHistory) // 应该包含 ['测试']
```

完成！现在可以在项目中使用Pinia Stores了。
