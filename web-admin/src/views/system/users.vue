<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-header-title">用户管理</h1>
      <p class="page-header-description">管理系统用户账号和权限</p>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="lib-card mb-md">
      <div class="lib-card-body">
        <el-form :model="queryForm" :inline="true">
          <el-form-item>
            <el-input
              v-model="queryForm.keyword"
              placeholder="请输入用户名或姓名"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="角色">
            <el-select v-model="queryForm.role" placeholder="全部角色" clearable style="width: 150px">
              <el-option label="全部" value="" />
              <el-option label="超级管理员" value="admin" />
              <el-option label="管理员" value="manager" />
              <el-option label="图书管理员" value="librarian" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="正常" value="active" />
              <el-option label="停用" value="inactive" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="lib-card">
      <div class="lib-card-body">
        <el-table v-loading="loading" :data="userList" stripe>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="getRoleType(row.role)" size="small">{{ getRoleName(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="department" label="部门" width="150" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="email" label="邮箱" show-overflow-tooltip min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'active'" type="success" size="small">正常</el-tag>
              <el-tag v-else type="danger" size="small">停用</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-button type="warning" link :icon="Key" @click="handleResetPassword(row)">重置密码</el-button>
              <el-button
                v-if="row.status === 'active'"
                type="danger"
                link
                :icon="Close"
                @click="handleToggleStatus(row)"
              >
                停用
              </el-button>
              <el-button v-else type="success" link :icon="Check" @click="handleToggleStatus(row)">启用</el-button>
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

    <!-- 添加/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="userFormRef" :model="userForm" :rules="userFormRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>

        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="超级管理员" value="admin" />
            <el-option label="管理员" value="manager" />
            <el-option label="图书管理员" value="librarian" />
          </el-select>
        </el-form-item>

        <el-form-item label="部门" prop="department">
          <el-input v-model="userForm.department" placeholder="请输入部门" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 查询表单
const queryForm = reactive({
  keyword: '',
  role: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const userList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))

// 表单
const userFormRef = ref(null)
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: '',
  department: '',
  phone: '',
  email: '',
  status: 'active'
})

// 表单验证
const userFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

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

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取用户列表
    await new Promise((resolve) => setTimeout(resolve, 500))

    const mockData = []
    for (let i = 1; i <= pagination.pageSize; i++) {
      const id = (pagination.page - 1) * pagination.pageSize + i
      mockData.push({
        id,
        username: `user${id}`,
        realName: ['张三', '李四', '王五', '赵六', '刘老师'][i % 5],
        role: ['admin', 'manager', 'librarian'][i % 3],
        department: ['图书馆', '信息中心', '行政部'][i % 3],
        phone: `138${Math.random().toString().substr(2, 8)}`,
        email: `user${id}@library.com`,
        status: i % 10 === 0 ? 'inactive' : 'active',
        lastLoginTime: `2025-10-${(i % 28) + 1} ${(i % 12) + 8}:${(i % 60).toString().padStart(2, '0')}:00`
      })
    }

    userList.value = mockData
    pagination.total = 68
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadUserList()
}

// 重置
const handleReset = () => {
  queryForm.keyword = ''
  queryForm.role = ''
  queryForm.status = ''
  pagination.page = 1
  loadUserList()
}

// 分页
const handlePageChange = (page) => {
  pagination.page = page
  loadUserList()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadUserList()
}

// 新增用户
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(userForm, row)
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await userFormRef.value.validate()

    // TODO: 调用API保存用户
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
    dialogVisible.value = false
    loadUserList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  }
}

// 重置密码
const handleResetPassword = (row) => {
  ElMessageBox.confirm(`确定要重置用户"${row.realName}"的密码吗？密码将重置为：123456`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API重置密码
      await new Promise((resolve) => setTimeout(resolve, 500))
      ElMessage.success('密码重置成功')
    })
    .catch(() => {})
}

// 切换用户状态
const handleToggleStatus = (row) => {
  const action = row.status === 'active' ? '停用' : '启用'
  ElMessageBox.confirm(`确定要${action}用户"${row.realName}"吗？`, '提示', {
    type: 'warning'
  })
    .then(async () => {
      // TODO: 调用API切换状态
      await new Promise((resolve) => setTimeout(resolve, 500))
      ElMessage.success(`${action}成功`)
      loadUserList()
    })
    .catch(() => {})
}

// 重置表单
const resetForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.realName = ''
  userForm.role = ''
  userForm.department = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.status = 'active'
  userFormRef.value?.resetFields()
}

// 初始化
loadUserList()
</script>

<style lang="scss" scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
