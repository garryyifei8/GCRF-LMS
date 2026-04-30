<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">图书归还</h1>
      <p class="page-header-description">扫描图书条码进行归还操作</p>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：图书扫描区 -->
      <el-col :xs="24" :md="10">
        <div class="lib-card mb-md">
          <div class="lib-card-header">
            <h3>图书扫描</h3>
          </div>
          <div class="lib-card-body">
            <!-- 图书条码扫描/输入 -->
            <el-input
              v-model="bookBarcode"
              placeholder="请扫描或输入图书条码"
              size="large"
              clearable
              @keyup.enter="scanBook"
            >
              <template #prepend>
                <el-icon><Barcode /></el-icon>
              </template>
              <template #append>
                <el-button :icon="Search" @click="scanBook">扫描</el-button>
              </template>
            </el-input>

            <!-- 扫描统计 -->
            <div v-if="returnList.length > 0" class="scan-stats">
              <el-statistic title="待归还图书" :value="returnList.length" suffix="本">
                <template #prefix>
                  <el-icon><Reading /></el-icon>
                </template>
              </el-statistic>
              <el-statistic
                title="逾期图书"
                :value="overdueCount"
                suffix="本"
                :value-style="{ color: overdueCount > 0 ? '#f5222d' : '#52c41a' }"
              >
                <template #prefix>
                  <el-icon><Warning /></el-icon>
                </template>
              </el-statistic>
              <el-statistic
                title="应缴罚款"
                :value="totalFine"
                suffix="元"
                :precision="2"
                :value-style="{ color: totalFine > 0 ? '#f5222d' : '#52c41a' }"
              >
                <template #prefix>
                  <el-icon><Money /></el-icon>
                </template>
              </el-statistic>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="returnList.length > 0" class="lib-card">
          <div class="lib-card-body">
            <el-button size="large" style="width: 100%" @click="clearReturnList"
              >清空列表</el-button
            >
            <el-button
              type="primary"
              size="large"
              style="width: 100%; margin-top: 12px"
              :loading="submitting"
              @click="confirmReturn"
            >
              确认归还 ({{ returnList.length }}本)
            </el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：归还清单 -->
      <el-col :xs="24" :md="14">
        <div class="lib-card">
          <div class="lib-card-header">
            <h3>归还清单</h3>
            <el-tag v-if="returnList.length > 0" class="ml-sm">{{ returnList.length }} 本</el-tag>
          </div>
          <div class="lib-card-body">
            <el-table v-if="returnList.length > 0" :data="returnList" stripe>
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="title" label="书名" show-overflow-tooltip />
              <el-table-column prop="readerName" label="读者" width="100" />
              <el-table-column prop="readerCardNo" label="读者证号" width="120" />
              <el-table-column label="借出日期" width="110">
                <template #default="{ row }">
                  {{ row.borrowDate }}
                </template>
              </el-table-column>
              <el-table-column label="应还日期" width="110">
                <template #default="{ row }">
                  <span :class="row.isOverdue ? 'text-danger' : ''">
                    {{ row.dueDate }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="逾期天数" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.overdueDays > 0" type="danger" size="small">
                    {{ row.overdueDays }} 天
                  </el-tag>
                  <el-tag v-else type="success" size="small">正常</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="罚款" width="100">
                <template #default="{ row }">
                  <span v-if="row.fine > 0" class="text-danger">¥{{ row.fine.toFixed(2) }}</span>
                  <span v-else class="text-success">¥0.00</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" fixed="right">
                <template #default="{ $index }">
                  <ActionIcons
                    :actions="[
                      { key: 'remove', label: '移除', icon: IconDelete, variant: 'danger' }
                    ]"
                    @action="() => removeBook($index)"
                  />
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-else description="暂无图书，请扫描图书条码添加" :image-size="100" />
          </div>
        </div>

        <!-- 罚款规则说明 -->
        <div v-if="returnList.length > 0 && overdueCount > 0" class="lib-card mt-md">
          <div class="lib-card-header">
            <h3>罚款规则</h3>
          </div>
          <div class="lib-card-body">
            <el-alert type="info" :closable="false">
              <template #title>
                <div>逾期罚款规则：</div>
                <div class="mt-sm">
                  1. 宽限期：{{ fineRules.graceDays }} 天内免罚款<br />
                  2. 每天罚款：¥{{ fineRules.finePerDay }}<br />
                  3. 罚款上限：¥{{ fineRules.maxFine }}
                </div>
              </template>
            </el-alert>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 归还确认对话框 -->
    <el-dialog v-model="confirmDialogVisible" title="确认归还" width="500px">
      <div class="confirm-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="归还图书数量"
            >{{ returnList.length }} 本</el-descriptions-item
          >
          <el-descriptions-item label="逾期图书数量">{{ overdueCount }} 本</el-descriptions-item>
          <el-descriptions-item label="应缴罚款">
            <span
              :class="totalFine > 0 ? 'text-danger' : 'text-success'"
              style="font-size: 18px; font-weight: 600"
            >
              ¥{{ totalFine.toFixed(2) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="totalFine > 0" type="warning" :closable="false" class="mt-md">
          <template #title>请确认读者已缴纳罚款后再进行归还操作</template>
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleConfirmReturn">
          确认归还
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import ActionIcons from '@/components/ActionIcons.vue'
import { Delete as IconDelete } from '@element-plus/icons-vue'
import { getBorrowRecordByBarcode, returnBook } from '@/api/circulation'

// 图书归还
const bookBarcode = ref('')
const returnList = ref([])
const submitting = ref(false)
const confirmDialogVisible = ref(false)

// 罚款规则配置
const fineRules = ref({
  graceDays: 3, // 宽限期天数
  finePerDay: 0.1, // 每天罚款
  maxFine: 50.0 // 罚款上限
})

// 计算逾期图书数量
const overdueCount = computed(() => {
  return returnList.value.filter((item) => item.isOverdue).length
})

// 计算总罚款
const totalFine = computed(() => {
  return returnList.value.reduce((sum, item) => sum + item.fine, 0)
})

// 扫描图书
const scanBook = async () => {
  if (!bookBarcode.value) {
    ElMessage.warning('请输入图书条码')
    return
  }

  // 检查是否已在清单中
  if (returnList.value.some((item) => item.barcode === bookBarcode.value)) {
    ElMessage.warning('该图书已在归还清单中')
    bookBarcode.value = ''
    return
  }

  try {
    const res = await getBorrowRecordByBarcode(bookBarcode.value)

    if (res.code !== 200 || !res.data) {
      ElMessage.error('未找到该图书的借阅记录')
      return
    }

    const recordData = res.data

    // 计算逾期天数
    const dueDate = dayjs(recordData.dueDate)
    const now = dayjs()
    const overdueDays = Math.max(0, now.diff(dueDate, 'day'))
    const isOverdue = overdueDays > 0

    // 计算罚款（宽限期内不罚款）
    let fine = 0
    if (isOverdue && overdueDays > fineRules.value.graceDays) {
      const billableDays = overdueDays - fineRules.value.graceDays
      fine = Math.min(billableDays * fineRules.value.finePerDay, fineRules.value.maxFine)
    }

    const record = {
      id: recordData.recordId || recordData.id,
      title: recordData.bookTitle,
      barcode: bookBarcode.value,
      readerName: recordData.readerName,
      readerCardNo: recordData.readerCardNo || recordData.cardNumber,
      borrowDate: dayjs(recordData.borrowDate).format('YYYY-MM-DD'),
      dueDate: dayjs(recordData.dueDate).format('YYYY-MM-DD'),
      overdueDays,
      isOverdue,
      fine
    }

    returnList.value.push(record)
    ElMessage.success(`《${record.title}》已添加到归还清单`)
    bookBarcode.value = ''
  } catch (error) {
    console.error('查询借阅记录失败:', error)
    ElMessage.error(error.message || '查询借阅记录失败')
  }
}

// 移除图书
const removeBook = (index) => {
  const book = returnList.value[index]
  returnList.value.splice(index, 1)
  ElMessage.info(`《${book.title}》已移除`)
}

// 清空归还清单
const clearReturnList = () => {
  ElMessageBox.confirm('确定要清空归还清单吗？', '提示', {
    type: 'warning'
  })
    .then(() => {
      returnList.value = []
      ElMessage.success('已清空归还清单')
    })
    .catch(() => {})
}

// 确认归还
const confirmReturn = () => {
  if (returnList.value.length === 0) {
    ElMessage.warning('归还清单为空')
    return
  }

  confirmDialogVisible.value = true
}

// 处理确认归还
const handleConfirmReturn = async () => {
  try {
    submitting.value = true

    // 为每本图书调用归还API
    const successCount = []
    const failedBooks = []

    for (const record of returnList.value) {
      try {
        const res = await returnBook({
          recordId: record.id,
          remark: record.fine > 0 ? `罚款: ¥${record.fine.toFixed(2)}` : ''
        })

        if (res.code === 200) {
          successCount.push(record.title)
        } else {
          failedBooks.push({ title: record.title, reason: res.message })
        }
      } catch (error) {
        failedBooks.push({ title: record.title, reason: error.message || '未知错误' })
      }
    }

    // 显示结果
    if (failedBooks.length === 0) {
      ElMessage.success(`成功归还 ${successCount.length} 本图书`)

      // 清空归还清单
      returnList.value = []
      bookBarcode.value = ''
      confirmDialogVisible.value = false
    } else {
      const successMsg = successCount.length > 0 ? `成功归还 ${successCount.length} 本，` : ''
      const failedMsg = `${failedBooks.length} 本失败`
      ElMessage.warning(`${successMsg}${failedMsg}`)

      // 从归还清单中移除成功的图书
      returnList.value = returnList.value.filter((record) =>
        failedBooks.some((fb) => fb.title === record.title)
      )

      // 如果全部失败则不关闭对话框
      if (successCount.length === 0) {
        confirmDialogVisible.value = false
      }
    }
  } catch (error) {
    console.error('归还失败:', error)
    ElMessage.error(error.message || '归还失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.scan-stats {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;

  :deep(.el-statistic) {
    .el-statistic__head {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.45);
      margin-bottom: 4px;
    }

    .el-statistic__content {
      font-size: 20px;
      font-weight: 600;
    }
  }
}

.text-danger {
  color: #f5222d;
}

.text-success {
  color: #52c41a;
}

.confirm-content {
  :deep(.el-descriptions__label) {
    width: 130px;
  }
}
</style>
