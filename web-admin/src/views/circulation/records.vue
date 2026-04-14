<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">流通记录</h1>
      <p class="page-header-description">查询和管理图书借阅归还记录</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="请输入读者姓名、图书名称或条码"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="借阅日期">
            <el-date-picker
              v-model="queryForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>

          <el-form-item label="状态">
            <el-select
              v-model="queryForm.status"
              placeholder="全部状态"
              clearable
              style="width: 120px"
            >
              <el-option label="全部" value="" />
              <el-option label="借出" value="borrowed" />
              <el-option label="已归还" value="returned" />
              <el-option label="逾期" value="overdue" />
            </el-select>
          </el-form-item>

          <el-form-item label="读者类型">
            <el-select
              v-model="queryForm.readerType"
              placeholder="全部类型"
              clearable
              style="width: 120px"
            >
              <el-option label="全部" value="" />
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button type="success" :icon="Download" @click="handleExport">导出Excel</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="16" class="mb-md">
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">总借阅次数</div>
          <div class="stat-mini-card-value">{{ statistics.totalBorrows }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">当前借出</div>
          <div class="stat-mini-card-value text-primary">{{ statistics.currentBorrowed }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">已归还</div>
          <div class="stat-mini-card-value text-success">{{ statistics.returned }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">逾期未还</div>
          <div class="stat-mini-card-value text-danger">{{ statistics.overdue }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <div class="card">
      <div class="card-content">
        <el-table v-loading="loading" :data="recordList" stripe>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column
            prop="bookTitle"
            label="图书名称"
            show-overflow-tooltip
            min-width="180"
          />
          <el-table-column prop="barcode" label="条码号" width="140" />
          <el-table-column label="读者" width="120">
            <template #default="{ row }">
              <div>{{ row.readerName }}</div>
              <el-tag size="small" :type="row.readerType === 'student' ? '' : 'success'">
                {{ row.readerType === 'student' ? '学生' : '教师' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="readerCardNo" label="读者证号" width="140" />
          <el-table-column prop="borrowDate" label="借出日期" width="110" />
          <el-table-column prop="dueDate" label="应还日期" width="110">
            <template #default="{ row }">
              <span :class="isOverdue(row) ? 'text-danger' : ''">
                {{ row.dueDate }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="归还日期" width="110">
            <template #default="{ row }">
              {{ row.returnDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'returned'" type="success" size="small">已归还</el-tag>
              <el-tag v-else-if="isOverdue(row)" type="danger" size="small">逾期</el-tag>
              <el-tag v-else type="warning" size="small">借出中</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="逾期天数" width="100">
            <template #default="{ row }">
              <span v-if="row.overdueDays > 0" class="text-danger">{{ row.overdueDays }} 天</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="罚款金额" width="100">
            <template #default="{ row }">
              <span v-if="row.fine > 0" class="text-danger">¥{{ row.fine.toFixed(2) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="handleView(row)">详情</el-button>
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
    <el-dialog v-model="detailDialogVisible" title="流通记录详情" width="700px">
      <el-descriptions v-if="currentRecord" :column="2" border>
        <el-descriptions-item label="图书名称" :span="2">{{
          currentRecord.bookTitle
        }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ currentRecord.isbn }}</el-descriptions-item>
        <el-descriptions-item label="条码号">{{ currentRecord.barcode }}</el-descriptions-item>
        <el-descriptions-item label="读者姓名">{{ currentRecord.readerName }}</el-descriptions-item>
        <el-descriptions-item label="读者类型">
          {{ currentRecord.readerType === 'student' ? '学生' : '教师' }}
        </el-descriptions-item>
        <el-descriptions-item label="读者证号">{{
          currentRecord.readerCardNo
        }}</el-descriptions-item>
        <el-descriptions-item label="年级/部门">{{
          currentRecord.readerGrade || currentRecord.readerDepartment
        }}</el-descriptions-item>
        <el-descriptions-item label="借出日期">{{ currentRecord.borrowDate }}</el-descriptions-item>
        <el-descriptions-item label="应还日期">
          <span :class="isOverdue(currentRecord) ? 'text-danger' : ''">
            {{ currentRecord.dueDate }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="归还日期">
          {{ currentRecord.returnDate || '未归还' }}
        </el-descriptions-item>
        <el-descriptions-item label="借阅天数">
          {{ calculateBorrowDays(currentRecord) }} 天
        </el-descriptions-item>
        <el-descriptions-item label="逾期天数">
          <span v-if="currentRecord.overdueDays > 0" class="text-danger"
            >{{ currentRecord.overdueDays }} 天</span
          >
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="罚款金额">
          <span v-if="currentRecord.fine > 0" class="text-danger"
            >¥{{ currentRecord.fine.toFixed(2) }}</span
          >
          <span v-else>¥0.00</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentRecord.status === 'returned'" type="success">已归还</el-tag>
          <el-tag v-else-if="isOverdue(currentRecord)" type="danger">逾期</el-tag>
          <el-tag v-else type="warning">借出中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="办理人">{{ currentRecord.operator }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentRecord.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getCirculationRecords } from '@/api/circulation'

// 查询表单
const queryForm = reactive({
  keyword: '',
  dateRange: [],
  status: '',
  readerType: ''
})

// 表格数据
const loading = ref(false)
const recordList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 统计信息
const statistics = reactive({
  totalBorrows: 0,
  currentBorrowed: 0,
  returned: 0,
  overdue: 0
})

// 详情
const detailDialogVisible = ref(false)
const currentRecord = ref(null)

// 判断是否逾期
const isOverdue = (record) => {
  if (record.status === 'returned') return false
  return dayjs().isAfter(dayjs(record.dueDate), 'day')
}

// 计算借阅天数
const calculateBorrowDays = (record) => {
  const start = dayjs(record.borrowDate)
  const end = record.returnDate ? dayjs(record.returnDate) : dayjs()
  return end.diff(start, 'day')
}

// 加载流通记录
const loadRecordList = async () => {
  loading.value = true
  try {
    const res = await getCirculationRecords({
      keyword: queryForm.keyword,
      status: queryForm.status,
      startDate: queryForm.dateRange?.[0],
      endDate: queryForm.dateRange?.[1],
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })

    if (res.code !== 200) {
      ElMessage.error(res.message || '加载流通记录失败')
      return
    }

    const data = res.data

    // 映射后端数据到前端格式
    recordList.value = (data.records || data.list || []).map((record) => {
      const borrowDate = dayjs(record.borrowDate)
      const dueDate = dayjs(record.dueDate)
      const returnDate = record.returnDate ? dayjs(record.returnDate) : null
      const overdueDays = returnDate
        ? Math.max(0, returnDate.diff(dueDate, 'day'))
        : Math.max(0, dayjs().diff(dueDate, 'day'))

      return {
        id: record.recordId || record.id,
        bookTitle: record.bookTitle,
        isbn: record.isbn,
        barcode: record.barcode,
        readerName: record.readerName,
        readerType: record.readerType,
        readerCardNo: record.readerCardNo || record.cardNumber,
        readerGrade: record.readerGrade,
        readerDepartment: record.readerDepartment,
        borrowDate: borrowDate.format('YYYY-MM-DD'),
        dueDate: dueDate.format('YYYY-MM-DD'),
        returnDate: returnDate ? returnDate.format('YYYY-MM-DD') : null,
        status: record.status,
        overdueDays: record.status !== 'returned' ? overdueDays : 0,
        fine: record.fine || 0,
        operator: record.operator || '系统',
        remark: record.remark || ''
      }
    })

    pagination.total = data.total || 0

    // 更新统计信息（如果后端提供了统计数据）
    if (data.statistics) {
      statistics.totalBorrows = data.statistics.totalBorrows || 0
      statistics.currentBorrowed = data.statistics.currentBorrowed || 0
      statistics.returned = data.statistics.returned || 0
      statistics.overdue = data.statistics.overdue || 0
    } else {
      // 否则根据返回的数据计算
      statistics.totalBorrows = pagination.total
      statistics.currentBorrowed = recordList.value.filter((r) => r.status === 'borrowed').length
      statistics.returned = recordList.value.filter((r) => r.status === 'returned').length
      statistics.overdue = recordList.value.filter(
        (r) => r.status === 'borrowed' && isOverdue(r)
      ).length
    }
  } catch (error) {
    console.error('加载流通记录失败:', error)
    ElMessage.error(error.message || '加载流通记录失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadRecordList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.dateRange = []
  queryForm.status = ''
  queryForm.readerType = ''
  pagination.page = 1
  loadRecordList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadRecordList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadRecordList()
}

// 查看详情
const handleView = (row) => {
  currentRecord.value = row
  detailDialogVisible.value = true
}

// 导出Excel
const handleExport = async () => {
  try {
    // 获取所有数据（不分页）
    const res = await getCirculationRecords({
      keyword: queryForm.keyword,
      status: queryForm.status,
      startDate: queryForm.dateRange?.[0],
      endDate: queryForm.dateRange?.[1],
      pageNum: 1,
      pageSize: 10000 // 获取所有数据
    })

    if (res.code !== 200) {
      ElMessage.error('导出失败')
      return
    }

    const records = res.data.records || res.data.list || []

    // 构建CSV内容
    const headers = [
      '序号',
      '图书名称',
      '条码号',
      '读者姓名',
      '读者类型',
      '读者证号',
      '借出日期',
      '应还日期',
      '归还日期',
      '状态',
      '逾期天数',
      '罚款金额',
      '备注'
    ]
    const csvRows = [headers.join(',')]

    records.forEach((record, index) => {
      const row = [
        index + 1,
        `"${record.bookTitle}"`,
        record.barcode,
        record.readerName,
        record.readerType === 'student' ? '学生' : '教师',
        record.readerCardNo || record.cardNumber,
        dayjs(record.borrowDate).format('YYYY-MM-DD'),
        dayjs(record.dueDate).format('YYYY-MM-DD'),
        record.returnDate ? dayjs(record.returnDate).format('YYYY-MM-DD') : '',
        record.status === 'returned' ? '已归还' : '借出中',
        record.overdueDays || 0,
        record.fine || 0,
        `"${record.remark || ''}"`
      ]
      csvRows.push(row.join(','))
    })

    // 创建Blob并下载
    const csvContent = '\uFEFF' + csvRows.join('\n') // 添加BOM以支持中文
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)

    link.setAttribute('href', url)
    link.setAttribute('download', `流通记录_${dayjs().format('YYYYMMDD_HHmmss')}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error(error.message || '导出失败')
  }
}

// 初始化
onMounted(() => {
  loadRecordList()
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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-primary {
  color: #1890ff;
}

.text-success {
  color: #52c41a;
}

.text-danger {
  color: #f5222d;
}
</style>
