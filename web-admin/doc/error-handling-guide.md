# 错误处理与Loading组件使用指南

快速参考指南，帮助开发者正确使用错误处理和Loading组件。

---

## 快速开始

### 1. 导入组件

```javascript
// 错误处理器（已在request.js中集成，无需手动导入）
import { errorHandler } from '@/utils/errorHandler'

// Loading组件
import { SkeletonLoader, PageLoading, ButtonLoading } from '@/components/Loading'
```

---

## 错误处理使用

### 基本用法

```javascript
// API调用（推荐）
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDataAPI(params)
    // 成功处理
    if (res.code === 200) {
      data.value = res.data
    }
  } catch (error) {
    // 错误已由errorHandler统一处理，只需记录日志
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}
```

### 特殊错误处理

```javascript
// 需要自定义错误提示时
try {
  const res = await deleteAPI(id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
  }
} catch (error) {
  // errorHandler已显示通用错误，这里可以添加额外逻辑
  if (error.code === 5003) {
    // 特殊业务逻辑
    ElMessageBox.confirm('图书已被借出，确定要删除吗？', '警告')
  }
}
```

---

## Loading组件使用

### 1. SkeletonLoader（骨架屏）

**适用场景**: 首次加载数据时

```vue
<template>
  <!-- 表格骨架屏 -->
  <SkeletonLoader v-if="loading && list.length === 0" type="table" :rows="10" :columns="8" />

  <!-- 实际内容 -->
  <el-table v-else :data="list" />
</template>

<script setup>
import { SkeletonLoader } from '@/components/Loading'

const loading = ref(false)
const list = ref([])
</script>
```

**类型选择**:

- `type="table"` - 表格
- `type="card"` - 卡片
- `type="form"` - 表单
- `type="list"` - 列表

### 2. PageLoading（页面加载）

**适用场景**: 整页或区域加载

```vue
<template>
  <!-- 全屏加载 -->
  <PageLoading v-if="initialLoading" fullscreen text="加载中..." tip="请稍候..." />

  <!-- 局部加载 -->
  <div style="position: relative; min-height: 400px">
    <PageLoading v-if="loading" text="数据加载中..." />
    <div v-else>
      <!-- 内容 -->
    </div>
  </div>
</template>

<script setup>
import { PageLoading } from '@/components/Loading'
</script>
```

### 3. ButtonLoading（按钮加载）

**适用场景**: 提交、保存等操作按钮

```vue
<template>
  <el-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
    <ButtonLoading v-if="submitting" text="保存中..." />
    <span v-else>保存</span>
  </el-button>
</template>

<script setup>
import { ButtonLoading } from '@/components/Loading'

const submitting = ref(false)

const handleSubmit = async () => {
  submitting.value = true
  try {
    await submitAPI(data)
    ElMessage.success('保存成功')
  } finally {
    submitting.value = false
  }
}
</script>
```

---

## 完整示例

### 列表页面

```vue
<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <el-form :model="searchForm" inline>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="搜索..." />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <div class="card">
      <!-- 首次加载：骨架屏 -->
      <SkeletonLoader v-if="loading && list.length === 0" type="table" :rows="10" />

      <!-- 数据加载后：表格 -->
      <template v-else>
        <el-table v-loading="loading" :data="list">
          <el-table-column prop="name" label="名称" />
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button link @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)"> 删除 </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          v-model:current-page="pagination.pageNum"
          :total="pagination.total"
          @current-change="loadList"
        />
      </template>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" title="编辑">
      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="submitting"
          @click="handleSubmit"
        >
          <ButtonLoading v-if="submitting" text="保存中..." />
          <span v-else>保存</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SkeletonLoader, ButtonLoading } from '@/components/Loading'
import { getListAPI, updateAPI, deleteAPI } from '@/api/example'

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

// 列表数据
const loading = ref(false)
const list = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 加载列表
const loadList = async () => {
  loading.value = true
  try {
    const res = await getListAPI({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    if (res.code === 200) {
      list.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error('加载列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadList()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

// 编辑
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: ''
})
const submitting = ref(false)

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    submitting.value = true
    const res = await updateAPI(form.id, form)

    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    }
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteAPI(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadList()
        }
      } catch (error) {
        console.error('删除失败:', error)
      }
    })
    .catch(() => {})
}

// 初始化
onMounted(() => {
  loadList()
})
</script>
```

---

## 常见问题

### Q1: 什么时候用SkeletonLoader，什么时候用v-loading？

**A**:

- 首次加载（list.length === 0）：使用 `SkeletonLoader`
- 刷新数据（list.length > 0）：使用 `v-loading`

```vue
<!-- 首次加载 -->
<SkeletonLoader v-if="loading && list.length === 0" />

<!-- 有数据时 -->
<el-table v-else v-loading="loading" :data="list" />
```

### Q2: API调用失败时，还需要手动显示错误提示吗？

**A**: 通常不需要。errorHandler已统一处理并显示错误提示。只在需要特殊业务逻辑时手动处理。

```javascript
// ✅ 推荐：让errorHandler处理
try {
  await someAPI()
} catch (error) {
  console.error('操作失败:', error) // 只记录日志
}

// ❌ 不推荐：重复显示错误
try {
  await someAPI()
} catch (error) {
  ElMessage.error('操作失败') // errorHandler已显示，这里重复了
}
```

### Q3: 按钮Loading状态如何管理？

**A**: 使用独立的ref变量，在finally中确保恢复状态。

```javascript
const submitting = ref(false)

const handleSubmit = async () => {
  submitting.value = true
  try {
    await submitAPI()
  } finally {
    submitting.value = false // 确保无论成功失败都恢复
  }
}
```

---

## 检查清单

开发新功能时，请检查以下项：

- [ ] API调用是否有try-catch-finally？
- [ ] loading状态是否在finally中恢复？
- [ ] 首次加载是否使用SkeletonLoader？
- [ ] 提交按钮是否有loading状态？
- [ ] 提交按钮是否在loading时禁用？
- [ ] 删除操作是否有确认对话框？
- [ ] 成功操作是否有成功提示？
- [ ] 错误是否由errorHandler统一处理？

---

**更新日期**: 2025-12-01
**相关文档**: [frontend-improvements.md](./frontend-improvements.md)
