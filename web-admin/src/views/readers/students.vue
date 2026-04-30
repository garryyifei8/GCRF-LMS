<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">学生读者管理</h1>
      <p class="page-header-description">管理学生读者信息和读者证</p>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="lib-card mb-md">
      <div class="lib-card-body">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="请输入姓名、学号或读者证号"
              clearable
              style="width: 280px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="年级">
            <el-select
              v-model="queryForm.grade"
              placeholder="全部年级"
              clearable
              style="width: 120px"
            >
              <el-option label="全部" value="" />
              <el-option label="高一" value="1" />
              <el-option label="高二" value="2" />
              <el-option label="高三" value="3" />
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
          </el-form-item>

          <el-form-item style="float: right">
            <el-button-group>
              <el-button
                :type="viewType === 'list' ? 'primary' : 'default'"
                @click="viewType = 'list'"
              >
                <el-icon><List /></el-icon>
                列表
              </el-button>
              <el-button
                :type="viewType === 'card' ? 'primary' : 'default'"
                @click="viewType = 'card'"
              >
                <el-icon><Grid /></el-icon>
                卡片
              </el-button>
            </el-button-group>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="lib-card mb-md">
      <div class="lib-card-body">
        <el-button type="success" :icon="Plus" @click="handleAdd">新增学生</el-button>
        <el-button type="danger" :icon="Delete" @click="showBatchCancel = true"
          >按年级批量注销</el-button
        >
        <el-button type="warning" :icon="Upload" @click="handleImport">批量导入</el-button>
        <el-button :icon="Download" @click="handleExport">导出数据</el-button>
      </div>
    </div>

    <!-- 数据表格/卡片视图 -->
    <div v-if="viewType === 'list'" class="lib-card">
      <div class="lib-card-body">
        <!-- 批量操作 -->
        <div v-if="selectedStudents.length > 0" class="batch-actions">
          <span class="batch-info">已选择 {{ selectedStudents.length }} 项</span>
          <el-button type="danger" size="small" :icon="Delete" @click="handleBatchDelete"
            >批量删除</el-button
          >
          <el-button size="small" @click="handleClearSelection">取消选择</el-button>
        </div>

        <!-- 表格 -->
        <el-table
          v-loading="loading"
          :data="studentList"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-image
                :src="row.avatar || defaultAvatar"
                :preview-src-list="[row.avatar || defaultAvatar]"
                class="student-avatar"
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
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="gender" label="性别" width="60">
            <template #default="{ row }">
              {{ row.gender === 'male' ? '男' : '女' }}
            </template>
          </el-table-column>
          <el-table-column label="年级班级" width="120">
            <template #default="{ row }"> {{ row.gradeName }}({{ row.className }}) </template>
          </el-table-column>
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
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-link type="primary" :underline="false" @click="handleView(row)">查看</el-link>
                <el-link type="primary" :underline="false" @click="handleEdit(row)">编辑</el-link>
                <el-link type="primary" :underline="false" @click="showBorrowHistory(row)"
                  >借阅历史</el-link
                >
                <el-link
                  v-if="row.status !== 'frozen'"
                  type="warning"
                  :underline="false"
                  @click="handleFreeze(row)"
                >
                  冻结
                </el-link>
                <el-link v-else type="success" :underline="false" @click="handleUnfreeze(row)"
                  >解冻</el-link
                >
                <el-link type="danger" :underline="false" @click="handleDelete(row)">删除</el-link>
              </div>
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

    <!-- 卡片视图 -->
    <div v-if="viewType === 'card'" class="card-view">
      <div class="card-grid">
        <div v-for="student in studentList" :key="student.id" class="student-card">
          <div class="card-avatar">
            <el-image :src="student.avatar || defaultAvatar" fit="cover" class="avatar-image">
              <template #error>
                <div class="avatar-placeholder">
                  <el-icon><User /></el-icon>
                </div>
              </template>
            </el-image>
            <el-tag
              v-if="student.status === 'normal'"
              type="success"
              size="small"
              class="status-tag"
            >
              正常
            </el-tag>
            <el-tag
              v-else-if="student.status === 'frozen'"
              type="danger"
              size="small"
              class="status-tag"
            >
              冻结
            </el-tag>
            <el-tag v-else type="info" size="small" class="status-tag"> 注销 </el-tag>
          </div>
          <div class="card-body">
            <h3>{{ student.name }}</h3>
            <p class="student-no">{{ student.studentNo }}</p>
            <div class="card-info">
              <div class="info-item">
                <span class="label">班级：</span>
                <span class="value">{{ student.gradeName }}({{ student.className }})</span>
              </div>
              <div class="info-item">
                <span class="label">电话：</span>
                <span class="value">{{ student.phone }}</span>
              </div>
              <div class="info-item">
                <span class="label">借阅：</span>
                <span class="value" :class="student.borrowedCount > 0 ? 'text-primary' : ''">
                  {{ student.borrowedCount }} / {{ student.maxBorrow }}
                </span>
              </div>
            </div>
          </div>
          <div class="card-footer">
            <el-link type="primary" :underline="false" @click="handleView(student)">查看</el-link>
            <el-link type="primary" :underline="false" @click="handleEdit(student)">编辑</el-link>
            <el-link type="danger" :underline="false" @click="handleDelete(student)">删除</el-link>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[12, 24, 48, 96]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
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
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="formData.studentNo" placeholder="请输入学号" />
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
            <el-form-item label="年级" prop="grade">
              <el-select v-model="formData.grade" placeholder="请选择年级" style="width: 100%">
                <el-option label="高一" value="1" />
                <el-option label="高二" value="2" />
                <el-option label="高三" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="formData.className" placeholder="如: 1班" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="formData.idCard" placeholder="请输入身份证号" />
        </el-form-item>

        <el-form-item label="家庭住址">
          <el-input
            v-model="formData.address"
            type="textarea"
            :rows="2"
            placeholder="请输入家庭住址"
          />
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
    <el-dialog v-model="importDialogVisible" title="批量导入学生" width="600px">
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

    <!-- 按年级批量注销对话框 -->
    <BatchCancelDialog v-model="showBatchCancel" @success="loadStudentList" />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="学生详情" width="900px">
      <div v-if="currentStudent" class="student-detail">
        <div class="student-detail-avatar">
          <el-image
            :src="currentStudent.avatar || defaultAvatar"
            :preview-src-list="[currentStudent.avatar || defaultAvatar]"
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

        <div class="student-detail-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="学号">{{ currentStudent.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ currentStudent.name }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ currentStudent.gender === 'male' ? '男' : '女' }}
            </el-descriptions-item>
            <el-descriptions-item label="年级班级">
              {{ currentStudent.gradeName }}({{ currentStudent.className }})
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentStudent.phone }}</el-descriptions-item>
            <el-descriptions-item label="读者证号">{{
              currentStudent.cardNo
            }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{
              currentStudent.idCard
            }}</el-descriptions-item>
            <el-descriptions-item label="借阅情况">
              {{ currentStudent.borrowedCount }} / {{ currentStudent.maxBorrow }}
            </el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <el-tag v-if="currentStudent.status === 'normal'" type="success">正常</el-tag>
              <el-tag v-else-if="currentStudent.status === 'frozen'" type="danger">冻结</el-tag>
              <el-tag v-else type="info">注销</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{
              currentStudent.createdAt
            }}</el-descriptions-item>
            <el-descriptions-item label="家庭住址" :span="2">
              {{ currentStudent.address || '暂无' }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- AI行为分析 -->
          <div class="ai-analysis-section">
            <div class="section-header">
              <h4>AI 行为分析</h4>
              <el-tag type="success" size="small">智能洞察</el-tag>
            </div>

            <el-row :gutter="16" class="mb-md">
              <el-col :span="8">
                <div class="analysis-card">
                  <div class="analysis-label">阅读活跃度</div>
                  <el-progress
                    type="circle"
                    :percentage="readerBehavior.activityScore"
                    :color="getActivityColor(readerBehavior.activityScore)"
                  >
                    <template #default="{ percentage }">
                      <span class="percentage-value">{{ percentage }}</span>
                      <span class="percentage-label">分</span>
                    </template>
                  </el-progress>
                  <div class="analysis-desc">
                    {{ getActivityLevel(readerBehavior.activityScore) }}
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="analysis-card">
                  <div class="analysis-label">阅读偏好匹配</div>
                  <el-progress
                    type="circle"
                    :percentage="readerBehavior.preferenceMatch"
                    color="#52c41a"
                  >
                    <template #default="{ percentage }">
                      <span class="percentage-value">{{ percentage }}</span>
                      <span class="percentage-label">%</span>
                    </template>
                  </el-progress>
                  <div class="analysis-desc">与同龄人相似度</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="analysis-card">
                  <div class="analysis-label">成长潜力</div>
                  <el-progress
                    type="circle"
                    :percentage="readerBehavior.growthPotential"
                    color="#fa8c16"
                  >
                    <template #default="{ percentage }">
                      <span class="percentage-value">{{ percentage }}</span>
                      <span class="percentage-label">%</span>
                    </template>
                  </el-progress>
                  <div class="analysis-desc">预测未来借阅趋势</div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="16" class="mb-md">
              <el-col :span="12">
                <div class="info-card">
                  <div class="info-header">
                    <el-icon><Collection /></el-icon>
                    <span>阅读偏好</span>
                  </div>
                  <div class="tag-list">
                    <el-tag
                      v-for="tag in readerBehavior.preferences"
                      :key="tag"
                      class="mr-sm mb-sm"
                      type="primary"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="info-card">
                  <div class="info-header">
                    <el-icon><TrendCharts /></el-icon>
                    <span>借阅趋势</span>
                  </div>
                  <div class="trend-info">
                    <div class="trend-item">
                      <span class="trend-label">月均借阅:</span>
                      <span class="trend-value">{{ readerBehavior.avgMonthlyBorrow }} 本</span>
                    </div>
                    <div class="trend-item">
                      <span class="trend-label">连续借阅:</span>
                      <span class="trend-value">{{ readerBehavior.consecutiveDays }} 天</span>
                    </div>
                    <div class="trend-item">
                      <span class="trend-label">最爱类别:</span>
                      <span class="trend-value">{{ readerBehavior.favoriteCategory }}</span>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>

            <div class="info-card">
              <div class="info-header">
                <el-icon><ChatDotRound /></el-icon>
                <span>AI 建议</span>
              </div>
              <ul class="suggestion-list">
                <li v-for="(suggestion, index) in readerBehavior.suggestions" :key="index">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Upload,
  Download,
  View,
  Edit,
  Delete,
  Lock,
  Unlock,
  User,
  List,
  Grid
} from '@element-plus/icons-vue'
import AvatarUpload from '@/components/AvatarUpload.vue'
import BorrowHistoryDialog from '@/components/readers/BorrowHistoryDialog.vue'
import BatchCancelDialog from '@/components/readers/BatchCancelDialog.vue'
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

// 视图类型
const viewType = ref('list')

// 查询表单
const queryForm = reactive({
  keyword: '',
  grade: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const studentList = ref([])
const selectedStudents = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑学生' : '新增学生'))
const submitting = ref(false)

// 表单
const formRef = ref()
const formData = reactive({
  studentNo: '',
  name: '',
  gender: 'male',
  grade: '',
  className: '',
  phone: '',
  idCard: '',
  address: '',
  cardNo: '',
  avatar: ''
})

// 默认头像
const defaultAvatar =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0iIzY2N2VlYSIvPjxwYXRoIGQ9Ik01MCwyMGMxMSwwLDIwLDksMjAsMjBzLTksMjAtMjAsMjAtMjAtOS0yMC0yMFMzOSwyMCw1MCwyMHpNMjAsODBjMC0xNiwxMy41LTMwLDMwLTMwczMwLDE0LDMwLDMwIiBmaWxsPSIjZmZmIi8+PC9zdmc+'

const formRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
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
const currentStudent = ref(null)

// 借阅历史
const showHistory = ref(false)
const currentReader = ref(null)
const showBorrowHistory = (row) => {
  currentReader.value = row
  showHistory.value = true
}

// 按年级批量注销
const showBatchCancel = ref(false)

// AI读者行为分析数据
const readerBehavior = reactive({
  activityScore: 85,
  preferenceMatch: 78,
  growthPotential: 92,
  preferences: ['计算机', '人工智能', '编程', '算法'],
  avgMonthlyBorrow: 4.5,
  consecutiveDays: 45,
  favoriteCategory: '计算机科学',
  suggestions: [
    '该读者阅读活跃度高,建议推荐更多技术类图书',
    '偏好计算机和人工智能类图书,可推荐《深度学习》《算法导论》等',
    '保持良好的阅读习惯,建议设置月度阅读目标激励'
  ]
})

// 年级映射
const gradeMap = {
  1: '高一',
  2: '高二',
  3: '高三'
}

// 加载学生列表
const loadStudentList = async () => {
  loading.value = true
  try {
    const res = await getReaders({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      keyword: queryForm.keyword,
      readerType: 'student',
      status:
        queryForm.status === 'normal'
          ? 'active'
          : queryForm.status === 'frozen'
            ? 'suspended'
            : queryForm.status
    })

    if (res.code === 200 && res.data) {
      // 映射API字段到页面字段
      studentList.value = res.data.records.map((reader) => ({
        id: reader.readerId,
        studentNo: reader.studentId || 'N/A',
        name: reader.realName,
        gender: reader.gender,
        grade: reader.grade || '',
        gradeName: reader.grade || '',
        className: reader.department || '',
        phone: reader.phone,
        idCard: reader.idCard || '',
        cardNo: reader.cardNumber,
        borrowedCount: reader.currentBorrowCount,
        maxBorrow: reader.maxBorrowCount,
        status:
          reader.status === 'active'
            ? 'normal'
            : reader.status === 'suspended'
              ? 'frozen'
              : 'disabled',
        address: reader.address || '',
        createdAt: reader.createdTime ? reader.createdTime.split('T')[0] : '',
        avatar: reader.avatar || ''
      }))

      pagination.total = res.data.total
    } else {
      ElMessage.error(res.message || '加载学生列表失败')
    }
  } catch (error) {
    console.error('加载学生列表失败:', error)
    ElMessage.error('加载学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadStudentList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.grade = ''
  queryForm.status = ''
  pagination.page = 1
  loadStudentList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadStudentList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadStudentList()
}

// 选择
const handleSelectionChange = (selection) => {
  selectedStudents.value = selection
}

const handleClearSelection = () => {
  selectedStudents.value = []
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
      readerType: 'student',
      gender: formData.gender,
      idCard: formData.idCard,
      address: formData.address,
      department: formData.grade ? `${gradeMap[formData.grade]}${formData.className}` : '',
      grade: formData.grade ? `${gradeMap[formData.grade]}${formData.className}` : '',
      studentId: formData.studentNo
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
      loadStudentList()
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
  currentStudent.value = row
  detailDialogVisible.value = true

  // Mock AI行为分析数据 (实际应该从API获取)
  readerBehavior.activityScore = Math.floor(Math.random() * 30) + 70
  readerBehavior.preferenceMatch = Math.floor(Math.random() * 30) + 60
  readerBehavior.growthPotential = Math.floor(Math.random() * 30) + 70
}

// 活跃度颜色
const getActivityColor = (score) => {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#fa8c16'
  return '#f5222d'
}

// 活跃度等级
const getActivityLevel = (score) => {
  if (score >= 80) return '非常活跃'
  if (score >= 60) return '较为活跃'
  return '活跃度较低'
}

// 冻结/解冻
const handleFreeze = (row) => {
  ElMessageBox.confirm(`确定要冻结学生 ${row.name} 的账户吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await updateReaderStatus(row.id, 'suspended')
        if (res.code === 200) {
          ElMessage.success('冻结成功')
          loadStudentList()
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
  ElMessageBox.confirm(`确定要解冻学生 ${row.name} 的账户吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await updateReaderStatus(row.id, 'active')
        if (res.code === 200) {
          ElMessage.success('解冻成功')
          loadStudentList()
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
  ElMessageBox.confirm(`确定要删除学生 ${row.name} 吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const res = await deleteReader(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          loadStudentList()
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
  ElMessageBox.confirm(`确定要删除选中的 ${selectedStudents.value.length} 名学生吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      try {
        const ids = selectedStudents.value.map((s) => s.id)
        const res = await batchDeleteReaders(ids)
        if (res.code === 200) {
          ElMessage.success('批量删除成功')
          selectedStudents.value = []
          loadStudentList()
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

// 批量导入
const handleImport = () => {
  importDialogVisible.value = true
  uploadFile.value = null
}

// Excel表头配置
const excelHeaders = [
  { label: '学号', key: 'studentNo', required: true, width: 15 },
  { label: '姓名', key: 'name', required: true, width: 12 },
  { label: '性别', key: 'gender', required: true, width: 8 },
  { label: '年级', key: 'gradeName', required: true, width: 12 },
  { label: '班级', key: 'className', required: true, width: 12 },
  { label: '联系电话', key: 'phone', required: true, width: 15 },
  { label: '身份证号', key: 'idCard', width: 20 },
  { label: '家庭住址', key: 'address', width: 30 }
]

// 导出数据
const handleExport = () => {
  try {
    if (studentList.value.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }

    // 准备导出数据
    const exportData = studentList.value.map((student) => ({
      studentNo: student.studentNo,
      name: student.name,
      gender: student.gender === 'male' ? '男' : '女',
      gradeName: student.gradeName,
      className: student.className,
      phone: student.phone,
      idCard: student.idCard,
      address: student.address
    }))

    exportExcel(exportData, excelHeaders, '学生读者列表')
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleDownloadTemplate = () => {
  try {
    // 示例数据
    const exampleData = [
      {
        studentNo: '2024001',
        name: '张三',
        gender: '男',
        gradeName: '高一',
        className: '1班',
        phone: '13800138000',
        idCard: '110101200001011234',
        address: '北京市朝阳区'
      }
    ]

    downloadTemplate(excelHeaders, '学生导入模板', exampleData)
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
      if (!item.studentNo) {
        errors.push(`第${rowNum}行: 学号不能为空`)
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

      // 准备API数据
      validData.push({
        realName: item.name,
        phone: item.phone,
        readerType: 'student',
        gender: gender,
        idCard: item.idCard || '',
        address: item.address || '',
        department: `${item.gradeName || ''}${item.className || ''}`,
        grade: `${item.gradeName || ''}${item.className || ''}`,
        studentId: item.studentNo
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
    loadStudentList()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

// 初始化
loadStudentList()
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
  color: #667eea;
}

// 表格操作链接样式
.table-actions {
  display: flex;
  gap: 12px;
  align-items: center;

  .el-link {
    padding: 2px 6px;
    border-radius: 4px;
    transition: all 0.2s;
    font-size: 13px;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
    }
  }
}

// 卡片视图样式
.card-view {
  .card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .student-card {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
    }

    .card-avatar {
      position: relative;
      padding: 20px;
      background: linear-gradient(
        135deg,
        rgba(102, 126, 234, 0.1) 0%,
        rgba(118, 75, 162, 0.1) 100%
      );
      text-align: center;

      .avatar-image {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        border: 3px solid white;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      .avatar-placeholder {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: #f5f5f5;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #999;
        font-size: 32px;
      }

      .status-tag {
        position: absolute;
        top: 20px;
        right: 20px;
      }
    }

    .card-body {
      padding: 20px;

      h3 {
        margin: 0 0 4px;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }

      .student-no {
        color: #909399;
        font-size: 14px;
        margin: 0 0 16px;
      }

      .card-info {
        .info-item {
          display: flex;
          margin-bottom: 8px;
          font-size: 14px;

          .label {
            color: #909399;
            margin-right: 8px;
            min-width: 48px;
          }

          .value {
            color: #606266;
            flex: 1;
          }
        }
      }
    }

    .card-footer {
      padding: 12px 20px;
      border-top: 1px solid #f0f0f0;
      display: flex;
      justify-content: space-around;

      .el-link {
        padding: 4px 12px;
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
          background: rgba(0, 0, 0, 0.04);
        }
      }
    }
  }
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

.student-avatar {
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

.student-detail {
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

.ai-analysis-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
    }
  }

  .analysis-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    background: #fafafa;
    border-radius: 8px;
    text-align: center;

    .analysis-label {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.65);
      margin-bottom: 16px;
      font-weight: 500;
    }

    .percentage-value {
      font-size: 24px;
      font-weight: 600;
    }

    .percentage-label {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.45);
      margin-left: 2px;
    }

    .analysis-desc {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.45);
      margin-top: 12px;
    }
  }

  .info-card {
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;

    .info-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
      margin-bottom: 12px;
    }

    .tag-list {
      display: flex;
      flex-wrap: wrap;
    }

    .trend-info {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .trend-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .trend-label {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.65);
      }

      .trend-value {
        font-size: 14px;
        font-weight: 600;
        color: #1890ff;
      }
    }

    .suggestion-list {
      margin: 0;
      padding-left: 20px;
      list-style: disc;

      li {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.65);
        line-height: 1.8;
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }
}
</style>
