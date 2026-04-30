<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">教师读者管理</h1>
      <p class="page-header-description">管理教师读者信息和读者证</p>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="lib-card mb-md">
      <div class="lib-card-body">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="请输入姓名、工号或读者证号"
              clearable
              style="width: 280px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="部门">
            <el-select
              v-model="queryForm.department"
              placeholder="全部部门"
              clearable
              style="width: 140px"
            >
              <el-option label="全部" value="" />
              <el-option label="语文组" value="chinese" />
              <el-option label="数学组" value="math" />
              <el-option label="英语组" value="english" />
              <el-option label="物理组" value="physics" />
              <el-option label="化学组" value="chemistry" />
              <el-option label="行政部门" value="admin" />
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
              <el-option label="正常" value="normal" />
              <el-option label="冻结" value="frozen" />
              <el-option label="注销" value="disabled" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button type="success" :icon="Plus" @click="handleAdd">新增教师</el-button>
            <el-button type="warning" :icon="Upload" @click="handleImport">批量导入</el-button>
            <el-button :icon="Download" @click="handleExport">导出数据</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="lib-card">
      <div class="lib-card-body">
        <!-- 批量操作 -->
        <div v-if="selectedTeachers.length > 0" class="batch-actions">
          <span class="batch-info">已选择 {{ selectedTeachers.length }} 项</span>
          <el-button type="danger" size="small" :icon="Delete" @click="handleBatchDelete"
            >批量删除</el-button
          >
          <el-button size="small" @click="handleClearSelection">取消选择</el-button>
        </div>

        <!-- 表格 -->
        <el-table
          v-loading="loading"
          :data="teacherList"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-image
                :src="row.avatar || defaultAvatar"
                :preview-src-list="[row.avatar || defaultAvatar]"
                class="teacher-avatar"
                fit="cover"
                :preview-teleported="true"
              >
                <template #error>
                  <div class="avatar-placeholder">
                    <el-icon><User /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="teacherNo" label="工号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="gender" label="性别" width="60">
            <template #default="{ row }">
              {{ row.gender === 'male' ? '男' : '女' }}
            </template>
          </el-table-column>
          <el-table-column label="部门" width="120">
            <template #default="{ row }">
              {{ getDepartmentName(row.department) }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="职称" width="100" />
          <el-table-column prop="phone" label="联系电话" width="130" />
          <el-table-column prop="cardNo" label="读者证号" width="140" />
          <el-table-column label="借阅情况" width="100">
            <template #default="{ row }">
              <span :class="row.borrowedCount > 0 ? 'text-primary' : ''">
                {{ row.borrowedCount }} / {{ row.maxBorrow }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="账户状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'normal'" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="row.status === 'frozen'" type="danger" size="small">冻结</el-tag>
              <el-tag v-else type="info" size="small">注销</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="110" />
          <el-table-column label="操作" width="290" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="handleView(row)">查看</el-button>
              <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-button type="primary" link @click="showBorrowHistory(row)">借阅历史</el-button>
              <el-button
                v-if="row.status !== 'frozen'"
                type="warning"
                link
                :icon="Lock"
                @click="handleFreeze(row)"
              >
                冻结
              </el-button>
              <el-button v-else type="success" link :icon="Unlock" @click="handleUnfreeze(row)"
                >解冻</el-button
              >
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)"
                >删除</el-button
              >
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="工号" prop="teacherNo">
          <el-input v-model="formData.teacherNo" placeholder="请输入工号" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="male">男</el-radio>
            <el-radio label="female">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-select v-model="formData.department" placeholder="请选择部门" style="width: 100%">
                <el-option label="语文组" value="chinese" />
                <el-option label="数学组" value="math" />
                <el-option label="英语组" value="english" />
                <el-option label="物理组" value="physics" />
                <el-option label="化学组" value="chemistry" />
                <el-option label="行政部门" value="admin" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-select v-model="formData.title" placeholder="请选择职称" style="width: 100%">
                <el-option label="助教" value="assistant" />
                <el-option label="讲师" value="lecturer" />
                <el-option label="副教授" value="associate_professor" />
                <el-option label="教授" value="professor" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="formData.idCard" placeholder="请输入身份证号" />
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入电子邮箱" />
        </el-form-item>

        <el-form-item label="办公地址">
          <el-input v-model="formData.office" placeholder="请输入办公地址" />
        </el-form-item>

        <el-form-item label="头像上传">
          <AvatarUpload
            v-model="formData.avatar"
            :size="120"
            shape="circle"
            :enable-camera="true"
            :show-actions="true"
            :show-tips="true"
          />
        </el-form-item>

        <el-form-item v-if="!isEdit" label="读者证号">
          <el-input v-model="formData.cardNo" placeholder="自动生成或手动输入" />
          <div class="form-tip">留空将自动生成读者证号</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入教师" width="600px">
      <el-alert type="info" :closable="false" class="mb-md">
        <template #title>
          <div>导入说明：</div>
          <div class="mt-sm">
            1. 请下载并填写导入模板<br />
            2. 支持Excel格式(.xlsx, .xls)<br />
            3. 单次最多导入500条数据
          </div>
        </template>
      </el-alert>

      <div class="import-actions">
        <el-button :icon="Download" @click="handleDownloadTemplate">下载导入模板</el-button>
      </div>

      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx/.xls 格式文件，大小不超过5MB</div>
        </template>
      </el-upload>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleConfirmImport"
          >确认导入</el-button
        >
      </template>
    </el-dialog>

    <!-- 借阅历史对话框 -->
    <BorrowHistoryDialog v-model="showHistory" :reader="currentReader" />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="教师详情" width="900px">
      <div v-if="currentTeacher" class="teacher-detail">
        <div class="teacher-detail-avatar">
          <el-image
            :src="currentTeacher.avatar || defaultAvatar"
            :preview-src-list="[currentTeacher.avatar || defaultAvatar]"
            class="detail-avatar-image"
            fit="cover"
          >
            <template #error>
              <div class="detail-avatar-placeholder">
                <el-icon><User /></el-icon>
                <p>暂无头像</p>
              </div>
            </template>
          </el-image>
        </div>

        <div class="teacher-detail-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工号">{{ currentTeacher.teacherNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ currentTeacher.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ currentTeacher.gender === 'male' ? '男' : '女' }}
            </el-descriptions-item>
            <el-descriptions-item label="部门">
              {{ getDepartmentName(currentTeacher.department) }}
            </el-descriptions-item>
            <el-descriptions-item label="职称">{{ currentTeacher.title }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentTeacher.phone }}</el-descriptions-item>
            <el-descriptions-item label="读者证号">{{
              currentTeacher.cardNo
            }}</el-descriptions-item>
            <el-descriptions-item label="电子邮箱">{{
              currentTeacher.email || '暂无'
            }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{
              currentTeacher.idCard
            }}</el-descriptions-item>
            <el-descriptions-item label="借阅情况">
              {{ currentTeacher.borrowedCount }} / {{ currentTeacher.maxBorrow }}
            </el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <el-tag v-if="currentTeacher.status === 'normal'" type="success">正常</el-tag>
              <el-tag v-else-if="currentTeacher.status === 'frozen'" type="danger">冻结</el-tag>
              <el-tag v-else type="info">注销</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{
              currentTeacher.createdAt
            }}</el-descriptions-item>
            <el-descriptions-item label="办公地址" :span="2">
              {{ currentTeacher.office || '暂无' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AvatarUpload from '@/components/AvatarUpload.vue'
import BorrowHistoryDialog from '@/components/readers/BorrowHistoryDialog.vue'
import {
  getReaders,
  getReaderById,
  createReader,
  updateReader,
  deleteReader,
  batchDeleteReaders,
  updateReaderStatus
} from '@/api/readers'
import { exportExcel, readExcel, downloadTemplate } from '@/utils/excel'

// 查询表单
const queryForm = reactive({
  keyword: '',
  department: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const teacherList = ref([])
const selectedTeachers = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑教师' : '新增教师'))
const submitting = ref(false)

// 表单
const formRef = ref()
const formData = reactive({
  teacherNo: '',
  name: '',
  gender: 'male',
  department: '',
  title: '',
  phone: '',
  idCard: '',
  email: '',
  office: '',
  cardNo: '',
  avatar: ''
})

// 默认头像
const defaultAvatar =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iIzY2N2VlYSIvPjxwYXRoIGQ9Ik01MCwyMGMxMSwwLDIwLDksMjAsMjBzLTksMjAtMjAsMjAtMjAtOS0yMC0yMFMzOSwyMCw1MCwyMHpNMjAsODBjMC0xNiwxMy41LTMwLDMwLTMwczMwLDE0LDMwLDMwIiBmaWxsPSIjZmZmIi8+PC9zdmc+'

const formRules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    {
      pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/,
      message: '请输入正确的邮箱',
      trigger: 'blur'
    }
  ],
  idCard: [
    {
      pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
      message: '请输入正确的身份证号',
      trigger: 'blur'
    }
  ]
}

// 批量导入
const importDialogVisible = ref(false)
const importing = ref(false)
const uploadRef = ref()
const uploadFile = ref(null)

// 详情
const detailDialogVisible = ref(false)
const currentTeacher = ref(null)

// 借阅历史
const showHistory = ref(false)
const currentReader = ref(null)
const showBorrowHistory = (row) => {
  currentReader.value = row
  showHistory.value = true
}

// 部门映射
const departmentMap = {
  chinese: '语文组',
  math: '数学组',
  english: '英语组',
  physics: '物理组',
  chemistry: '化学组',
  admin: '行政部门'
}

// 获取部门名称
const getDepartmentName = (department) => {
  return departmentMap[department] || department
}

// 加载教师列表
const loadTeacherList = async () => {
  loading.value = true
  try {
    const res = await getReaders({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      keyword: queryForm.keyword,
      readerType: 'teacher',
      status:
        queryForm.status === 'normal'
          ? 'active'
          : queryForm.status === 'frozen'
            ? 'suspended'
            : queryForm.status
    })

    if (res.code === 200 && res.data) {
      // 映射API字段到页面字段
      teacherList.value = res.data.records.map((reader) => ({
        id: reader.readerId,
        teacherNo: reader.teacherId || 'N/A',
        name: reader.realName,
        gender: reader.gender,
        department: reader.department || '',
        title: reader.title || '',
        phone: reader.phone,
        idCard: reader.idCard || '',
        email: reader.email || '',
        office: reader.address || '',
        cardNo: reader.cardNumber,
        borrowedCount: reader.currentBorrowCount,
        maxBorrow: reader.maxBorrowCount,
        status:
          reader.status === 'active'
            ? 'normal'
            : reader.status === 'suspended'
              ? 'frozen'
              : 'disabled',
        createdAt: reader.createdTime ? reader.createdTime.split('T')[0] : '',
        avatar: reader.avatar || ''
      }))

      pagination.total = res.data.total
    } else {
      ElMessage.error(res.message || '加载教师列表失败')
    }
  } catch (error) {
    console.error('加载教师列表失败:', error)
    ElMessage.error('加载教师列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadTeacherList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.department = ''
  queryForm.status = ''
  pagination.page = 1
  loadTeacherList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadTeacherList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadTeacherList()
}

// 选择
const handleSelectionChange = (selection) => {
  selectedTeachers.value = selection
}

const handleClearSelection = () => {
  selectedTeachers.value = []
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.keys(formData).forEach((key) => {
    formData[key] = key === 'gender' ? 'male' : ''
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 准备API数据
    const apiData = {
      realName: formData.name,
      phone: formData.phone,
      readerType: 'teacher',
      gender: formData.gender,
      idCard: formData.idCard,
      email: formData.email,
      address: formData.office,
      department: formData.department,
      title: formData.title,
      teacherId: formData.teacherNo
    }

    let res
    if (isEdit.value) {
      res = await updateReader(formData.id, apiData)
    } else {
      res = await createReader(apiData)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      loadTeacherList()
    } else {
      ElMessage.error(res.message || (isEdit.value ? '编辑失败' : '新增失败'))
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交表单失败:', error)
      ElMessage.error(isEdit.value ? '编辑失败' : '新增失败')
    }
  } finally {
    submitting.value = false
  }
}

// 查看详情
const handleView = (row) => {
  currentTeacher.value = row
  detailDialogVisible.value = true
}

// 冻结/解冻
const handleFreeze = (row) => {
  ElMessageBox.confirm(`确定要冻结教师 ${row.name} 的账户吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await updateReaderStatus(row.id, 'suspended')
        if (res.code === 200) {
          ElMessage.success('冻结成功')
          loadTeacherList()
        } else {
          ElMessage.error(res.message || '冻结失败')
        }
      } catch (error) {
        console.error('冻结失败:', error)
        ElMessage.error('冻结失败')
      }
    })
    .catch(() => {})
}

const handleUnfreeze = (row) => {
  ElMessageBox.confirm(`确定要解冻教师 ${row.name} 的账户吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await updateReaderStatus(row.id, 'active')
        if (res.code === 200) {
          ElMessage.success('解冻成功')
          loadTeacherList()
        } else {
          ElMessage.error(res.message || '解冻失败')
        }
      } catch (error) {
        console.error('解冻失败:', error)
        ElMessage.error('解冻失败')
      }
    })
    .catch(() => {})
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除教师 ${row.name} 吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteReader(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadTeacherList()
        } else {
          ElMessage.error(res.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败:', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedTeachers.value.length} 名教师吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const ids = selectedTeachers.value.map((t) => t.id)
        const res = await batchDeleteReaders(ids)
        if (res.code === 200) {
          ElMessage.success('批量删除成功')
          selectedTeachers.value = []
          loadTeacherList()
        } else {
          ElMessage.error(res.message || '批量删除失败')
        }
      } catch (error) {
        console.error('批量删除失败:', error)
        ElMessage.error('批量删除失败')
      }
    })
    .catch(() => {})
}

// Excel表头配置
const excelHeaders = [
  { label: '工号', key: 'teacherNo', required: true, width: 15 },
  { label: '姓名', key: 'name', required: true, width: 12 },
  { label: '性别', key: 'gender', required: true, width: 8 },
  { label: '部门', key: 'department', required: true, width: 15 },
  { label: '职称', key: 'title', required: true, width: 12 },
  { label: '联系电话', key: 'phone', required: true, width: 15 },
  { label: '电子邮箱', key: 'email', width: 25 },
  { label: '身份证号', key: 'idCard', width: 20 },
  { label: '办公地址', key: 'office', width: 30 }
]

// 导出数据
const handleExport = () => {
  try {
    if (teacherList.value.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }

    // 准备导出数据
    const exportData = teacherList.value.map((teacher) => ({
      teacherNo: teacher.teacherNo,
      name: teacher.name,
      gender: teacher.gender === 'male' ? '男' : '女',
      department: getDepartmentName(teacher.department),
      title: teacher.title,
      phone: teacher.phone,
      email: teacher.email,
      idCard: teacher.idCard,
      office: teacher.office
    }))

    exportExcel(exportData, excelHeaders, '教师读者列表')
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 批量导入
const handleImport = () => {
  importDialogVisible.value = true
  uploadFile.value = null
}

const handleDownloadTemplate = () => {
  try {
    // 示例数据
    const exampleData = [
      {
        teacherNo: 'T001',
        name: '李老师',
        gender: '男',
        department: '语文组',
        title: '副教授',
        phone: '13800138000',
        email: 'li@school.edu',
        idCard: '110101197001011234',
        office: '教学楼203室'
      }
    ]

    downloadTemplate(excelHeaders, '教师导入模板', exampleData)
    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    ElMessage.error('下载模板失败')
  }
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleConfirmImport = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  try {
    importing.value = true

    // 读取Excel文件
    const data = await readExcel(uploadFile.value, excelHeaders)

    if (data.length === 0) {
      ElMessage.warning('Excel文件中没有有效数据')
      importing.value = false
      return
    }

    if (data.length > 500) {
      ElMessage.warning('单次最多导入500条数据')
      importing.value = false
      return
    }

    // 验证数据
    const errors = []
    const validData = []

    for (let i = 0; i < data.length; i++) {
      const item = data[i]
      const rowNum = item._rowIndex

      // 验证必填字段
      if (!item.teacherNo) {
        errors.push(`第${rowNum}行: 工号不能为空`)
        continue
      }
      if (!item.name) {
        errors.push(`第${rowNum}行: 姓名不能为空`)
        continue
      }
      if (!item.phone) {
        errors.push(`第${rowNum}行: 联系电话不能为空`)
        continue
      }
      if (!item.department) {
        errors.push(`第${rowNum}行: 部门不能为空`)
        continue
      }

      // 验证手机号格式
      if (!/^1[3-9]\d{9}$/.test(item.phone)) {
        errors.push(`第${rowNum}行: 手机号格式不正确`)
        continue
      }

      // 转换性别
      let gender = 'male'
      if (item.gender === '女' || item.gender === 'female') {
        gender = 'female'
      }

      // 转换部门
      let departmentCode = item.department
      for (const [code, name] of Object.entries(departmentMap)) {
        if (name === item.department) {
          departmentCode = code
          break
        }
      }

      // 准备API数据
      validData.push({
        realName: item.name,
        phone: item.phone,
        readerType: 'teacher',
        gender: gender,
        idCard: item.idCard || '',
        email: item.email || '',
        address: item.office || '',
        department: departmentCode,
        title: item.title || '',
        teacherId: item.teacherNo
      })
    }

    // 如果有错误,显示前10条
    if (errors.length > 0) {
      const errorMsg = errors.slice(0, 10).join('\n')
      ElMessageBox.alert(
        `发现 ${errors.length} 个错误:\n${errorMsg}${errors.length > 10 ? '\n...' : ''}`,
        '数据验证失败',
        { type: 'error' }
      )
      importing.value = false
      return
    }

    // 批量创建
    let successCount = 0
    let failCount = 0
    const failedItems = []

    for (const item of validData) {
      try {
        const res = await createReader(item)
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
          failedItems.push({ name: item.realName, error: res.message || '创建失败' })
        }
      } catch (error) {
        failCount++
        failedItems.push({ name: item.realName, error: '网络错误' })
      }
    }

    // 显示导入结果
    const resultMsg = `导入完成!\n成功: ${successCount} 条\n失败: ${failCount} 条${
      failedItems.length > 0
        ? '\n\n失败详情:\n' +
          failedItems
            .slice(0, 5)
            .map((f) => `${f.name}: ${f.error}`)
            .join('\n')
        : ''
    }`

    ElMessageBox.alert(resultMsg, '导入结果', {
      type: successCount > 0 ? 'success' : 'warning'
    })

    importDialogVisible.value = false
    uploadFile.value = null
    loadTeacherList()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

// 初始化
loadTeacherList()
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

.text-primary {
  color: #1890ff;
}

.form-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

.import-actions {
  margin-bottom: 16px;
}

.upload-area {
  width: 100%;
}

.teacher-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: scale(1.1);
  }
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  color: #999;
  font-size: 20px;
  border-radius: 50%;
}

.teacher-detail {
  display: flex;
  gap: 24px;

  &-avatar {
    flex-shrink: 0;

    .detail-avatar-image {
      width: 150px;
      height: 150px;
      border-radius: 50%;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .detail-avatar-placeholder {
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
