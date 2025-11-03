<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">读者证办理</h1>
      <p class="page-header-description">办理新读者证、证件补办和注销</p>
    </div>

    <!-- 办理类型选择 -->
    <div class="card mb-md">
      <div class="card-content">
        <el-radio-group v-model="cardType" size="large">
          <el-radio-button value="new">
            <el-icon><Plus /></el-icon>
            新证办理
          </el-radio-button>
          <el-radio-button value="reissue">
            <el-icon><RefreshRight /></el-icon>
            证件补办
          </el-radio-button>
          <el-radio-button value="cancel">
            <el-icon><Close /></el-icon>
            证件注销
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 新证办理 -->
    <div v-if="cardType === 'new'" class="card">
      <div class="card-content">
        <el-form ref="cardFormRef" :model="cardForm" :rules="cardFormRules" label-width="120px">
          <el-form-item label="读者类型" prop="readerType">
            <el-radio-group v-model="cardForm.readerType">
              <el-radio value="student">学生</el-radio>
              <el-radio value="teacher">教师</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 学生信息 -->
          <template v-if="cardForm.readerType === 'student'">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="cardForm.studentNo" placeholder="请输入学号" style="width: 300px">
                <template #append>
                  <el-button :icon="Search" @click="handleSearchStudent">查询</el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="姓名" prop="name">
              <el-input v-model="cardForm.name" placeholder="请输入姓名" style="width: 300px" />
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="cardForm.gender">
                <el-radio value="male">男</el-radio>
                <el-radio value="female">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="年级" prop="grade">
                  <el-select v-model="cardForm.grade" placeholder="请选择年级">
                    <el-option label="高一" value="1" />
                    <el-option label="高二" value="2" />
                    <el-option label="高三" value="3" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="班级" prop="className">
                  <el-input v-model="cardForm.className" placeholder="如: 1班" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="cardForm.phone" placeholder="请输入联系电话" style="width: 300px" />
            </el-form-item>
          </template>

          <!-- 教师信息 -->
          <template v-if="cardForm.readerType === 'teacher'">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="cardForm.teacherNo" placeholder="请输入工号" style="width: 300px">
                <template #append>
                  <el-button :icon="Search" @click="handleSearchTeacher">查询</el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="姓名" prop="name">
              <el-input v-model="cardForm.name" placeholder="请输入姓名" style="width: 300px" />
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="cardForm.gender">
                <el-radio value="male">男</el-radio>
                <el-radio value="female">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="部门" prop="department">
                  <el-select v-model="cardForm.department" placeholder="请选择部门">
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
                  <el-select v-model="cardForm.title" placeholder="请选择职称">
                    <el-option label="助教" value="assistant" />
                    <el-option label="讲师" value="lecturer" />
                    <el-option label="副教授" value="associate_professor" />
                    <el-option label="教授" value="professor" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="cardForm.phone" placeholder="请输入联系电话" style="width: 300px" />
            </el-form-item>
          </template>

          <!-- 照片采集 -->
          <el-form-item label="证件照片" prop="photo">
            <AvatarUpload
              v-model="cardForm.photo"
              :size="160"
              shape="square"
              :enable-camera="true"
              :show-actions="true"
              :show-tips="true"
              placeholder-text="上传证件照"
              :tips="['建议尺寸 400x500 像素', '支持 JPG、PNG 格式', '文件大小不超过 2MB']"
            />
          </el-form-item>

          <!-- 读者证号 -->
          <el-form-item label="读者证号">
            <el-input v-model="cardForm.cardNo" placeholder="系统自动生成" disabled style="width: 300px">
              <template #prepend>
                <el-icon><Tickets /></el-icon>
              </template>
            </el-input>
            <span class="form-tip">系统将在办理成功后自动生成证号</span>
          </el-form-item>

          <!-- 有效期 -->
          <el-form-item label="有效期" prop="validDate">
            <el-date-picker
              v-model="cardForm.validDate"
              type="date"
              placeholder="选择有效期截止日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              style="width: 300px"
            />
            <span class="form-tip">默认为一年后</span>
          </el-form-item>

          <!-- 操作按钮 -->
          <el-form-item>
            <el-button type="primary" size="large" :icon="Check" :loading="submitting" @click="handleSubmit">
              立即办理
            </el-button>
            <el-button size="large" @click="handleReset">重置表单</el-button>
            <el-button size="large" :icon="Printer" @click="handlePreview">预览证件</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 证件补办 -->
    <div v-if="cardType === 'reissue'" class="card">
      <div class="card-content">
        <el-form :model="reissueForm" label-width="120px">
          <el-form-item label="原读者证号">
            <el-input
              v-model="reissueForm.oldCardNo"
              placeholder="请输入原读者证号"
              style="width: 300px"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearchCard">查询</el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-divider />

          <template v-if="reissueForm.readerInfo">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ reissueForm.readerInfo.name }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                {{ reissueForm.readerInfo.type === 'student' ? '学生' : '教师' }}
              </el-descriptions-item>
              <el-descriptions-item label="学号/工号">
                {{ reissueForm.readerInfo.no }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ reissueForm.readerInfo.phone }}
              </el-descriptions-item>
              <el-descriptions-item label="原证号">
                {{ reissueForm.oldCardNo }}
              </el-descriptions-item>
              <el-descriptions-item label="办证日期">
                {{ reissueForm.readerInfo.issueDate }}
              </el-descriptions-item>
            </el-descriptions>

            <el-form-item label="补办原因" style="margin-top: 24px">
              <el-input
                v-model="reissueForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入补办原因（如：遗失、损坏等）"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :icon="Check" @click="handleReissue">
                确认补办
              </el-button>
              <el-button size="large" @click="reissueForm.readerInfo = null">取消</el-button>
            </el-form-item>
          </template>
        </el-form>
      </div>
    </div>

    <!-- 证件注销 -->
    <div v-if="cardType === 'cancel'" class="card">
      <div class="card-content">
        <el-form :model="cancelForm" label-width="120px">
          <el-form-item label="读者证号">
            <el-input
              v-model="cancelForm.cardNo"
              placeholder="请输入读者证号"
              style="width: 300px"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearchCardForCancel">查询</el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-divider />

          <template v-if="cancelForm.readerInfo">
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              title="注意：证件注销后无法恢复"
              style="margin-bottom: 24px"
            />

            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ cancelForm.readerInfo.name }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                {{ cancelForm.readerInfo.type === 'student' ? '学生' : '教师' }}
              </el-descriptions-item>
              <el-descriptions-item label="读者证号">
                {{ cancelForm.cardNo }}
              </el-descriptions-item>
              <el-descriptions-item label="借阅情况">
                {{ cancelForm.readerInfo.borrowedCount }} / {{ cancelForm.readerInfo.maxBorrow }}
              </el-descriptions-item>
              <el-descriptions-item label="欠款金额" :span="2">
                <span :class="cancelForm.readerInfo.debt > 0 ? 'text-danger' : ''">
                  ¥{{ cancelForm.readerInfo.debt.toFixed(2) }}
                </span>
              </el-descriptions-item>
            </el-descriptions>

            <el-alert
              v-if="cancelForm.readerInfo.borrowedCount > 0"
              type="error"
              :closable="false"
              show-icon
              title="该读者还有未归还图书，无法注销证件"
              style="margin-top: 16px"
            />

            <el-alert
              v-if="cancelForm.readerInfo.debt > 0"
              type="warning"
              :closable="false"
              show-icon
              title="该读者还有未缴清的欠款，请先处理欠款"
              style="margin-top: 16px"
            />

            <el-form-item label="注销原因" style="margin-top: 24px">
              <el-input
                v-model="cancelForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入注销原因（如：毕业离校、工作调动等）"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="danger"
                size="large"
                :icon="Close"
                :disabled="cancelForm.readerInfo.borrowedCount > 0 || cancelForm.readerInfo.debt > 0"
                @click="handleCancel"
              >
                确认注销
              </el-button>
              <el-button size="large" @click="cancelForm.readerInfo = null">取消</el-button>
            </el-form-item>
          </template>
        </el-form>
      </div>
    </div>

    <!-- 证件预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="读者证预览" width="500px">
      <div class="card-preview">
        <div class="card-preview-header">
          <h3>国创睿峰智能图书馆</h3>
          <p>读者证</p>
        </div>
        <div class="card-preview-body">
          <div class="card-preview-photo">
            <img v-if="cardForm.photo" :src="cardForm.photo" alt="Photo" />
            <div v-else class="card-preview-photo-placeholder">照片</div>
          </div>
          <div class="card-preview-info">
            <div class="info-item">
              <span class="label">姓名：</span>
              <span class="value">{{ cardForm.name || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">类型：</span>
              <span class="value">{{ cardForm.readerType === 'student' ? '学生' : '教师' }}</span>
            </div>
            <div class="info-item">
              <span class="label">证号：</span>
              <span class="value">{{ cardForm.cardNo || '自动生成' }}</span>
            </div>
            <div class="info-item">
              <span class="label">有效期：</span>
              <span class="value">{{ cardForm.validDate || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="card-preview-footer">
          <el-icon><Barcode /></el-icon>
          <span>{{ cardForm.cardNo || '* * * * * * * * *' }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Printer">打印证件</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import AvatarUpload from '@/components/AvatarUpload.vue'

// 办理类型
const cardType = ref('new')

// 表单引用
const cardFormRef = ref()

// 新证办理表单
const cardForm = reactive({
  readerType: 'student',
  studentNo: '',
  teacherNo: '',
  name: '',
  gender: 'male',
  grade: '',
  className: '',
  department: '',
  title: '',
  phone: '',
  photo: '',
  cardNo: '',
  validDate: dayjs().add(1, 'year').format('YYYY-MM-DD')
})

// 表单验证规则
const cardFormRules = {
  readerType: [{ required: true, message: '请选择读者类型', trigger: 'change' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  validDate: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

// 证件补办表单
const reissueForm = reactive({
  oldCardNo: '',
  readerInfo: null,
  reason: ''
})

// 证件注销表单
const cancelForm = reactive({
  cardNo: '',
  readerInfo: null,
  reason: ''
})

// 提交状态
const submitting = ref(false)

// 预览对话框
const previewDialogVisible = ref(false)

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

// 查询学生信息
const handleSearchStudent = async () => {
  if (!cardForm.studentNo) {
    ElMessage.warning('请输入学号')
    return
  }

  try {
    // TODO: 调用API查询学生信息
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock数据
    cardForm.name = '张三'
    cardForm.gender = 'male'
    cardForm.grade = '1'
    cardForm.className = '1班'
    cardForm.phone = '13800138000'

    ElMessage.success('学生信息查询成功')
  } catch (error) {
    ElMessage.error('查询失败，请检查学号是否正确')
  }
}

// 查询教师信息
const handleSearchTeacher = async () => {
  if (!cardForm.teacherNo) {
    ElMessage.warning('请输入工号')
    return
  }

  try {
    // TODO: 调用API查询教师信息
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock数据
    cardForm.name = '李老师'
    cardForm.gender = 'female'
    cardForm.department = 'chinese'
    cardForm.title = 'lecturer'
    cardForm.phone = '13900139000'

    ElMessage.success('教师信息查询成功')
  } catch (error) {
    ElMessage.error('查询失败，请检查工号是否正确')
  }
}

// 提交办理
const handleSubmit = async () => {
  try {
    await cardFormRef.value.validate()

    submitting.value = true

    // TODO: 调用API办理读者证
    await new Promise((resolve) => setTimeout(resolve, 1000))

    // 生成读者证号（Mock）
    const prefix = cardForm.readerType === 'student' ? 'S' : 'T'
    cardForm.cardNo = `${prefix}2025${Math.random().toString().substr(2, 6)}`

    ElMessage.success(`读者证办理成功！证号：${cardForm.cardNo}`)

    // 自动打开预览
    setTimeout(() => {
      previewDialogVisible.value = true
    }, 500)
  } catch (error) {
    if (error !== false) {
      ElMessage.error('办理失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  cardFormRef.value?.resetFields()
  cardForm.photo = ''
  cardForm.cardNo = ''
  cardForm.validDate = dayjs().add(1, 'year').format('YYYY-MM-DD')
}

// 预览证件
const handlePreview = () => {
  if (!cardForm.name) {
    ElMessage.warning('请先填写读者信息')
    return
  }
  previewDialogVisible.value = true
}

// 查询读者证（补办）
const handleSearchCard = async () => {
  if (!reissueForm.oldCardNo) {
    ElMessage.warning('请输入读者证号')
    return
  }

  try {
    // TODO: 调用API查询读者证信息
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock数据
    reissueForm.readerInfo = {
      name: '张三',
      type: 'student',
      no: 'S20250001',
      phone: '13800138000',
      issueDate: '2025-09-01'
    }

    ElMessage.success('读者信息查询成功')
  } catch (error) {
    ElMessage.error('查询失败，请检查证号是否正确')
  }
}

// 确认补办
const handleReissue = () => {
  if (!reissueForm.reason) {
    ElMessage.warning('请输入补办原因')
    return
  }

  ElMessageBox.confirm('确定要补办该读者证吗？补办后原证号将失效。', '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API补办读者证
      await new Promise((resolve) => setTimeout(resolve, 500))

      ElMessage.success('补办成功，新证号已生成')
      reissueForm.readerInfo = null
      reissueForm.oldCardNo = ''
      reissueForm.reason = ''
    })
    .catch(() => {})
}

// 查询读者证（注销）
const handleSearchCardForCancel = async () => {
  if (!cancelForm.cardNo) {
    ElMessage.warning('请输入读者证号')
    return
  }

  try {
    // TODO: 调用API查询读者证信息
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock数据
    cancelForm.readerInfo = {
      name: '王五',
      type: 'student',
      borrowedCount: 0,
      maxBorrow: 5,
      debt: 0
    }

    ElMessage.success('读者信息查询成功')
  } catch (error) {
    ElMessage.error('查询失败，请检查证号是否正确')
  }
}

// 确认注销
const handleCancel = () => {
  if (!cancelForm.reason) {
    ElMessage.warning('请输入注销原因')
    return
  }

  ElMessageBox.confirm('确定要注销该读者证吗？此操作不可恢复！', '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API注销读者证
      await new Promise((resolve) => setTimeout(resolve, 500))

      ElMessage.success('证件注销成功')
      cancelForm.readerInfo = null
      cancelForm.cardNo = ''
      cancelForm.reason = ''
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.text-danger {
  color: #f5222d;
  font-weight: 600;
}

.card-preview {
  width: 400px;
  margin: 0 auto;
  border: 2px solid #1890ff;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  &-header {
    text-align: center;
    padding: 24px 16px 16px;

    h3 {
      font-size: 20px;
      margin: 0 0 8px;
    }

    p {
      font-size: 14px;
      margin: 0;
      opacity: 0.9;
    }
  }

  &-body {
    display: flex;
    padding: 16px;
    background: rgba(255, 255, 255, 0.95);
    color: #333;
  }

  &-photo {
    width: 100px;
    height: 130px;
    margin-right: 16px;
    border: 2px solid #ddd;
    border-radius: 4px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      background: #f0f0f0;
      color: #999;
      font-size: 14px;
    }
  }

  &-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-around;

    .info-item {
      font-size: 14px;

      .label {
        color: #666;
        margin-right: 8px;
      }

      .value {
        color: #333;
        font-weight: 500;
      }
    }
  }

  &-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    background: rgba(255, 255, 255, 0.2);
    font-family: monospace;
    font-size: 16px;
    letter-spacing: 2px;
  }
}

:deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
