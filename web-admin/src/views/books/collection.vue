<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">馆藏管理</h1>
      <p class="page-header-description">查看和管理所有图书馆藏副本</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="请输入书名、ISBN或条码号"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="分类">
            <el-select
              v-model="queryForm.category"
              placeholder="全部分类"
              clearable
              style="width: 140px"
            >
              <el-option label="全部" value="" />
              <el-option label="文学" value="literature" />
              <el-option label="历史" value="history" />
              <el-option label="科学" value="science" />
              <el-option label="艺术" value="art" />
              <el-option label="计算机" value="computer" />
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
              <el-option label="在库" value="available" />
              <el-option label="借出" value="borrowed" />
              <el-option label="损坏" value="damaged" />
              <el-option label="丢失" value="lost" />
              <el-option label="维修中" value="repair" />
            </el-select>
          </el-form-item>

          <el-form-item label="位置">
            <el-input
              v-model="queryForm.location"
              placeholder="如: A区"
              clearable
              style="width: 120px"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button type="success" :icon="Download" @click="handleExport">导出数据</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="16" class="mb-md">
      <el-col :xs="24" :sm="8" :md="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">总馆藏量</div>
          <div class="stat-mini-card-value">{{ statistics.total }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">在库数量</div>
          <div class="stat-mini-card-value text-success">{{ statistics.available }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">借出数量</div>
          <div class="stat-mini-card-value text-primary">{{ statistics.borrowed }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">异常数量</div>
          <div class="stat-mini-card-value text-danger">{{ statistics.abnormal }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <div class="card">
      <div class="card-content">
        <!-- 批量操作 -->
        <div v-if="selectedItems.length > 0" class="batch-actions">
          <span class="batch-info">已选择 {{ selectedItems.length }} 项</span>
          <el-button type="primary" size="small" :icon="Edit" @click="handleBatchUpdateLocation"
            >批量调整位置</el-button
          >
          <el-button type="warning" size="small" :icon="Warning" @click="handleBatchSetStatus"
            >批量设置状态</el-button
          >
          <el-button size="small" @click="handleClearSelection">取消选择</el-button>
        </div>

        <!-- 表格 -->
        <el-table
          v-loading="loading"
          :data="collectionList"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="barcode" label="条码号" width="140" />
          <el-table-column prop="bookTitle" label="书名" show-overflow-tooltip min-width="180" />
          <el-table-column prop="isbn" label="ISBN" width="140" />
          <el-table-column label="分类" width="100">
            <template #default="{ row }">
              {{ getCategoryName(row.category) }}
            </template>
          </el-table-column>
          <el-table-column prop="location" label="存放位置" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="借阅次数" width="100" align="center">
            <template #default="{ row }">
              <span class="borrow-count">{{ row.borrowCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="acquisitionDate" label="入馆日期" width="110" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <ActionIcons
                :actions="[
                  { key: 'view', label: '查看详情', icon: IconView, variant: 'primary' },
                  { key: 'loc', label: '调整位置', icon: IconLocation, variant: 'primary' },
                  { key: 'status', label: '设置状态', icon: IconWarning, variant: 'warning' },
                  { key: 'del', label: '注销', icon: IconDelete, variant: 'danger' }
                ]"
                @action="
                  (k) => {
                    if (k === 'view') handleView(row)
                    else if (k === 'loc') handleEditLocation(row)
                    else if (k === 'status') handleSetStatus(row)
                    else handleDelete(row)
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="馆藏详情" width="700px">
      <el-descriptions v-if="currentItem" :column="2" border>
        <el-descriptions-item label="条码号">{{ currentItem.barcode }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ currentItem.isbn }}</el-descriptions-item>
        <el-descriptions-item label="书名" :span="2">{{
          currentItem.bookTitle
        }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ currentItem.author }}</el-descriptions-item>
        <el-descriptions-item label="出版社">{{ currentItem.publisher }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{
          getCategoryName(currentItem.category)
        }}</el-descriptions-item>
        <el-descriptions-item label="存放位置">{{ currentItem.location }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentItem.status)" size="small">
            {{ getStatusName(currentItem.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="借阅次数"
          >{{ currentItem.borrowCount }} 次</el-descriptions-item
        >
        <el-descriptions-item label="入馆日期">{{
          currentItem.acquisitionDate
        }}</el-descriptions-item>
        <el-descriptions-item label="最后借出">
          {{ currentItem.lastBorrowDate || '未借出' }}
        </el-descriptions-item>
        <el-descriptions-item label="当前借阅人" :span="2">
          {{ currentItem.currentBorrower || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentItem.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 调整位置对话框 -->
    <el-dialog v-model="locationDialogVisible" title="调整存放位置" width="500px">
      <el-form :model="locationForm" label-width="100px">
        <el-form-item label="当前位置">
          <el-input :value="locationForm.oldLocation" disabled />
        </el-form-item>

        <el-form-item label="新位置" required>
          <el-input v-model="locationForm.newLocation" placeholder="如: A区3层5排" />
        </el-form-item>

        <el-form-item label="调整原因">
          <el-input
            v-model="locationForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmLocation">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设置状态对话框 -->
    <el-dialog v-model="statusDialogVisible" title="设置馆藏状态" width="500px">
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(statusForm.oldStatus)" size="small">
            {{ getStatusName(statusForm.oldStatus) }}
          </el-tag>
        </el-form-item>

        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.newStatus" placeholder="请选择新状态" style="width: 100%">
            <el-option label="在库" value="available" />
            <el-option label="损坏" value="damaged" />
            <el-option label="丢失" value="lost" />
            <el-option label="维修中" value="repair" />
            <el-option label="已报废" value="discarded" />
          </el-select>
        </el-form-item>

        <el-form-item label="变更原因">
          <el-input
            v-model="statusForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入变更原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmStatus">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量调整位置对话框 -->
    <el-dialog v-model="batchLocationDialogVisible" title="批量调整存放位置" width="500px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title> 即将调整 {{ selectedItems.length }} 项馆藏的存放位置 </template>
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="新位置" required>
          <el-input v-model="batchLocationForm.location" placeholder="如: A区3层5排" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchLocationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmBatchLocation">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量设置状态对话框 -->
    <el-dialog v-model="batchStatusDialogVisible" title="批量设置状态" width="500px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title> 即将设置 {{ selectedItems.length }} 项馆藏的状态 </template>
      </el-alert>

      <el-form label-width="100px">
        <el-form-item label="状态" required>
          <el-select v-model="batchStatusForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="在库" value="available" />
            <el-option label="损坏" value="damaged" />
            <el-option label="丢失" value="lost" />
            <el-option label="维修中" value="repair" />
            <el-option label="已报废" value="discarded" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="batchStatusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmBatchStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ActionIcons from '@/components/ActionIcons.vue'
import {
  View as IconView,
  Location as IconLocation,
  Warning as IconWarning,
  Delete as IconDelete
} from '@element-plus/icons-vue'

// 查询表单
const queryForm = reactive({
  keyword: '',
  category: '',
  status: '',
  location: ''
})

// 表格数据
const loading = ref(false)
const collectionList = ref([])
const selectedItems = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 统计信息
const statistics = reactive({
  total: 0,
  available: 0,
  borrowed: 0,
  abnormal: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentItem = ref(null)

// 调整位置对话框
const locationDialogVisible = ref(false)
const locationForm = reactive({
  id: null,
  oldLocation: '',
  newLocation: '',
  reason: ''
})

// 设置状态对话框
const statusDialogVisible = ref(false)
const statusForm = reactive({
  id: null,
  oldStatus: '',
  newStatus: '',
  reason: ''
})

// 批量操作对话框
const batchLocationDialogVisible = ref(false)
const batchLocationForm = reactive({ location: '' })

const batchStatusDialogVisible = ref(false)
const batchStatusForm = reactive({ status: '' })

// 分类映射
const categoryMap = {
  literature: '文学',
  history: '历史',
  science: '科学',
  art: '艺术',
  computer: '计算机',
  philosophy: '哲学',
  economics: '经济',
  education: '教育'
}

// 状态映射
const statusMap = {
  available: '在库',
  borrowed: '借出',
  damaged: '损坏',
  lost: '丢失',
  repair: '维修中',
  discarded: '已报废'
}

// 获取分类名称
const getCategoryName = (category) => categoryMap[category] || category

// 获取状态名称
const getStatusName = (status) => statusMap[status] || status

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    available: 'success',
    borrowed: '',
    damaged: 'warning',
    lost: 'danger',
    repair: 'info',
    discarded: 'info'
  }
  return typeMap[status] || ''
}

// 加载馆藏列表
const loadCollectionList = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取馆藏列表
    await new Promise((resolve) => setTimeout(resolve, 500))

    const mockData = []
    const categories = Object.keys(categoryMap)
    const statuses = ['available', 'borrowed', 'damaged', 'lost', 'repair']

    for (let i = 1; i <= pagination.pageSize; i++) {
      const id = (pagination.page - 1) * pagination.pageSize + i
      const status = statuses[i % statuses.length]

      mockData.push({
        id,
        barcode: `B${id.toString().padStart(8, '0')}`,
        isbn: `978-7-${Math.random().toString().substr(2, 9)}`,
        bookTitle: ['活着', '三体', '百年孤独', 'Python编程', '数据结构与算法'][i % 5],
        author: ['余华', '刘慈欣', '马尔克斯', 'Eric Matthes', '严蔚敏'][i % 5],
        publisher: ['作家出版社', '重庆出版社', '南海出版社', '人民邮电出版社', '清华大学出版社'][
          i % 5
        ],
        category: categories[i % categories.length],
        location: `${'ABCD'[i % 4]}区${(i % 5) + 1}层${(i % 10) + 1}排`,
        status,
        borrowCount: Math.floor(Math.random() * 100),
        acquisitionDate: '2024-09-01',
        lastBorrowDate: status === 'borrowed' ? '2025-10-05' : null,
        currentBorrower: status === 'borrowed' ? '张三 (S20250001)' : null,
        remark: i % 5 === 0 ? '第一版，状态良好' : ''
      })
    }

    collectionList.value = mockData
    pagination.total = 1856

    // 更新统计信息
    statistics.total = pagination.total
    statistics.available = Math.floor(pagination.total * 0.6)
    statistics.borrowed = Math.floor(pagination.total * 0.3)
    statistics.abnormal = Math.floor(pagination.total * 0.1)
  } catch (error) {
    ElMessage.error('加载馆藏列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadCollectionList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.status = ''
  queryForm.location = ''
  pagination.page = 1
  loadCollectionList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadCollectionList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadCollectionList()
}

// 选择
const handleSelectionChange = (selection) => {
  selectedItems.value = selection
}

const handleClearSelection = () => {
  selectedItems.value = []
}

// 查看详情
const handleView = (row) => {
  currentItem.value = row
  detailDialogVisible.value = true
}

// 调整位置
const handleEditLocation = (row) => {
  locationForm.id = row.id
  locationForm.oldLocation = row.location
  locationForm.newLocation = ''
  locationForm.reason = ''
  locationDialogVisible.value = true
}

// 确认调整位置
const handleConfirmLocation = async () => {
  if (!locationForm.newLocation) {
    ElMessage.warning('请输入新位置')
    return
  }

  try {
    // TODO: 调用API调整位置
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('位置调整成功')
    locationDialogVisible.value = false
    loadCollectionList()
  } catch (error) {
    ElMessage.error('调整失败')
  }
}

// 设置状态
const handleSetStatus = (row) => {
  if (row.status === 'borrowed') {
    ElMessage.warning('图书已借出，无法直接设置状态')
    return
  }

  statusForm.id = row.id
  statusForm.oldStatus = row.status
  statusForm.newStatus = ''
  statusForm.reason = ''
  statusDialogVisible.value = true
}

// 确认设置状态
const handleConfirmStatus = async () => {
  if (!statusForm.newStatus) {
    ElMessage.warning('请选择新状态')
    return
  }

  try {
    // TODO: 调用API设置状态
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('状态设置成功')
    statusDialogVisible.value = false
    loadCollectionList()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

// 批量调整位置
const handleBatchUpdateLocation = () => {
  batchLocationForm.location = ''
  batchLocationDialogVisible.value = true
}

// 确认批量调整位置
const handleConfirmBatchLocation = async () => {
  if (!batchLocationForm.location) {
    ElMessage.warning('请输入新位置')
    return
  }

  try {
    // TODO: 调用API批量调整位置
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success(`成功调整 ${selectedItems.value.length} 项馆藏的位置`)
    batchLocationDialogVisible.value = false
    selectedItems.value = []
    loadCollectionList()
  } catch (error) {
    ElMessage.error('批量调整失败')
  }
}

// 批量设置状态
const handleBatchSetStatus = () => {
  const hasBorrowed = selectedItems.value.some((item) => item.status === 'borrowed')
  if (hasBorrowed) {
    ElMessage.warning('选中的项目中包含已借出图书，无法批量设置状态')
    return
  }

  batchStatusForm.status = ''
  batchStatusDialogVisible.value = true
}

// 确认批量设置状态
const handleConfirmBatchStatus = async () => {
  if (!batchStatusForm.status) {
    ElMessage.warning('请选择状态')
    return
  }

  try {
    // TODO: 调用API批量设置状态
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success(`成功设置 ${selectedItems.value.length} 项馆藏的状态`)
    batchStatusDialogVisible.value = false
    selectedItems.value = []
    loadCollectionList()
  } catch (error) {
    ElMessage.error('批量设置失败')
  }
}

// 注销
const handleDelete = (row) => {
  if (row.status === 'borrowed') {
    ElMessage.warning('图书已借出，无法注销')
    return
  }

  ElMessageBox.confirm(`确定要注销该馆藏吗？条码号：${row.barcode}`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API注销馆藏
      await new Promise((resolve) => setTimeout(resolve, 500))

      ElMessage.success('注销成功')
      loadCollectionList()
    })
    .catch(() => {})
}

// 导出数据
const handleExport = () => {
  // TODO: 实现导出功能
  ElMessage.success('导出功能开发中，敬请期待')
}

// 初始化
onMounted(() => {
  loadCollectionList()
})
</script>

<style lang="scss" scoped>
.stat-mini-card {
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  text-align: center;

  &-label {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    margin-bottom: 8px;
  }

  &-value {
    font-size: 24px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);

    &.text-primary {
      color: #1890ff;
    }

    &.text-success {
      color: #52c41a;
    }

    &.text-danger {
      color: #f5222d;
    }
  }
}

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

.borrow-count {
  font-weight: 600;
  color: #1890ff;
}
</style>
