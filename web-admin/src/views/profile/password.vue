<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">修改密码</h1>
      <p class="page-header-description">定期修改密码以保证账户安全</p>
    </div>

    <div class="card">
      <div class="card-content">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="密码安全建议"
          style="margin-bottom: 24px"
        >
          <template #default>
            <div>1. 密码长度至少6位</div>
            <div>2. 建议包含字母、数字和特殊字符</div>
            <div>3. 定期更换密码以保证账户安全</div>
          </template>
        </el-alert>

        <el-form
          ref="formRef"
          :model="passwordForm"
          :rules="formRules"
          label-width="120px"
          style="max-width: 600px"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入当前密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              确认修改
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// 表单引用
const formRef = ref()

// 表单数据
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const formRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 提交状态
const submitting = ref(false)

// 提交保存
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    // TODO: 调用API修改密码
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('密码修改成功，请重新登录')

    // 清空表单
    handleReset()

    // TODO: 退出登录并跳转到登录页
    setTimeout(() => {
      // router.push('/login')
    }, 1500)
  } catch (error) {
    if (error !== false) {
      ElMessage.error('密码修改失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  formRef.value?.resetFields()
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style lang="scss" scoped>
// 页面特定样式
</style>
