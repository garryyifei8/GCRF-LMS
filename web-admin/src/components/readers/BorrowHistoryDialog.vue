<template>
  <el-dialog v-model="visible" title="借阅历史" width="800px" @close="handleClose">
    <div class="reader-info" v-if="reader">
      <strong>{{ reader.name }}</strong> — {{ reader.cardNo }} —
      {{ reader.department || reader.className || '' }}
    </div>
    <el-table :data="records" v-loading="loading" stripe style="margin-top: 12px">
      <el-table-column prop="bookId" label="图书ID" width="80" />
      <el-table-column prop="borrowDate" label="借阅日期" width="120">
        <template #default="{ row }">{{ formatDate(row.borrowDate) }}</template>
      </el-table-column>
      <el-table-column prop="dueDate" label="应还日期" width="120">
        <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
      </el-table-column>
      <el-table-column prop="returnDate" label="实际归还" width="120">
        <template #default="{ row }">{{
          row.returnDate ? formatDate(row.returnDate) : '-'
        }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="renewCount" label="续借次数" width="90" />
      <el-table-column prop="fineAmount" label="罚金(元)" width="90" />
    </el-table>
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadHistory"
      />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getReaderBorrowHistory } from '@/api/readers'

const props = defineProps({
  modelValue: Boolean,
  reader: Object
})
const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
    if (v && props.reader) {
      pageNum.value = 1
      loadHistory()
    }
  }
)
watch(visible, (v) => emit('update:modelValue', v))

const loadHistory = async () => {
  if (!props.reader) return
  loading.value = true
  try {
    const res = await getReaderBorrowHistory(props.reader.id, {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || '加载借阅历史失败')
    }
  } catch (e) {
    ElMessage.error('加载借阅历史失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  records.value = []
  total.value = 0
  pageNum.value = 1
}

const formatDate = (d) => (d ? d.toString().slice(0, 10) : '')
const statusTag = (s) =>
  ({ BORROWED: 'warning', RETURNED: 'success', OVERDUE: 'danger' })[s] || 'info'
const statusLabel = (s) => ({ BORROWED: '借阅中', RETURNED: '已归还', OVERDUE: '逾期' })[s] || s
</script>

<style lang="scss" scoped>
.reader-info {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
