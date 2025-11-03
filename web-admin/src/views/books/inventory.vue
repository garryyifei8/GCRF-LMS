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
              <el-progress :percentage="row.progress" :status="row.progress === 100 ? 'success' : ''" />
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
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending'"
                type="success"
                link
                :icon="VideoPlay"
                @click="handleStartTask(row)"
              >
                开始盘点
              </el-button>
              <el-button type="primary" link :icon="View" @click="handleViewTask(row)">查看详情</el-button>
              <el-button
                v-if="row.status === 'in_progress'"
                type="warning"
                link
                :icon="VideoPause"
                @click="handlePauseTask(row)"
              >
                暂停
              </el-button>
              <el-button
                v-if="row.status === 'completed'"
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
          <el-select v-model="taskForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="文学" value="literature" />
            <el-option label="历史" value="history" />
            <el-option label="科学" value="science" />
            <el-option label="艺术" value="art" />
            <el-option label="计算机" value="computer" />
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
          <el-input v-model="taskForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTask">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 盘点详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="盘点任务详情" width="900px">
      <el-descriptions v-if="currentTask" :column="2" border>
        <el-descriptions-item label="任务编号">{{ currentTask.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="盘点范围">{{ getScopeName(currentTask.scope) }}</el-descriptions-item>
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
        <el-descriptions-item label="创建时间" :span="2">{{ currentTask.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>差异明细</el-divider>

      <el-table v-if="currentTask.diffList && currentTask.diffList.length > 0" :data="currentTask.diffList" stripe>
        <el-table-column prop="barcode" label="条码号" width="140" />
        <el-table-column prop="bookTitle" label="书名" />
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
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 表格数据
const loading = ref(false)
const taskList = ref([])

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

// 详情对话框
const detailDialogVisible = ref(false)
const currentTask = ref(null)

// 盘点范围映射
const scopeMap = {
  all: '全部馆藏',
  category: '指定分类',
  location: '指定位置',
  barcode_range: '指定条码范围'
}

// 状态映射
const statusMap = {
  pending: '待开始',
  in_progress: '进行中',
  paused: '已暂停',
  completed: '已完成',
  cancelled: '已取消'
}

// 获取范围名称
const getScopeName = (scope) => scopeMap[scope] || scope

// 获取状态名称
const getStatusName = (status) => statusMap[status] || status

// 获取状态标签类型
const getStatusType = (status) => {
  const typeMap = {
    pending: 'info',
    in_progress: '',
    paused: 'warning',
    completed: 'success',
    cancelled: 'danger'
  }
  return typeMap[status] || ''
}

// 加载任务列表
const loadTaskList = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取任务列表
    await new Promise((resolve) => setTimeout(resolve, 500))

    taskList.value = [
      {
        id: 1,
        taskNo: 'INV2025100001',
        taskName: '2025年第一季度全馆盘点',
        scope: 'all',
        status: 'completed',
        progress: 100,
        totalCount: 1856,
        checkedCount: 1856,
        diffCount: 5,
        creator: '管理员',
        createdAt: '2025-10-01 09:00:00',
        diffList: [
          {
            barcode: 'B00001234',
            bookTitle: '活着',
            diffType: 'missing',
            expectedStatus: '在库',
            actualStatus: '缺失',
            location: 'A区3层'
          }
        ]
      },
      {
        id: 2,
        taskNo: 'INV2025100002',
        taskName: '计算机类图书盘点',
        scope: 'category',
        status: 'in_progress',
        progress: 65,
        totalCount: 300,
        checkedCount: 195,
        diffCount: 2,
        creator: '张三',
        createdAt: '2025-10-08 14:30:00',
        diffList: []
      },
      {
        id: 3,
        taskNo: 'INV2025100003',
        taskName: 'A区图书盘点',
        scope: 'location',
        status: 'pending',
        progress: 0,
        totalCount: 450,
        checkedCount: 0,
        diffCount: 0,
        creator: '李四',
        createdAt: '2025-10-10 10:00:00',
        diffList: []
      }
    ]
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
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

// 提交任务
const handleSubmitTask = async () => {
  try {
    await taskFormRef.value.validate()

    // TODO: 调用API创建任务
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('盘点任务创建成功')
    taskDialogVisible.value = false
    loadTaskList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('创建失败')
    }
  }
}

// 开始任务
const handleStartTask = (row) => {
  ElMessageBox.confirm('确定要开始盘点任务吗？', '提示', {
    type: 'info'
  })
    .then(async () => {
      // TODO: 调用API开始任务
      await new Promise((resolve) => setTimeout(resolve, 500))

      ElMessage.success('盘点任务已开始')
      loadTaskList()
    })
    .catch(() => {})
}

// 暂停任务
const handlePauseTask = (row) => {
  ElMessageBox.confirm('确定要暂停盘点任务吗？', '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API暂停任务
      await new Promise((resolve) => setTimeout(resolve, 500))

      ElMessage.success('盘点任务已暂停')
      loadTaskList()
    })
    .catch(() => {})
}

// 查看详情
const handleViewTask = (row) => {
  currentTask.value = row
  detailDialogVisible.value = true
}

// 导出报告
const handleExportReport = (row) => {
  // TODO: 实现导出功能
  ElMessage.success('盘点报告导出功能开发中')
}

// 初始化
loadTaskList()
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
</style>
