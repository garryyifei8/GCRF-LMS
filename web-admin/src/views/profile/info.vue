<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">个人信息</h1>
      <p class="page-header-description">查看和编辑个人资料</p>
    </div>

    <div class="card">
      <div class="card-content">
        <el-form ref="formRef" :model="userInfo" :rules="formRules" label-width="120px">
          <el-form-item label="用户名">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>

          <el-form-item label="姓名" prop="realName">
            <el-input v-model="userInfo.realName" placeholder="请输入姓名" style="width: 400px" />
          </el-form-item>

          <el-form-item label="角色">
            <el-tag :type="getRoleType(userInfo.role)" size="large">
              {{ getRoleName(userInfo.role) }}
            </el-tag>
          </el-form-item>

          <el-form-item label="部门">
            <el-input v-model="userInfo.department" placeholder="请输入部门" style="width: 400px" />
          </el-form-item>

          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="userInfo.phone" placeholder="请输入联系电话" style="width: 400px" />
          </el-form-item>

          <el-form-item label="电子邮箱" prop="email">
            <el-input v-model="userInfo.email" placeholder="请输入电子邮箱" style="width: 400px" />
          </el-form-item>

          <el-form-item label="最后登录时间">
            <el-input :value="userInfo.lastLoginTime" disabled style="width: 400px" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
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

// 用户信息
const userInfo = reactive({
  username: 'admin',
  realName: '管理员',
  role: 'admin',
  department: '图书馆',
  phone: '13800138000',
  email: 'admin@library.com',
  lastLoginTime: '2025-10-10 18:30:00'
})

// 备份初始数据
const initialData = { ...userInfo }

// 表单验证规则
const formRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 提交状态
const submitting = ref(false)

// 角色映射
const roleMap = {
  admin: '超级管理员',
  manager: '管理员',
  librarian: '图书管理员'
}

// 获取角色名称
const getRoleName = (role) => roleMap[role] || role

// 获取角色标签类型
const getRoleType = (role) => {
  const typeMap = { admin: 'danger', manager: 'warning', librarian: '' }
  return typeMap[role] || ''
}

// 提交保存
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    // TODO: 调用API保存用户信息
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('个人信息保存成功')

    // 更新初始数据
    Object.assign(initialData, userInfo)
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  Object.assign(userInfo, initialData)
  formRef.value?.clearValidate()
}
</script>

<style lang="scss" scoped>
// 页面特定样式
</style>
