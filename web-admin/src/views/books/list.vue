<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">图书列表</h1>
      <p class="page-header-description">查询和管理图书信息</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="lib-card mb-md">
      <div class="lib-card-body">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-autocomplete
              v-model="queryForm.keyword"
              :fetch-suggestions="querySearchAsync"
              placeholder="请输入书名、作者或ISBN（支持智能搜索）"
              clearable
              style="width: 400px"
              @select="handleSelectSuggestion"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #suffix>
                <el-tag v-if="isAISearchEnabled" type="success" size="small">AI</el-tag>
              </template>
              <template #default="{ item }">
                <div class="search-suggestion-item">
                  <div class="suggestion-icon">
                    <el-icon v-if="item.type === 'book'"><Reading /></el-icon>
                    <el-icon v-else-if="item.type === 'author'"><User /></el-icon>
                    <el-icon v-else><Document /></el-icon>
                  </div>
                  <div class="suggestion-content">
                    <div class="suggestion-title">{{ item.value }}</div>
                    <div class="suggestion-meta">{{ item.meta }}</div>
                  </div>
                  <div v-if="item.score" class="suggestion-score">
                    <el-tag size="small">{{ (item.score * 100).toFixed(0) }}%</el-tag>
                  </div>
                </div>
              </template>
            </el-autocomplete>
          </el-form-item>

          <el-form-item label="分类">
            <el-select
              v-model="queryForm.category"
              placeholder="全部分类"
              clearable
              style="width: 150px"
            >
              <el-option label="全部" value="" />
              <el-option label="文学" value="literature" />
              <el-option label="历史" value="history" />
              <el-option label="科学" value="science" />
              <el-option label="艺术" value="art" />
              <el-option label="计算机" value="computer" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select
              v-model="queryForm.status"
              placeholder="全部状态"
              clearable
              style="width: 120px"
            >
              <el-option label="全部" value="" />
              <el-option label="在架" value="available" />
              <el-option label="借出" value="borrowed" />
              <el-option label="预约中" value="reserved" />
              <el-option label="停用" value="disabled" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button type="success" :icon="Plus" @click="$router.push('/books/catalog')"
              >新增图书</el-button
            >
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="lib-card">
      <div class="lib-card-body">
        <!-- 批量操作 -->
        <div v-if="selectedBooks.length > 0" class="batch-actions">
          <span class="batch-info">已选择 {{ selectedBooks.length }} 项</span>
          <el-button type="danger" size="small" :icon="Delete" @click="handleBatchDelete"
            >批量删除</el-button
          >
          <el-button size="small" @click="handleClearSelection">取消选择</el-button>
        </div>

        <!-- 表格 -->
        <el-table
          v-loading="loading"
          :data="bookList"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column label="封面" width="80">
            <template #default="{ row }">
              <el-image
                :src="row.cover || defaultBookCover"
                :preview-src-list="[row.cover || defaultBookCover]"
                class="book-cover"
                fit="cover"
                :preview-teleported="true"
              >
                <template #error>
                  <div class="book-cover-placeholder">
                    <el-icon><Reading /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="isbn" label="ISBN" width="140" />
          <el-table-column prop="title" label="书名" show-overflow-tooltip min-width="200" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="publisher" label="出版社" width="150" show-overflow-tooltip />
          <el-table-column label="分类" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getCategoryName(row.category) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="馆藏/可借" width="100">
            <template #default="{ row }">
              <span :class="row.availableCount > 0 ? 'text-success' : 'text-danger'">
                {{ row.totalCount }} / {{ row.availableCount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="borrowCount" label="借阅次数" width="100" sortable />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'available'" type="success" size="small">在架</el-tag>
              <el-tag v-else-if="row.status === 'borrowed'" type="warning" size="small"
                >借出</el-tag
              >
              <el-tag v-else-if="row.status === 'reserved'" type="info" size="small">预约中</el-tag>
              <el-tag v-else type="danger" size="small">停用</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <ActionIcons
                :actions="[
                  { key: 'view', label: '查看详情', icon: IconView, variant: 'primary' },
                  { key: 'edit', label: '编辑', icon: IconEdit, variant: 'primary' },
                  { key: 'delete', label: '删除', icon: IconDelete, variant: 'danger' }
                ]"
                @action="
                  (key) => {
                    if (key === 'view') handleView(row)
                    else if (key === 'edit') handleEdit(row)
                    else if (key === 'delete') handleDelete(row)
                  }
                "
              />
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 图书详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="图书详情" width="900px">
      <div v-if="currentBook" class="book-detail">
        <div class="book-detail-cover">
          <el-image
            :src="currentBook.cover || defaultBookCover"
            :preview-src-list="[currentBook.cover || defaultBookCover]"
            class="detail-cover-image"
            fit="cover"
          >
            <template #error>
              <div class="detail-cover-placeholder">
                <el-icon><Reading /></el-icon>
                <p>暂无封面</p>
              </div>
            </template>
          </el-image>
        </div>

        <div class="book-detail-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="ISBN">{{ currentBook.isbn }}</el-descriptions-item>
            <el-descriptions-item label="条码号">{{ currentBook.barcode }}</el-descriptions-item>
            <el-descriptions-item label="书名" :span="2">{{
              currentBook.title
            }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentBook.author }}</el-descriptions-item>
            <el-descriptions-item label="出版社">{{ currentBook.publisher }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{
              getCategoryName(currentBook.category)
            }}</el-descriptions-item>
            <el-descriptions-item label="价格">¥{{ currentBook.price }}</el-descriptions-item>
            <el-descriptions-item label="馆藏数量">{{
              currentBook.totalCount
            }}</el-descriptions-item>
            <el-descriptions-item label="可借数量">{{
              currentBook.availableCount
            }}</el-descriptions-item>
            <el-descriptions-item label="借阅次数">{{
              currentBook.borrowCount
            }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="currentBook.status === 'available'" type="success">在架</el-tag>
              <el-tag v-else-if="currentBook.status === 'borrowed'" type="warning">借出</el-tag>
              <el-tag v-else-if="currentBook.status === 'reserved'" type="info">预约中</el-tag>
              <el-tag v-else type="danger">停用</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">
              {{ currentBook.summary || '暂无简介' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBooks, deleteBook, batchDeleteBooks, getBookCategories } from '@/api/books'
import ActionIcons from '@/components/ActionIcons.vue'
import { View as IconView, Edit as IconEdit, Delete as IconDelete } from '@element-plus/icons-vue'

// 查询表单
const queryForm = reactive({
  keyword: '',
  category: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const bookList = ref([])
const selectedBooks = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 图书详情
const detailDialogVisible = ref(false)
const currentBook = ref(null)

// AI智能搜索
const isAISearchEnabled = ref(true)

// 默认图书封面
const defaultBookCover =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjE0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjE0MCIgZmlsbD0iIzY2N2VlYSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiNmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGRvbWluYW50LWJhc2VsaW5lPSJtaWRkbGUiPuWbvuS5pjwvdGV4dD48L3N2Zz4='

// 分类映射
const categoryMap = {
  literature: '文学',
  history: '历史',
  science: '科学',
  art: '艺术',
  computer: '计算机',
  other: '其他'
}

// 获取分类名称
const getCategoryName = (category) => {
  return categoryMap[category] || category
}

// 加载图书列表
const loadBookList = async () => {
  loading.value = true
  try {
    const res = await getBooks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      keyword: queryForm.keyword,
      category: queryForm.category,
      status: queryForm.status
    })

    if (res.code === 200 && res.data) {
      // 转换Mock API数据格式到页面需要的格式
      bookList.value = res.data.records.map((book) => ({
        id: book.bookId,
        isbn: book.isbn,
        barcode: book.callNumber,
        title: book.title,
        author: book.author,
        publisher: book.publisher,
        category: book.category,
        price: book.price,
        totalCount: book.totalCopies,
        availableCount: book.availableCopies,
        borrowCount: book.borrowCount,
        status: book.status,
        summary: book.description,
        cover: book.coverUrl
      }))
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadBookList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.status = ''
  pagination.page = 1
  loadBookList()
}

// 分页变化
const handlePageChange = (page) => {
  pagination.page = page
  loadBookList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadBookList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedBooks.value = selection
}

const handleClearSelection = () => {
  selectedBooks.value = []
}

// 查看详情
const handleView = (row) => {
  currentBook.value = row
  detailDialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  ElMessage.info(`编辑图书: ${row.title}`)
  // TODO: 跳转到编辑页面或打开编辑对话框
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除《${row.title}》吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteBook(row.id)
        ElMessage.success('删除成功')
        loadBookList()
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedBooks.value.length} 本图书吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const ids = selectedBooks.value.map((b) => b.id)
        await batchDeleteBooks(ids)
        ElMessage.success('批量删除成功')
        selectedBooks.value = []
        loadBookList()
      } catch (error) {
        console.error('批量删除失败:', error)
        ElMessage.error('批量删除失败')
      }
    })
    .catch(() => {})
}

// AI智能搜索建议
const querySearchAsync = (queryString, cb) => {
  if (!queryString) {
    cb([])
    return
  }

  // 模拟AI智能搜索
  setTimeout(() => {
    const suggestions = [
      // 图书建议
      {
        value: '人工智能基础',
        type: 'book',
        meta: '周志华 | 机械工业出版社',
        score: 0.95
      },
      {
        value: 'Python编程从入门到实践',
        type: 'book',
        meta: 'Eric Matthes | 人民邮电出版社',
        score: 0.89
      },
      // 作者建议
      {
        value: '刘慈欣',
        type: 'author',
        meta: '作者 - 共23本图书',
        score: 0.85
      },
      // 分类建议
      {
        value: '计算机科学与技术',
        type: 'category',
        meta: '分类 - 共456本图书',
        score: 0.78
      }
    ]

    // 根据输入过滤建议
    const results = suggestions.filter((item) =>
      item.value.toLowerCase().includes(queryString.toLowerCase())
    )

    cb(results)
  }, 300)
}

// 选择搜索建议
const handleSelectSuggestion = (item) => {
  queryForm.keyword = item.value
  handleSearch()
}

// 组件挂载时加载数据
onMounted(() => {
  loadBookList()
})
</script>

<style lang="scss" scoped>
.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 16px;

  .batch-info {
    flex: 1;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.65);
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-success {
  color: #52c41a;
}

.text-danger {
  color: #f5222d;
}

.search-suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;

  .suggestion-icon {
    flex-shrink: 0;
    color: #1890ff;
  }

  .suggestion-content {
    flex: 1;
    min-width: 0;
  }

  .suggestion-title {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .suggestion-meta {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
  }

  .suggestion-score {
    flex-shrink: 0;
  }
}

.book-cover {
  width: 50px;
  height: 70px;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: scale(1.1);
  }
}

.book-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  color: #999;
  font-size: 24px;
}

.book-detail {
  display: flex;
  gap: 24px;

  &-cover {
    flex-shrink: 0;

    .detail-cover-image {
      width: 200px;
      height: 280px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .detail-cover-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      background: #f5f5f5;
      color: #999;

      .el-icon {
        font-size: 48px;
        margin-bottom: 8px;
      }

      p {
        margin: 0;
        font-size: 14px;
      }
    }
  }

  &-info {
    flex: 1;
    min-width: 0;
  }
}
</style>
