<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">库存盘点</h1>
      <p class="page-header-description">创建盘点任务，核对馆藏实际数量</p>
    </div>

    <!-- 盘点任务列表 -->
    <div class="card mb-md">
      <div class="card-header">
        <span>盘点任务</span>
        <el-button type="primary" :icon="Plus" @click="handleCreateTask">新建盘点任务</el-button>
      </div>
      <div class="card-content">
        <!-- 骨架屏 -->
        <SkeletonLoader
          v-if="loading && taskList.length === 0"
          type="table"
          :rows="5"
          :columns="10"
        />

        <!-- 数据表格 -->
        <template v-else>
          <el-table v-loading="loading" :data="taskList" stripe>
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="taskNo" label="任务编号" width="140" />
            <el-table-column prop="taskName" label="任务名称" min-width="180" />
            <el-table-column label="盘点范围" width="150">
              <template #default="{ row }">
                {{ getScopeName(row.scope) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="150">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.progress"
                  :status="row.progress === 100 ? 'success' : ''"
                />
              </template>
            </el-table-column>
            <el-table-column prop="totalCount" label="总数量" width="100" align="center" />
            <el-table-column prop="checkedCount" label="已盘点" width="100" align="center" />
            <el-table-column label="差异" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.diffCount > 0" class="text-danger">{{ row.diffCount }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="creator" label="创建人" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="160" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status?.toUpperCase() === 'PENDING'"
                  type="success"
                  link
                  :icon="VideoPlay"
                  @click="handleStartTask(row)"
                >
                  开始盘点
                </el-button>
                <el-button
                  v-if="row.status?.toUpperCase() === 'PAUSED'"
                  type="success"
                  link
                  :icon="VideoPlay"
                  @click="handleResumeTask(row)"
                >
                  恢复
                </el-button>
                <el-button type="primary" link :icon="View" @click="handleViewTask(row)"
                  >查看详情</el-button
                >
                <el-button
                  v-if="row.status?.toUpperCase() === 'IN_PROGRESS'"
                  type="warning"
                  link
                  :icon="VideoPause"
                  @click="handlePauseTask(row)"
                >
                  暂停
                </el-button>
                <el-button
                  v-if="row.status?.toUpperCase() === 'COMPLETED'"
                  type="primary"
                  link
                  :icon="Download"
                  @click="handleExportReport(row)"
                >
                  导出报告
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div v-if="pagination.total > 0" class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </template>
      </div>
    </div>

    <!-- 新建盘点任务对话框 -->
    <el-dialog v-model="taskDialogVisible" title="新建盘点任务" width="600px">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskFormRules" label-width="100px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="taskForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="盘点范围" prop="scope">
          <el-select v-model="taskForm.scope" placeholder="请选择盘点范围" style="width: 100%">
            <el-option label="全部馆藏" value="all" />
            <el-option label="指定分类" value="category" />
            <el-option label="指定位置" value="location" />
            <el-option label="指定条码范围" value="barcode_range" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="taskForm.scope === 'category'" label="选择分类">
          <el-select
            v-model="taskForm.category"
            placeholder="请选择分类"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="taskForm.scope === 'location'" label="存放位置">
          <el-input v-model="taskForm.location" placeholder="如: A区" />
        </el-form-item>

        <el-form-item v-if="taskForm.scope === 'barcode_range'" label="条码范围">
          <el-row :gutter="16">
            <el-col :span="11">
              <el-input v-model="taskForm.barcodeStart" placeholder="起始条码" />
            </el-col>
            <el-col :span="2" style="text-align: center">至</el-col>
            <el-col :span="11">
              <el-input v-model="taskForm.barcodeEnd" placeholder="结束条码" />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="taskForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTask">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 盘点详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="盘点任务详情" width="900px">
      <div v-loading="diffLoading">
        <el-descriptions v-if="currentTask" :column="2" border>
          <el-descriptions-item label="任务编号">{{ currentTask.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
          <el-descriptions-item label="盘点范围">{{
            getScopeName(currentTask.scope)
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ getStatusName(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总数量">{{ currentTask.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="已盘点">{{ currentTask.checkedCount }}</el-descriptions-item>
          <el-descriptions-item label="差异数量">
            <span :class="currentTask.diffCount > 0 ? 'text-danger' : ''">
              {{ currentTask.diffCount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ currentTask.creator }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{
            currentTask.createdAt
          }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>差异明细</el-divider>

        <el-table
          v-if="currentTask?.diffList && currentTask.diffList.length > 0"
          :data="currentTask.diffList"
          stripe
          max-height="400"
        >
          <el-table-column prop="barcode" label="条码号" width="140" />
          <el-table-column prop="bookTitle" label="书名" min-width="200" />
          <el-table-column label="差异类型" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.diffType === 'missing'" type="warning" size="small">缺失</el-tag>
              <el-tag v-else-if="row.diffType === 'extra'" type="success" size="small">多余</el-tag>
              <el-tag v-else type="info" size="small">状态不符</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expectedStatus" label="预期状态" width="100" />
          <el-table-column prop="actualStatus" label="实际状态" width="100" />
          <el-table-column prop="location" label="位置" width="120" />
        </el-table>

        <el-empty v-else description="无差异项" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, VideoPlay, VideoPause, Download } from '@element-plus/icons-vue'
import { SkeletonLoader, PageLoading } from '@/components/Loading'
import {
  getInventoryTasks,
  getInventoryTaskById,
  createInventoryTask,
  startInventoryTask,
  pauseInventoryTask,
  resumeInventoryTask,
  exportInventoryReport,
  getInventoryDiffs
} from '@/api/inventory'
import { getBookCategories } from '@/api/books'

// 表格数据
const loading = ref(false)
const taskList = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 任务对话框
const taskDialogVisible = ref(false)
const taskFormRef = ref()
const taskForm = reactive({
  taskName: '',
  scope: 'all',
  category: '',
  location: '',
  barcodeStart: '',
  barcodeEnd: '',
  remark: ''
})

const taskFormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  scope: [{ required: true, message: '请选择盘点范围', trigger: 'change' }]
}

// 分类选项
const categoryOptions = ref([])

// 详情对话框
const detailDialogVisible = ref(false)
const currentTask = ref(null)
const diffLoading = ref(false)

// 盘点范围映射 - 支持前端小写和后端大写格式
const scopeMap = {
  all: '全部馆藏',
  category: '指定分类',
  location: '指定位置',
  barcode_range: '指定条码范围',
  // 后端返回大写格式
  ALL: '全部馆藏',
  CATEGORY: '指定分类',
  LOCATION: '指定位置'
}

// 状态映射 - 支持前端小写和后端大写格式
const statusMap = {
  pending: '待开始',
  in_progress: '进行中',
  paused: '已暂停',
  completed: '已完成',
  cancelled: '已取消',
  // 后端返回大写格式
  PENDING: '待开始',
  IN_PROGRESS: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

// 获取范围名称
const getScopeName = (scope) => scopeMap[scope] || scope

// 获取状态名称
const getStatusName = (status) => statusMap[status] || status

// 获取状态标签类型 - 统一转换为小写进行比较
const getStatusType = (status) => {
  const normalizedStatus = status?.toLowerCase?.() || status
  const typeMap = {
    pending: 'info',
    in_progress: '',
    paused: 'warning',
    completed: 'success',
    cancelled: 'danger'
  }
  return typeMap[normalizedStatus] || ''
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getBookCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map((cat) => ({
        label: cat.name,
        value: cat.code
      }))
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    // 使用默认分类
    categoryOptions.value = [
      { label: '文学', value: 'literature' },
      { label: '历史', value: 'history' },
      { label: '科学', value: 'science' },
      { label: '艺术', value: 'art' },
      { label: '计算机', value: 'computer' }
    ]
  }
}

// 加载任务列表
const loadTaskList = async () => {
  loading.value = true
  try {
    const res = await getInventoryTasks({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    if (res.code === 200) {
      taskList.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载任务列表失败:', error)
    // 错误已由errorHandler处理
  } finally {
    loading.value = false
  }
}

// 分页变化
const handlePageChange = (page) => {
  pagination.pageNum = page
  loadTaskList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadTaskList()
}

// 新建任务
const handleCreateTask = () => {
  taskForm.taskName = ''
  taskForm.scope = 'all'
  taskForm.category = ''
  taskForm.location = ''
  taskForm.barcodeStart = ''
  taskForm.barcodeEnd = ''
  taskForm.remark = ''
  taskDialogVisible.value = true
}

// 前端scope值到后端scope值的映射
const scopeMapping = {
  all: 'ALL',
  category: 'CATEGORY',
  location: 'LOCATION',
  barcode_range: 'ALL' // 条码范围作为全部处理，使用bookIds
}

// 提交任务
const handleSubmitTask = async () => {
  if (!taskFormRef.value) return

  try {
    await taskFormRef.value.validate()

    // 转换前端表单数据为后端API格式
    const requestData = {
      taskName: taskForm.taskName,
      taskType: 'FULL', // 默认为全面盘点，后续可根据需求调整
      scope: scopeMapping[taskForm.scope] || 'ALL',
      location: taskForm.scope === 'location' ? taskForm.location : null,
      categoryCode: taskForm.scope === 'category' ? taskForm.category : null,
      notes: taskForm.remark
    }

    const res = await createInventoryTask(requestData)

    if (res.code === 200 || res.code === 201) {
      ElMessage.success('盘点任务创建成功')
      taskDialogVisible.value = false
      loadTaskList()
    }
  } catch (error) {
    console.error('创建任务失败:', error)
    // 错误已由errorHandler处理
  }
}

// 开始任务
const handleStartTask = (row) => {
  ElMessageBox.confirm(`确定要开始盘点任务"${row.taskName}"吗？`, '开始盘点', {
    type: 'info',
    confirmButtonText: '开始',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        const res = await startInventoryTask(row.id)
        if (res.code === 200) {
          ElMessage.success('盘点任务已开始')
          loadTaskList()
        }
      } catch (error) {
        console.error('开始任务失败:', error)
      }
    })
    .catch(() => {})
}

// 暂停任务
const handlePauseTask = (row) => {
  ElMessageBox.confirm(`确定要暂停盘点任务"${row.taskName}"吗？`, '暂停盘点', {
    type: 'warning',
    confirmButtonText: '暂停',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        const res = await pauseInventoryTask(row.id)
        if (res.code === 200) {
          ElMessage.success('盘点任务已暂停')
          loadTaskList()
        }
      } catch (error) {
        console.error('暂停任务失败:', error)
      }
    })
    .catch(() => {})
}

// 恢复任务
const handleResumeTask = (row) => {
  ElMessageBox.confirm(`确定要恢复盘点任务"${row.taskName}"吗？`, '恢复盘点', {
    type: 'info',
    confirmButtonText: '恢复',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        const res = await resumeInventoryTask(row.id)
        if (res.code === 200) {
          ElMessage.success('盘点任务已恢复')
          loadTaskList()
        }
      } catch (error) {
        console.error('恢复任务失败:', error)
      }
    })
    .catch(() => {})
}

// 查看详情
const handleViewTask = async (row) => {
  try {
    diffLoading.value = true
    detailDialogVisible.value = true

    // 加载任务详情和差异明细
    const [taskRes, diffRes] = await Promise.all([
      getInventoryTaskById(row.id),
      getInventoryDiffs(row.id, { pageNum: 1, pageSize: 100 })
    ])

    if (taskRes.code === 200) {
      currentTask.value = taskRes.data
      if (diffRes.code === 200) {
        currentTask.value.diffList = diffRes.data.records || diffRes.data.list || []
      }
    }
  } catch (error) {
    console.error('加载任务详情失败:', error)
    detailDialogVisible.value = false
  } finally {
    diffLoading.value = false
  }
}

// 导出报告
const handleExportReport = async (row) => {
  try {
    ElMessage.info('正在生成盘点报告...')

    const blob = await exportInventoryReport(row.id)

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `盘点报告_${row.taskNo}_${new Date().getTime()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('报告导出成功')
  } catch (error) {
    console.error('导出报告失败:', error)
  }
}

// 初始化
onMounted(() => {
  loadCategories()
  loadTaskList()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}

.text-danger {
  color: #f5222d;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
