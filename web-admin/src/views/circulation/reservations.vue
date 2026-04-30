<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">预约管理</h1>
      <p class="page-header-description">管理图书预约申请和处理通知</p>
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

          <el-form-item label="预约日期">
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
              <el-option label="待处理" value="pending" />
              <el-option label="可取书" value="ready" />
              <el-option label="已过期" value="expired" />
              <el-option label="已取消" value="cancelled" />
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
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="16" class="mb-md">
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">总预约数</div>
          <div class="stat-mini-card-value">{{ statistics.total }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">待处理</div>
          <div class="stat-mini-card-value text-warning">{{ statistics.pending }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">可取书</div>
          <div class="stat-mini-card-value text-success">{{ statistics.ready }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="6">
        <div class="stat-mini-card">
          <div class="stat-mini-card-label">已过期</div>
          <div class="stat-mini-card-value text-danger">{{ statistics.expired }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <div class="card">
      <div class="card-content">
        <el-table v-loading="loading" :data="reservationList" stripe>
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
          <el-table-column prop="reserveDate" label="预约日期" width="110" />
          <el-table-column label="到期日期" width="110">
            <template #default="{ row }">
              <span v-if="row.expiryDate" :class="isExpired(row) ? 'text-danger' : ''">
                {{ row.expiryDate }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'pending'" type="warning" size="small">待处理</el-tag>
              <el-tag v-else-if="row.status === 'ready'" type="success" size="small">可取书</el-tag>
              <el-tag v-else-if="row.status === 'expired'" type="danger" size="small"
                >已过期</el-tag
              >
              <el-tag v-else-if="row.status === 'cancelled'" type="info" size="small"
                >已取消</el-tag
              >
              <el-tag v-else type="info" size="small">已完成</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="通知状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.notified" type="success" size="small">已通知</el-tag>
              <el-tag v-else type="info" size="small">未通知</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <ActionIcons
                :actions="[
                  {
                    key: 'process',
                    label: '处理预约',
                    icon: IconCheck,
                    variant: 'success',
                    hidden: row.status !== 'pending'
                  },
                  {
                    key: 'cancel',
                    label: '取消预约',
                    icon: IconClose,
                    variant: 'danger',
                    hidden: row.status !== 'pending' && row.status !== 'ready'
                  },
                  {
                    key: 'notify',
                    label: '通知读者',
                    icon: IconMessage,
                    variant: 'primary',
                    hidden: !(row.status === 'ready' && !row.notified)
                  },
                  { key: 'view', label: '查看详情', icon: IconView, variant: 'primary' }
                ]"
                @action="
                  (k) => {
                    if (k === 'process') handleProcess(row)
                    else if (k === 'cancel') handleCancel(row)
                    else if (k === 'notify') handleNotify(row)
                    else handleView(row)
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
    <el-dialog v-model="detailDialogVisible" title="预约详情" width="700px">
      <el-descriptions v-if="currentReservation" :column="2" border>
        <el-descriptions-item label="预约编号" :span="2">{{
          currentReservation.reservationNo
        }}</el-descriptions-item>
        <el-descriptions-item label="图书名称" :span="2">{{
          currentReservation.bookTitle
        }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ currentReservation.isbn }}</el-descriptions-item>
        <el-descriptions-item label="条码号">{{ currentReservation.barcode }}</el-descriptions-item>
        <el-descriptions-item label="读者姓名">{{
          currentReservation.readerName
        }}</el-descriptions-item>
        <el-descriptions-item label="读者类型">
          {{ currentReservation.readerType === 'student' ? '学生' : '教师' }}
        </el-descriptions-item>
        <el-descriptions-item label="读者证号">{{
          currentReservation.readerCardNo
        }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{
          currentReservation.readerPhone
        }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{
          currentReservation.reserveDate
        }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">
          <span
            v-if="currentReservation.expiryDate"
            :class="isExpired(currentReservation) ? 'text-danger' : ''"
          >
            {{ currentReservation.expiryDate }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentReservation.status === 'pending'" type="warning">待处理</el-tag>
          <el-tag v-else-if="currentReservation.status === 'ready'" type="success">可取书</el-tag>
          <el-tag v-else-if="currentReservation.status === 'expired'" type="danger">已过期</el-tag>
          <el-tag v-else-if="currentReservation.status === 'cancelled'" type="info">已取消</el-tag>
          <el-tag v-else type="info">已完成</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知状态">
          <el-tag v-if="currentReservation.notified" type="success">已通知</el-tag>
          <el-tag v-else type="info">未通知</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{
          currentReservation.operator || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{
          currentReservation.processTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentReservation.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 处理预约对话框 -->
    <el-dialog v-model="processDialogVisible" title="处理预约" width="500px">
      <el-form
        ref="processFormRef"
        :model="processForm"
        :rules="processFormRules"
        label-width="100px"
      >
        <el-form-item label="预约信息">
          <div class="reservation-info">
            <div class="info-row">
              <span class="label">图书名称：</span>
              <span class="value">{{ currentReservation?.bookTitle }}</span>
            </div>
            <div class="info-row">
              <span class="label">读者姓名：</span>
              <span class="value">{{ currentReservation?.readerName }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="处理结果" prop="result">
          <el-radio-group v-model="processForm.result">
            <el-radio value="ready">图书已到馆，通知取书</el-radio>
            <el-radio value="cancel">无法满足，取消预约</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="processForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（选填）"
          />
        </el-form-item>
        <el-form-item label="发送通知" prop="sendNotification">
          <el-switch v-model="processForm.sendNotification" />
          <span class="form-tip">自动发送短信或邮件通知读者</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import ActionIcons from '@/components/ActionIcons.vue'
import {
  View as IconView,
  Check as IconCheck,
  Close as IconClose,
  Message as IconMessage
} from '@element-plus/icons-vue'
import {
  getReservations,
  cancelReservation,
  processReservation,
  notifyReservation
} from '@/api/circulation'

// 查询表单
const queryForm = reactive({
  keyword: '',
  dateRange: [],
  status: '',
  readerType: ''
})

// 表格数据
const loading = ref(false)
const reservationList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 统计信息
const statistics = reactive({
  total: 0,
  pending: 0,
  ready: 0,
  expired: 0
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentReservation = ref(null)

// 处理预约对话框
const processDialogVisible = ref(false)
const processFormRef = ref(null)
const processForm = reactive({
  result: 'ready',
  remark: '',
  sendNotification: true
})

const processFormRules = {
  result: [{ required: true, message: '请选择处理结果', trigger: 'change' }]
}

// 判断是否过期
const isExpired = (reservation) => {
  if (!reservation.expiryDate) return false
  return dayjs().isAfter(dayjs(reservation.expiryDate), 'day')
}

// 加载预约列表
const loadReservationList = async () => {
  loading.value = true
  try {
    const res = await getReservations({
      keyword: queryForm.keyword,
      status: queryForm.status,
      startDate: queryForm.dateRange?.[0],
      endDate: queryForm.dateRange?.[1],
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })

    if (res.code !== 200) {
      ElMessage.error(res.message || '加载预约列表失败')
      return
    }

    const data = res.data

    // 映射后端数据到前端格式
    reservationList.value = (data.records || data.list || []).map((reservation) => ({
      id: reservation.reservationId || reservation.id,
      reservationNo: reservation.reservationNo || `R${reservation.id}`,
      bookTitle: reservation.bookTitle,
      isbn: reservation.isbn,
      barcode: reservation.barcode,
      readerName: reservation.readerName,
      readerType: reservation.readerType,
      readerCardNo: reservation.readerCardNo || reservation.cardNumber,
      readerPhone: reservation.readerPhone || reservation.phone,
      reserveDate: dayjs(reservation.reserveDate).format('YYYY-MM-DD'),
      expiryDate: reservation.expiryDate
        ? dayjs(reservation.expiryDate).format('YYYY-MM-DD')
        : null,
      status: reservation.status,
      notified: reservation.notified || false,
      operator: reservation.operator,
      processTime: reservation.processTime
        ? dayjs(reservation.processTime).format('YYYY-MM-DD HH:mm:ss')
        : null,
      remark: reservation.remark || ''
    }))

    pagination.total = data.total || 0

    // 更新统计信息（如果后端提供了统计数据）
    if (data.statistics) {
      statistics.total = data.statistics.total || 0
      statistics.pending = data.statistics.pending || 0
      statistics.ready = data.statistics.ready || 0
      statistics.expired = data.statistics.expired || 0
    } else {
      // 否则根据返回的数据计算
      statistics.total = pagination.total
      statistics.pending = reservationList.value.filter((r) => r.status === 'pending').length
      statistics.ready = reservationList.value.filter((r) => r.status === 'ready').length
      statistics.expired = reservationList.value.filter((r) => r.status === 'expired').length
    }
  } catch (error) {
    console.error('加载预约列表失败:', error)
    ElMessage.error(error.message || '加载预约列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadReservationList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.dateRange = []
  queryForm.status = ''
  queryForm.readerType = ''
  pagination.page = 1
  loadReservationList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadReservationList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadReservationList()
}

// 查看详情
const handleView = (row) => {
  currentReservation.value = row
  detailDialogVisible.value = true
}

// 处理预约
const handleProcess = (row) => {
  currentReservation.value = row
  processForm.result = 'ready'
  processForm.remark = ''
  processForm.sendNotification = true
  processDialogVisible.value = true
}

// 提交处理
const submitProcess = async () => {
  try {
    await processFormRef.value.validate()

    const res = await processReservation(currentReservation.value.id, {
      result: processForm.result,
      remark: processForm.remark,
      sendNotification: processForm.sendNotification
    })

    if (res.code !== 200) {
      ElMessage.error(res.message || '处理失败')
      return
    }

    const action = processForm.result === 'ready' ? '处理成功，图书已标记为可取书' : '预约已取消'
    ElMessage.success(action)

    if (processForm.sendNotification) {
      ElMessage.info('通知已发送给读者')
    }

    processDialogVisible.value = false
    loadReservationList()
  } catch (error) {
    console.error('处理失败:', error)
    if (error !== false) {
      ElMessage.error(error.message || '处理失败')
    }
  }
}

// 取消预约
const handleCancel = (row) => {
  ElMessageBox.confirm(
    `确定要取消读者"${row.readerName}"对图书"${row.bookTitle}"的预约吗？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '返回'
    }
  )
    .then(async () => {
      try {
        const res = await cancelReservation({
          reservationId: row.id,
          remark: '管理员手动取消'
        })

        if (res.code !== 200) {
          ElMessage.error(res.message || '取消失败')
          return
        }

        ElMessage.success('预约已取消')
        loadReservationList()
      } catch (error) {
        console.error('取消失败:', error)
        ElMessage.error(error.message || '取消失败')
      }
    })
    .catch(() => {})
}

// 发送通知
const handleNotify = (row) => {
  ElMessageBox.confirm(`确定要发送取书通知给读者"${row.readerName}"吗？`, '提示', {
    type: 'info',
    confirmButtonText: '发送通知',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        const res = await notifyReservation(row.id)

        if (res.code !== 200) {
          ElMessage.error(res.message || '发送通知失败')
          return
        }

        ElMessage.success('通知已发送')
        loadReservationList()
      } catch (error) {
        console.error('发送通知失败:', error)
        ElMessage.error(error.message || '发送通知失败')
      }
    })
    .catch(() => {})
}

// 初始化
onMounted(() => {
  loadReservationList()
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

    &.text-warning {
      color: #faad14;
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

.text-danger {
  color: #f5222d;
}

.reservation-info {
  width: 100%;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;

  .info-row {
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }

    .label {
      font-weight: 500;
      color: rgba(0, 0, 0, 0.65);
    }

    .value {
      color: rgba(0, 0, 0, 0.85);
    }
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
