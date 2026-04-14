# Pinia Stores 使用示例

本文档提供实际场景下使用Pinia Stores的代码示例。

## 场景1: 图书管理页面

### BookList.vue - 图书列表页面

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useBookStore } from '@/stores'
import { getBooks } from '@/api/books'

const bookStore = useBookStore()

const loading = ref(false)
const tableData = ref([])
const searchForm = ref({
  keyword: '',
  category: '',
  pageNum: 1,
  pageSize: 20
})

// 加载图书列表
async function loadBooks() {
  try {
    loading.value = true

    // 如果有搜索关键词，添加到历史
    if (searchForm.value.keyword) {
      bookStore.addSearchHistory(searchForm.value.keyword)
    }

    const response = await getBooks(searchForm.value)
    tableData.value = response.data.records
  } catch (error) {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

// 查看图书详情
function viewDetail(book) {
  // 记录到最近访问
  bookStore.addRecentBook(book)

  // 跳转到详情页
  router.push({ name: 'BookDetail', params: { id: book.id } })
}

// 页面加载时获取分类
onMounted(async () => {
  try {
    // 加载分类（有缓存，不会重复请求）
    await bookStore.loadCategories()
  } catch (error) {
    console.error('加载分类失败:', error)
  }

  // 加载图书列表
  loadBooks()
})
</script>

<template>
  <div class="book-list">
    <!-- 搜索表单 -->
    <el-form :model="searchForm" inline>
      <el-form-item label="关键词">
        <el-autocomplete
          v-model="searchForm.keyword"
          :fetch-suggestions="searchSuggestions"
          placeholder="书名/作者/ISBN"
        />
      </el-form-item>

      <el-form-item label="分类">
        <el-select v-model="searchForm.category" placeholder="请选择分类">
          <el-option
            v-for="opt in bookStore.categoryOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="loadBooks">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 搜索历史 -->
    <div v-if="bookStore.searchHistory.length > 0" class="search-history">
      <span>搜索历史: </span>
      <el-tag
        v-for="keyword in bookStore.searchHistory"
        :key="keyword"
        @click="
          searchForm.keyword = keyword
          loadBooks()
        "
        style="cursor: pointer; margin-right: 8px"
      >
        {{ keyword }}
      </el-tag>
      <el-button text @click="bookStore.clearSearchHistory">清空</el-button>
    </div>

    <!-- 图书列表 -->
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="title" label="书名" />
      <el-table-column prop="author" label="作者" />
      <el-table-column prop="category" label="分类">
        <template #default="{ row }">
          {{ bookStore.getCategoryName(row.category) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button text @click="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 最近访问 -->
    <div v-if="bookStore.recentBooks.length > 0" class="recent-books">
      <h3>最近访问的图书</h3>
      <el-tag
        v-for="book in bookStore.recentBooks.slice(0, 5)"
        :key="book.id"
        @click="viewDetail(book)"
        style="cursor: pointer; margin-right: 8px"
      >
        {{ book.title }}
      </el-tag>
    </div>
  </div>
</template>
```

---

## 场景2: 借书页面

### BorrowPage.vue - 借书流程

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useReaderStore, useCirculationStore, useBookStore } from '@/stores'
import { getReaderByCardNumber } from '@/api/readers'
import { getBookByBarcode } from '@/api/books'
import { borrowBook } from '@/api/circulation'

const readerStore = useReaderStore()
const circulationStore = useCirculationStore()
const bookStore = useBookStore()

const cardNumber = ref('')
const barcode = ref('')
const processing = ref(false)

// 当前读者借阅能力
const borrowCapacity = computed(() => readerStore.currentReaderBorrowCapacity)

// 扫描读者借阅证
async function scanReaderCard() {
  if (!cardNumber.value) {
    ElMessage.warning('请输入借阅证号')
    return
  }

  try {
    const response = await getReaderByCardNumber(cardNumber.value)
    const reader = response.data

    // 检查读者状态
    if (reader.status !== 'active') {
      ElMessage.error('该读者已被冻结，无法借书')
      return
    }

    // 设置为当前读者
    readerStore.setCurrentReader(reader)
    ElMessage.success(`读者: ${reader.realName} (${reader.cardNumber})`)

    // 清空输入框
    cardNumber.value = ''
  } catch (error) {
    ElMessage.error('读者信息获取失败')
  }
}

// 扫描图书条码
async function scanBookBarcode() {
  if (!barcode.value) {
    ElMessage.warning('请输入图书条码')
    return
  }

  if (!readerStore.hasCurrentReader) {
    ElMessage.warning('请先扫描读者借阅证')
    return
  }

  // 检查是否还能借书
  if (!borrowCapacity.value.canBorrow) {
    ElMessage.error('该读者已达到最大借阅数量')
    return
  }

  try {
    const response = await getBookByBarcode(barcode.value)
    const book = response.data

    // 检查图书状态
    if (book.availableCopies <= 0) {
      ElMessage.error('该图书无可借副本')
      return
    }

    // 添加到购物车
    circulationStore.addToBorrowCart(book)
    ElMessage.success(`已添加: ${book.title}`)

    // 清空输入框，准备扫描下一本
    barcode.value = ''
  } catch (error) {
    ElMessage.error('图书信息获取失败')
  }
}

// 确认借书
async function confirmBorrow() {
  if (!readerStore.hasCurrentReader) {
    ElMessage.warning('请先扫描读者借阅证')
    return
  }

  if (circulationStore.borrowCartCount === 0) {
    ElMessage.warning('请添加要借阅的图书')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认为 ${readerStore.currentReader.realName} 借出 ${circulationStore.borrowCartCount} 本图书?`,
      '确认借书',
      { type: 'warning' }
    )

    processing.value = true

    // 开始借阅流程
    circulationStore.startBorrow(readerStore.currentReader)

    // 逐本借出
    const books = circulationStore.borrowOperation.books
    for (const book of books) {
      await borrowBook({
        readerId: readerStore.currentReader.id,
        bookId: book.id
      })
    }

    // 成功完成
    circulationStore.endBorrow(true)
    ElMessage.success('借书成功')

    // 清空当前读者
    readerStore.clearCurrentReader()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('借书失败')
      circulationStore.endBorrow(false)
    }
  } finally {
    processing.value = false
  }
}

// 从购物车移除
function removeFromCart(bookId) {
  circulationStore.removeFromBorrowCart(bookId)
}

// 清空购物车和当前读者
function reset() {
  circulationStore.clearBorrowCart()
  readerStore.clearCurrentReader()
  cardNumber.value = ''
  barcode.value = ''
}

// 页面加载时加载读者类型
onMounted(async () => {
  try {
    await readerStore.loadReaderTypes()
  } catch (error) {
    console.error('加载读者类型失败:', error)
  }
})
</script>

<template>
  <div class="borrow-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图书借阅</span>
          <el-button text @click="reset">重置</el-button>
        </div>
      </template>

      <!-- 扫描读者借阅证 -->
      <el-form inline>
        <el-form-item label="借阅证号">
          <el-input
            v-model="cardNumber"
            placeholder="扫描或输入借阅证号"
            @keyup.enter="scanReaderCard"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="scanReaderCard">确认</el-button>
        </el-form-item>
      </el-form>

      <!-- 当前读者信息 -->
      <div v-if="readerStore.hasCurrentReader" class="reader-info">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="姓名">
            {{ readerStore.currentReader.realName }}
          </el-descriptions-item>
          <el-descriptions-item label="借阅证号">
            {{ readerStore.currentReader.cardNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="读者类型">
            {{ readerStore.getReaderTypeName(readerStore.currentReader.readerType) }}
          </el-descriptions-item>
          <el-descriptions-item label="已借数量">
            {{ borrowCapacity.currentBorrowCount }}
          </el-descriptions-item>
          <el-descriptions-item label="最大借阅">
            {{ borrowCapacity.maxBorrowCount }}
          </el-descriptions-item>
          <el-descriptions-item label="剩余额度">
            <el-tag :type="borrowCapacity.canBorrow ? 'success' : 'danger'">
              {{ borrowCapacity.remainingCount }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 扫描图书条码 -->
      <div v-if="readerStore.hasCurrentReader" style="margin-top: 20px">
        <el-form inline>
          <el-form-item label="图书条码">
            <el-input
              v-model="barcode"
              placeholder="扫描或输入图书条码"
              @keyup.enter="scanBookBarcode"
              :disabled="!borrowCapacity.canBorrow"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="scanBookBarcode"
              :disabled="!borrowCapacity.canBorrow"
            >
              添加
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 借阅购物车 -->
      <div v-if="circulationStore.borrowCartCount > 0" style="margin-top: 20px">
        <h3>待借图书 ({{ circulationStore.borrowCartCount }})</h3>
        <el-table :data="circulationStore.borrowCart">
          <el-table-column prop="title" label="书名" />
          <el-table-column prop="author" label="作者" />
          <el-table-column prop="isbn" label="ISBN" />
          <el-table-column prop="barcode" label="条码" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button text type="danger" @click="removeFromCart(row.id)"> 移除 </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top: 20px; text-align: right">
          <el-button @click="circulationStore.clearBorrowCart">清空</el-button>
          <el-button type="primary" @click="confirmBorrow" :loading="processing">
            确认借出
          </el-button>
        </div>
      </div>

      <!-- 最近操作的读者 -->
      <div v-if="readerStore.recentReaders.length > 0" style="margin-top: 20px">
        <h4>最近操作的读者</h4>
        <el-tag
          v-for="reader in readerStore.recentReaders.slice(0, 5)"
          :key="reader.id"
          style="margin-right: 8px; cursor: pointer"
          @click="
            cardNumber = reader.cardNumber
            scanReaderCard()
          "
        >
          {{ reader.realName }} ({{ reader.cardNumber }})
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.reader-info {
  margin-top: 20px;
}
</style>
```

---

## 场景3: 还书页面

### ReturnPage.vue - 还书流程

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCirculationStore } from '@/stores'
import { getBorrowRecordByBarcode } from '@/api/circulation'
import { returnBook } from '@/api/circulation'

const circulationStore = useCirculationStore()

const barcode = ref('')
const processing = ref(false)

// 扫描图书条码
async function scanBookBarcode() {
  if (!barcode.value) {
    ElMessage.warning('请输入图书条码')
    return
  }

  try {
    const response = await getBorrowRecordByBarcode(barcode.value)
    const record = response.data

    if (!record) {
      ElMessage.error('未找到该图书的借阅记录')
      return
    }

    // 添加到归还购物车
    circulationStore.addToReturnCart(record)

    // 如果逾期，显示罚款
    if (record.isOverdue) {
      const fine = circulationStore.calculateOverdueFine(record.overdueDays)
      ElMessage.warning(`该图书已逾期 ${record.overdueDays} 天，应缴罚款: ${fine.toFixed(2)} 元`)
    } else {
      ElMessage.success(`已添加: ${record.bookTitle}`)
    }

    // 清空输入框
    barcode.value = ''
  } catch (error) {
    ElMessage.error('借阅记录获取失败')
  }
}

// 确认还书
async function confirmReturn() {
  if (circulationStore.returnCartCount === 0) {
    ElMessage.warning('请添加要归还的图书')
    return
  }

  // 计算总罚款
  const totalFine = circulationStore.returnCart.reduce((sum, record) => {
    if (record.isOverdue) {
      return sum + circulationStore.calculateOverdueFine(record.overdueDays)
    }
    return sum
  }, 0)

  try {
    let message = `确认归还 ${circulationStore.returnCartCount} 本图书?`
    if (totalFine > 0) {
      message += `\n总罚款: ${totalFine.toFixed(2)} 元`
    }

    await ElMessageBox.confirm(message, '确认还书', {
      type: 'warning',
      distinguishCancelAndClose: true
    })

    processing.value = true

    // 逐本还书
    const records = circulationStore.returnCart
    for (const record of records) {
      await returnBook({
        recordId: record.id
      })
    }

    // 成功完成
    circulationStore.clearReturnCart()
    ElMessage.success('还书成功')

    if (totalFine > 0) {
      ElMessage.info(`请收取罚款: ${totalFine.toFixed(2)} 元`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('还书失败')
    }
  } finally {
    processing.value = false
  }
}

// 从购物车移除
function removeFromCart(recordId) {
  circulationStore.removeFromReturnCart(recordId)
}

// 页面加载时加载罚款规则
onMounted(async () => {
  try {
    await circulationStore.loadFineRules()
  } catch (error) {
    console.error('加载罚款规则失败:', error)
  }
})
</script>

<template>
  <div class="return-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图书归还</span>
          <el-button text @click="circulationStore.clearReturnCart">清空</el-button>
        </div>
      </template>

      <!-- 扫描图书条码 -->
      <el-form inline>
        <el-form-item label="图书条码">
          <el-input
            v-model="barcode"
            placeholder="扫描或输入图书条码"
            @keyup.enter="scanBookBarcode"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="scanBookBarcode">添加</el-button>
        </el-form-item>
      </el-form>

      <!-- 归还购物车 -->
      <div v-if="circulationStore.returnCartCount > 0" style="margin-top: 20px">
        <h3>待还图书 ({{ circulationStore.returnCartCount }})</h3>
        <el-table :data="circulationStore.returnCart">
          <el-table-column prop="bookTitle" label="书名" />
          <el-table-column prop="barcode" label="条码" />
          <el-table-column prop="borrowDate" label="借阅日期" />
          <el-table-column prop="dueDate" label="应还日期" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.isOverdue" type="danger"> 逾期 {{ row.overdueDays }} 天 </el-tag>
              <el-tag v-else type="success">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="罚款" width="100">
            <template #default="{ row }">
              <span v-if="row.isOverdue" style="color: red">
                {{ circulationStore.calculateOverdueFine(row.overdueDays).toFixed(2) }} 元
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button text type="danger" @click="removeFromCart(row.id)"> 移除 </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 汇总信息 -->
        <el-row style="margin-top: 20px">
          <el-col :span="12">
            <el-statistic title="待还数量" :value="circulationStore.returnCartCount" />
          </el-col>
          <el-col :span="12">
            <el-statistic
              title="总罚款"
              :value="
                circulationStore.returnCart
                  .reduce((sum, record) => {
                    if (record.isOverdue) {
                      return sum + circulationStore.calculateOverdueFine(record.overdueDays)
                    }
                    return sum
                  }, 0)
                  .toFixed(2)
              "
              suffix="元"
            />
          </el-col>
        </el-row>

        <div style="margin-top: 20px; text-align: right">
          <el-button @click="circulationStore.clearReturnCart">清空</el-button>
          <el-button type="primary" @click="confirmReturn" :loading="processing">
            确认归还
          </el-button>
        </div>
      </div>

      <!-- 罚款规则说明 -->
      <el-alert title="罚款规则" type="info" :closable="false" style="margin-top: 20px">
        <ul>
          <li>逾期罚款: {{ circulationStore.fineRules.overduePerDay }} 元/天</li>
          <li>最大罚款: {{ circulationStore.fineRules.maxFine }} 元</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>
```

---

## 场景4: 系统设置页面

### SystemSettings.vue - 系统配置

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '@/stores'

const systemStore = useSystemStore()

const formData = ref({})

// 页面加载时初始化表单
onMounted(() => {
  formData.value = { ...systemStore.config }
})

// 保存配置
function saveConfig() {
  systemStore.updateConfig(formData.value)
  ElMessage.success('配置已保存')
}

// 重置配置
function resetConfig() {
  systemStore.resetConfig()
  formData.value = { ...systemStore.config }
  ElMessage.success('配置已重置')
}
</script>

<template>
  <div class="system-settings">
    <el-card>
      <template #header>
        <span>系统配置</span>
      </template>

      <el-form :model="formData" label-width="120px">
        <!-- 系统信息 -->
        <el-divider content-position="left">系统信息</el-divider>

        <el-form-item label="系统名称">
          <el-input v-model="formData.systemName" />
        </el-form-item>

        <el-form-item label="系统简称">
          <el-input v-model="formData.systemShortName" />
        </el-form-item>

        <el-form-item label="版本号">
          <el-input v-model="formData.version" disabled />
        </el-form-item>

        <!-- UI配置 -->
        <el-divider content-position="left">界面配置</el-divider>

        <el-form-item label="主题">
          <el-radio-group v-model="formData.theme">
            <el-radio label="light">浅色</el-radio>
            <el-radio label="dark">深色</el-radio>
          </el-radio-group>
          <el-button text @click="systemStore.toggleTheme">切换主题</el-button>
        </el-form-item>

        <el-form-item label="显示Logo">
          <el-switch v-model="formData.showLogo" />
        </el-form-item>

        <el-form-item label="显示面包屑">
          <el-switch v-model="formData.showBreadcrumb" />
        </el-form-item>

        <el-form-item label="固定头部">
          <el-switch v-model="formData.fixedHeader" />
        </el-form-item>

        <!-- 分页配置 -->
        <el-divider content-position="left">分页配置</el-divider>

        <el-form-item label="默认分页大小">
          <el-input-number v-model="formData.pageSize" :min="10" :max="100" />
        </el-form-item>

        <!-- 安全配置 -->
        <el-divider content-position="left">安全配置</el-divider>

        <el-form-item label="会话超时(分钟)">
          <el-input-number v-model="formData.sessionTimeout" :min="5" :max="120" />
        </el-form-item>

        <el-form-item label="自动登出">
          <el-switch v-model="formData.autoLogout" />
        </el-form-item>

        <el-form-item label="密码最小长度">
          <el-input-number v-model="formData.passwordMinLength" :min="6" :max="20" />
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
          <el-button @click="resetConfig">重置为默认值</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 借阅规则配置 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>借阅规则配置</span>
      </template>

      <el-table :data="Object.entries(systemStore.borrowRules)">
        <el-table-column label="读者类型" width="120">
          <template #default="{ row }">
            {{ row[0] }}
          </template>
        </el-table-column>
        <el-table-column label="最大借阅数量">
          <template #default="{ row }">
            {{ row[1].maxBorrowCount }}
          </template>
        </el-table-column>
        <el-table-column label="借阅天数">
          <template #default="{ row }">
            {{ row[1].borrowDays }}
          </template>
        </el-table-column>
        <el-table-column label="最大续借次数">
          <template #default="{ row }">
            {{ row[1].maxRenewCount }}
          </template>
        </el-table-column>
        <el-table-column label="续借天数">
          <template #default="{ row }">
            {{ row[1].renewDays }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

---

## 场景5: 应用初始化

### App.vue - 应用初始化时加载数据

```vue
<script setup>
import { onMounted } from 'vue'
import { useBookStore, useReaderStore, useSystemStore } from '@/stores'

const bookStore = useBookStore()
const readerStore = useReaderStore()
const systemStore = useSystemStore()

// 应用启动时加载全局数据
onMounted(async () => {
  console.log('[App] Initializing application...')

  try {
    // 并行加载不依赖的数据
    await Promise.all([
      bookStore.loadCategories(),
      readerStore.loadReaderTypes(),
      systemStore.loadRoles()
    ])

    console.log('[App] Global data loaded successfully')
  } catch (error) {
    console.error('[App] Failed to load global data:', error)
  }
})
</script>

<template>
  <router-view />
</template>
```

---

## 总结

这些示例展示了如何在实际场景中使用Pinia Stores：

1. **图书管理**: 使用分类缓存、搜索历史、最近访问
2. **借书流程**: 结合多个Store实现复杂业务流程
3. **还书流程**: 使用购物车模式和罚款计算
4. **系统设置**: 配置管理和持久化
5. **应用初始化**: 预加载全局数据

关键点：

- 使用缓存减少API调用
- 利用购物车模式优化用户体验
- 结合多个Store完成复杂业务
- 合理使用持久化策略
- 在适当时机加载数据
