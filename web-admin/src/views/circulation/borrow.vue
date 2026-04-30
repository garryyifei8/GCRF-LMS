<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">图书借出</h1>
      <p class="page-header-description">扫描读者证和图书条码进行借阅操作</p>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：读者信息区 -->
      <el-col :xs="24" :md="10">
        <div class="lib-card mb-md">
          <div class="lib-card-header">
            <h3>读者信息</h3>
          </div>
          <div class="lib-card-body">
            <!-- 读者证扫描/输入 -->
            <el-input
              v-model="readerCardNo"
              placeholder="请扫描或输入读者证号"
              size="large"
              clearable
              @keyup.enter="searchReader"
            >
              <template #prepend>
                <el-icon><CreditCard /></el-icon>
              </template>
              <template #append>
                <el-button :icon="Search" @click="searchReader">查询</el-button>
              </template>
            </el-input>

            <!-- 读者详细信息 -->
            <div v-if="readerInfo" class="reader-info">
              <el-descriptions :column="1" border class="mt-md">
                <el-descriptions-item label="姓名">
                  <el-tag>{{ readerInfo.name }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="读者类型">
                  {{ readerInfo.type === 'student' ? '学生' : '教师' }}
                </el-descriptions-item>
                <el-descriptions-item label="班级/部门">
                  {{ readerInfo.grade || readerInfo.department }}
                </el-descriptions-item>
                <el-descriptions-item label="读者证号">
                  {{ readerInfo.cardNo }}
                </el-descriptions-item>
                <el-descriptions-item label="已借图书">
                  <span
                    :class="
                      readerInfo.borrowedCount >= readerInfo.maxBorrow
                        ? 'text-danger'
                        : 'text-primary'
                    "
                  >
                    {{ readerInfo.borrowedCount }} / {{ readerInfo.maxBorrow }}
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="可借天数">
                  {{ readerInfo.borrowDays }} 天
                </el-descriptions-item>
                <el-descriptions-item label="账户状态">
                  <el-tag v-if="readerInfo.status === 'normal'" type="success">正常</el-tag>
                  <el-tag v-else-if="readerInfo.status === 'frozen'" type="danger">冻结</el-tag>
                  <el-tag v-else type="warning">异常</el-tag>
                </el-descriptions-item>
              </el-descriptions>

              <!-- 当前借阅图书列表 -->
              <div
                v-if="readerInfo.currentBorrows && readerInfo.currentBorrows.length > 0"
                class="mt-md"
              >
                <div class="section-title">当前借阅</div>
                <el-table :data="readerInfo.currentBorrows" size="small" stripe>
                  <el-table-column prop="title" label="书名" show-overflow-tooltip />
                  <el-table-column prop="borrowDate" label="借出日期" width="100" />
                  <el-table-column prop="dueDate" label="应还日期" width="100">
                    <template #default="{ row }">
                      <span :class="isOverdue(row.dueDate) ? 'text-danger' : ''">
                        {{ row.dueDate }}
                      </span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>

            <el-empty v-else description="请输入读者证号查询读者信息" :image-size="80" />
          </div>
        </div>
      </el-col>

      <!-- 右侧：图书借出区 -->
      <el-col :xs="24" :md="14">
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
              :disabled="!readerInfo || readerInfo.status !== 'normal'"
              @keyup.enter="addBook"
            >
              <template #prepend>
                <el-icon><Barcode /></el-icon>
              </template>
              <template #append>
                <el-button
                  :icon="Plus"
                  :disabled="!readerInfo || readerInfo.status !== 'normal'"
                  @click="addBook"
                >
                  添加
                </el-button>
              </template>
            </el-input>

            <!-- 提示信息 -->
            <el-alert v-if="!readerInfo" type="warning" :closable="false" class="mt-md" show-icon>
              <template #title>请先扫描读者证</template>
            </el-alert>

            <el-alert
              v-else-if="readerInfo.status !== 'normal'"
              type="error"
              :closable="false"
              class="mt-md"
              show-icon
            >
              <template #title>该读者账户状态异常，无法借阅</template>
            </el-alert>

            <el-alert
              v-else-if="readerInfo.borrowedCount >= readerInfo.maxBorrow"
              type="error"
              :closable="false"
              class="mt-md"
              show-icon
            >
              <template #title>已达借阅上限，无法继续借阅</template>
            </el-alert>
          </div>
        </div>

        <!-- 借阅清单 -->
        <div class="lib-card">
          <div class="lib-card-header">
            <h3>借阅清单</h3>
            <el-tag v-if="borrowList.length > 0" class="ml-sm">{{ borrowList.length }} 本</el-tag>
          </div>
          <div class="lib-card-body">
            <el-table v-if="borrowList.length > 0" :data="borrowList" stripe>
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="title" label="书名" show-overflow-tooltip />
              <el-table-column prop="author" label="作者" width="120" />
              <el-table-column prop="isbn" label="ISBN" width="140" />
              <el-table-column prop="barcode" label="条码号" width="140" />
              <el-table-column label="应还日期" width="120">
                <template #default>
                  {{ dueDate }}
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

            <!-- 操作按钮 -->
            <div v-if="borrowList.length > 0" class="mt-md text-right">
              <el-button @click="clearBorrowList">清空列表</el-button>
              <el-button type="primary" :loading="submitting" @click="confirmBorrow">
                确认借出 ({{ borrowList.length }}本)
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import ActionIcons from '@/components/ActionIcons.vue'
import { Delete as IconDelete } from '@element-plus/icons-vue'
import { getReaderByCardNumber } from '@/api/readers'
import { getBookByBarcode } from '@/api/books'
import { borrowBook } from '@/api/circulation'

// 读者信息
const readerCardNo = ref('')
const readerInfo = ref(null)

// 图书借阅
const bookBarcode = ref('')
const borrowList = ref([])
const submitting = ref(false)

// 计算应还日期
const dueDate = computed(() => {
  if (!readerInfo.value) return ''
  return dayjs().add(readerInfo.value.borrowDays, 'day').format('YYYY-MM-DD')
})

// 查询读者信息
const searchReader = async () => {
  if (!readerCardNo.value) {
    ElMessage.warning('请输入读者证号')
    return
  }

  try {
    const res = await getReaderByCardNumber(readerCardNo.value)

    if (res.code !== 200 || !res.data) {
      ElMessage.error('读者证号不存在')
      readerInfo.value = null
      return
    }

    const reader = res.data

    // 映射后端字段到前端字段
    readerInfo.value = {
      id: reader.readerId || reader.id,
      name: reader.realName,
      type: reader.readerType,
      grade: reader.grade,
      department: reader.department,
      cardNo: reader.cardNumber,
      borrowedCount: reader.currentBorrowCount || 0,
      maxBorrow: reader.maxBorrowCount || 5,
      borrowDays: reader.borrowDays || 30,
      status: reader.status === 'active' ? 'normal' : reader.status,
      currentBorrows: reader.currentBorrows || []
    }

    ElMessage.success('读者信息加载成功')
    // 清空之前的借阅清单
    borrowList.value = []
  } catch (error) {
    console.error('查询读者信息失败:', error)
    ElMessage.error(error.message || '查询读者信息失败')
    readerInfo.value = null
  }
}

// 添加图书到借阅清单
const addBook = async () => {
  if (!bookBarcode.value) {
    ElMessage.warning('请输入图书条码')
    return
  }

  // 检查是否已达借阅上限
  if (readerInfo.value.borrowedCount + borrowList.value.length >= readerInfo.value.maxBorrow) {
    ElMessage.error('已达借阅上限')
    return
  }

  // 检查是否已在清单中
  if (borrowList.value.some((item) => item.barcode === bookBarcode.value)) {
    ElMessage.warning('该图书已在借阅清单中')
    bookBarcode.value = ''
    return
  }

  try {
    const res = await getBookByBarcode(bookBarcode.value)

    if (res.code !== 200 || !res.data) {
      ElMessage.error('图书条码不存在')
      return
    }

    const bookData = res.data

    // 检查图书状态
    if (bookData.status !== 'available' && bookData.availableCopies <= 0) {
      ElMessage.error('该图书不可借阅')
      return
    }

    const book = {
      id: bookData.bookId || bookData.id,
      title: bookData.title,
      author: bookData.author,
      isbn: bookData.isbn,
      barcode: bookBarcode.value,
      status: bookData.status
    }

    borrowList.value.push(book)
    ElMessage.success(`《${book.title}》已添加到借阅清单`)
    bookBarcode.value = ''
  } catch (error) {
    console.error('图书信息查询失败:', error)
    ElMessage.error(error.message || '图书信息查询失败')
  }
}

// 移除图书
const removeBook = (index) => {
  const book = borrowList.value[index]
  borrowList.value.splice(index, 1)
  ElMessage.info(`《${book.title}》已移除`)
}

// 清空借阅清单
const clearBorrowList = () => {
  ElMessageBox.confirm('确定要清空借阅清单吗？', '提示', {
    type: 'warning'
  })
    .then(() => {
      borrowList.value = []
      ElMessage.success('已清空借阅清单')
    })
    .catch(() => {})
}

// 确认借出
const confirmBorrow = async () => {
  if (borrowList.value.length === 0) {
    ElMessage.warning('借阅清单为空')
    return
  }

  try {
    submitting.value = true

    // 为每本图书调用借阅API
    const successCount = []
    const failedBooks = []

    for (const book of borrowList.value) {
      try {
        const res = await borrowBook({
          readerId: readerInfo.value.id,
          bookId: book.id,
          remark: ''
        })

        if (res.code === 200) {
          successCount.push(book.title)
        } else {
          failedBooks.push({ title: book.title, reason: res.message })
        }
      } catch (error) {
        failedBooks.push({ title: book.title, reason: error.message || '未知错误' })
      }
    }

    // 显示结果
    if (failedBooks.length === 0) {
      ElMessage.success(`成功借出 ${successCount.length} 本图书`)

      // 更新读者已借数量
      readerInfo.value.borrowedCount += successCount.length

      // 清空借阅清单
      borrowList.value = []
      bookBarcode.value = ''
    } else {
      const successMsg = successCount.length > 0 ? `成功借出 ${successCount.length} 本，` : ''
      const failedMsg = `${failedBooks.length} 本失败`
      ElMessage.warning(`${successMsg}${failedMsg}`)

      // 从借阅清单中移除成功的图书
      borrowList.value = borrowList.value.filter((book) =>
        failedBooks.some((fb) => fb.title === book.title)
      )

      // 更新读者已借数量
      if (successCount.length > 0) {
        readerInfo.value.borrowedCount += successCount.length
      }
    }
  } catch (error) {
    console.error('借出失败:', error)
    ElMessage.error(error.message || '借出失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 判断是否逾期
const isOverdue = (dueDate) => {
  return dayjs(dueDate).isBefore(dayjs(), 'day')
}
</script>

<style lang="scss" scoped>
.reader-info {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}

.text-danger {
  color: #f5222d;
}

.text-primary {
  color: #1890ff;
}
</style>
